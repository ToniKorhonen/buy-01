import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../services/product.service';
import { ProductResponse } from '../../models/product.model';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.scss']
})
export class ProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);

  products: ProductResponse[] = [];
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
        this.products = products;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load products';
        this.loading = false;
      }
    });
  }
}

