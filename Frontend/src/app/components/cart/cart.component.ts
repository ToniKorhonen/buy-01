import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { OrderResponse } from '../../models/order.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  cartItems: OrderResponse[] = [];
  loading = false;
  error = '';
  actionLoading: { [orderId: string]: boolean } = {};

  get totalPrice(): number {
    return this.cartItems.reduce((sum, item) => sum + item.totalPrice, 0);
  }

  ngOnInit() {
    this.loadCart();
  }

  loadCart() {
    this.loading = true;
    this.error = '';
    this.orderService.getMyCart().subscribe({
      next: (items: OrderResponse[]) => { this.cartItems = items; this.loading = false; },
      error: () => { this.error = 'Failed to load cart.'; this.loading = false; }
    });
  }

  updateQuantity(order: OrderResponse, qty: number) {
    if (qty < 1) return;
    this.actionLoading[order.id] = true;
    this.orderService.updateQuantity(order.id, { quantity: qty }).subscribe({
      next: (updated: OrderResponse) => {
        const idx = this.cartItems.findIndex(o => o.id === updated.id);
        if (idx !== -1) this.cartItems[idx] = updated;
        this.actionLoading[order.id] = false;
      },
      error: () => { this.error = 'Failed to update quantity.'; this.actionLoading[order.id] = false; }
    });
  }

  removeFromCart(order: OrderResponse) {
    this.actionLoading[order.id] = true;
    this.orderService.deleteOrder(order.id).subscribe({
      next: () => { this.cartItems = this.cartItems.filter(o => o.id !== order.id); this.actionLoading[order.id] = false; },
      error: () => { this.error = 'Failed to remove item.'; this.actionLoading[order.id] = false; }
    });
  }

  payOnDelivery(order: OrderResponse) {
    this.actionLoading[order.id] = true;
    this.orderService.placeOrder(order.id).subscribe({
      next: () => { this.cartItems = this.cartItems.filter(o => o.id !== order.id); this.actionLoading[order.id] = false; },
      error: () => { this.error = 'Failed to place order.'; this.actionLoading[order.id] = false; }
    });
  }

  payAllOnDelivery() {
    this.cartItems.forEach(item => this.payOnDelivery(item));
  }

  goToProducts() {
    this.router.navigate(['/products']);
  }
}

