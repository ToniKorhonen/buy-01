import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../services/product.service';
import { MediaService } from '../../services/media.service';
import { ProductResponse } from '../../models/product.model';
import { forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

// Extended product interface for display
interface ProductWithMedia extends ProductResponse {
  imageUrl?: string;
}

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.scss']
})
export class ProductsComponent implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);
  private readonly mediaService = inject(MediaService);
  private readonly STORAGE_KEY = 'product_filters';
  private debounceTimer: any;

  products: ProductWithMedia[] = [];
  filteredProducts: ProductWithMedia[] = [];
  loading = false;
  error = '';

  // Search and filter properties
  searchKeyword = '';
  priceMin: number | null = null;
  priceMax: number | null = null;
  stockMin: number | null = null;
  stockMax: number | null = null;

  ngOnInit() {
    this.loadSavedFilters();
    this.loadProducts();
  }

  ngOnDestroy() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  }

  loadProducts() {
    this.loading = true;
    this.error = '';

    this.productService.getAllProducts().subscribe({
      next: (products) => {
        this.loadProductsWithMedia(products);
      },
      error: (err) => {
        this.error = 'Failed to load products';
        this.loading = false;
      }
    });
  }

  private loadProductsWithMedia(products: ProductResponse[]) {
    if (products.length === 0) {
      this.products = [];
      this.filteredProducts = [];
      this.loading = false;
      return;
    }

    // For each product, fetch its media
    const mediaRequests = products.map(product =>
      this.mediaService.getMediaByProductId(product.id).pipe(
        map(mediaList => ({
          ...product,
          imageUrl: mediaList.length > 0 ? mediaList[0].downloadUrl : undefined
        })),
        catchError(() => of({ ...product, imageUrl: undefined }))
      )
    );

    forkJoin(mediaRequests).subscribe({
      next: (productsWithMedia) => {
        this.products = productsWithMedia;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.products = products.map(p => ({ ...p, imageUrl: undefined }));
        this.applyFilters();
        this.loading = false;
      }
    });
  }

  onSearchChange() {
    this.applyFiltersWithDebounce();
  }

  onFilterChange() {
    this.applyFiltersWithDebounce();
  }

  private applyFiltersWithDebounce() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    this.debounceTimer = setTimeout(() => {
      this.applyFilters();
      this.saveFilters();
    }, 300);
  }

  applyFilters() {
    this.filteredProducts = this.products.filter(product => {
      const matchesKeyword = !this.searchKeyword ||
        product.name.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        product.description?.toLowerCase().includes(this.searchKeyword.toLowerCase());

      const matchesPriceMin = this.priceMin === null || product.price >= this.priceMin;
      const matchesPriceMax = this.priceMax === null || product.price <= this.priceMax;

      const matchesStockMin = this.stockMin === null || product.quantity >= this.stockMin;
      const matchesStockMax = this.stockMax === null || product.quantity <= this.stockMax;

      return matchesKeyword && matchesPriceMin && matchesPriceMax &&
             matchesStockMin && matchesStockMax;
    });
  }

  clearSearch() {
    this.searchKeyword = '';
    this.applyFiltersWithDebounce();
  }

  clearPriceMin() {
    this.priceMin = null;
    this.applyFiltersWithDebounce();
  }

  clearPriceMax() {
    this.priceMax = null;
    this.applyFiltersWithDebounce();
  }

  clearStockMin() {
    this.stockMin = null;
    this.applyFiltersWithDebounce();
  }

  clearStockMax() {
    this.stockMax = null;
    this.applyFiltersWithDebounce();
  }

  clearAllFilters() {
    this.searchKeyword = '';
    this.priceMin = null;
    this.priceMax = null;
    this.stockMin = null;
    this.stockMax = null;
    this.applyFilters();
    this.saveFilters();
  }

  private saveFilters() {
    const filters = {
      searchKeyword: this.searchKeyword,
      priceMin: this.priceMin,
      priceMax: this.priceMax,
      stockMin: this.stockMin,
      stockMax: this.stockMax
    };
    sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(filters));
  }

  private loadSavedFilters() {
    const saved = sessionStorage.getItem(this.STORAGE_KEY);
    if (saved) {
      try {
        const filters = JSON.parse(saved);
        this.searchKeyword = filters.searchKeyword || '';
        this.priceMin = filters.priceMin;
        this.priceMax = filters.priceMax;
        this.stockMin = filters.stockMin;
        this.stockMax = filters.stockMax;
      } catch (e) {
        // Invalid saved data, ignore
      }
    }
  }
}
