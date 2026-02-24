import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environment';
import { OrderRequest, OrderResponse, UpdateQuantityRequest } from '../models/order.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  // Buyer: add product to cart (creates order with STARTED status)
  addToCart(req: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders`, req);
  }

  // Buyer: get own cart (STARTED orders)
  getMyCart(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.base}/orders/cart`);
  }

  // Buyer: get own order history (all non-cart orders)
  getMyOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.base}/orders/my-orders`);
  }

  // Seller: get incoming orders
  getSellerOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.base}/orders/seller-orders`);
  }

  // Update quantity (buyer, STARTED orders only)
  updateQuantity(orderId: string, req: UpdateQuantityRequest): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.base}/orders/${orderId}/quantity`, req);
  }

  // Buyer: place order (STARTED -> pays, triggers ONGOING on seller side)
  placeOrder(orderId: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.base}/orders/${orderId}/place`, {});
  }

  // Seller: mark as ongoing
  markOngoing(orderId: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.base}/orders/${orderId}/ongoing`, {});
  }

  // Buyer: mark as delivered
  markDelivered(orderId: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.base}/orders/${orderId}/delivered`, {});
  }

  // Cancel order (buyer or seller)
  cancelOrder(orderId: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.base}/orders/${orderId}/cancel`, {});
  }

  // Remove from cart / delete order (STARTED only)
  deleteOrder(orderId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/orders/${orderId}`);
  }

  // Reorder: update existing cancelled/delivered order back to STARTED with new quantity
  reorder(orderId: string, req: UpdateQuantityRequest): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`${this.base}/orders/${orderId}/reorder`, req);
  }
}

