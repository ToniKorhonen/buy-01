import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../services/order.service';
import { UserService } from '../../services/user.service';
import { OrderResponse } from '../../models/order.model';

@Component({
  selector: 'app-order-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-dashboard.component.html',
  styleUrls: ['./order-dashboard.component.scss']
})
export class OrderDashboardComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  orders: OrderResponse[] = [];
  loading = false;
  error = '';
  actionLoading: { [orderId: string]: boolean } = {};
  reorderQty: { [orderId: string]: number } = {};

  get activeOrders(): OrderResponse[] {
    return this.orders.filter(o => o.status === 'STARTED' || o.status === 'ONGOING');
  }

  get historyOrders(): OrderResponse[] {
    return this.orders.filter(o => o.status === 'DELIVERED' || o.status === 'CANCELLED');
  }

  ngOnInit() { this.loadOrders(); }

  loadOrders() {
    this.loading = true;
    this.error = '';
    this.orderService.getMyOrders().subscribe({
      next: (orders: OrderResponse[]) => {
        this.orders = orders;
        orders.forEach((o: OrderResponse) => { if (!this.reorderQty[o.id]) this.reorderQty[o.id] = o.quantity; });
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load orders.'; this.loading = false; }
    });
  }

  markDelivered(order: OrderResponse) {
    this.actionLoading[order.id] = true;
    this.orderService.markDelivered(order.id).subscribe({
      next: (updated: OrderResponse) => {
        this.replaceOrder(updated);
        this.actionLoading[order.id] = false;
        this.userService.fetchUserProfile();
      },
      error: () => { this.error = 'Failed to mark delivered.'; this.actionLoading[order.id] = false; }
    });
  }

  cancelOrder(order: OrderResponse) {
    this.actionLoading[order.id] = true;
    this.orderService.cancelOrder(order.id).subscribe({
      next: (updated: OrderResponse) => {
        this.replaceOrder(updated);
        this.actionLoading[order.id] = false;
        this.userService.fetchUserProfile();
      },
      error: () => { this.error = 'Failed to cancel order.'; this.actionLoading[order.id] = false; }
    });
  }

  reorder(order: OrderResponse) {
    const qty = this.reorderQty[order.id] ?? order.quantity;
    this.actionLoading[order.id] = true;
    this.orderService.reorder(order.id, { quantity: qty }).subscribe({
      next: (updated: OrderResponse) => { this.replaceOrder(updated); this.actionLoading[order.id] = false; },
      error: () => { this.error = 'Failed to reorder.'; this.actionLoading[order.id] = false; }
    });
  }

  removeFromList(order: OrderResponse) {
    this.orders = this.orders.filter(o => o.id !== order.id);
  }

  private replaceOrder(updated: OrderResponse): void {
    const idx = this.orders.findIndex(o => o.id === updated.id);
    if (idx === -1) {
      this.orders.unshift(updated);
    } else {
      this.orders[idx] = updated;
    }
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = { ADDED: '🛒 In Cart', STARTED: '💳 Paid - Awaiting Seller', ONGOING: '🚚 On the Way', DELIVERED: '✅ Delivered', CANCELLED: '❌ Cancelled' };
    return map[status] ?? status;
  }
}

