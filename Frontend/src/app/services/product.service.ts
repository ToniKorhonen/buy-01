import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environment';
import { ProductRequest, ProductResponse } from '../models/product.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  // Public endpoint - get all products
  getAllProducts(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(`${this.base}/products`);
  }

  // Public endpoint - get single product
  getProduct(id: string): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.base}/products/${id}`);
  }

  // Authenticated - get current user's products
  getMyProducts(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(`${this.base}/products/my-products`);
  }

  // Authenticated - create product (seller only)
  createProduct(product: ProductRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`${this.base}/products`, product);
  }

  // Authenticated - update product (seller only, own products)
  updateProduct(id: string, product: ProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${this.base}/products/${id}`, product);
  }

  // Authenticated - delete product (seller only, own products)
  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/products/${id}`);
  }
}

