import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  imports: [CommonModule],
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.scss']
})
export class ProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly mediaService = inject(MediaService);

  products: ProductWithMedia[] = [];
  loading = false;
  error = '';

  ngOnInit() {
    this.loadProducts();
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
        this.loading = false;
      },
      error: () => {
        // If media loading fails, still show products without images
        this.products = products.map(p => ({ ...p, imageUrl: undefined }));
        this.loading = false;
      }
    });
  }
}

