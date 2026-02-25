import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../services/order.service';
import { UserService } from '../../services/user.service';
import { OrderResponse } from '../../models/order.model';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seller-orders.component.html',
  styleUrls: ['./seller-orders.component.scss']
})
export class SellerOrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  orders: OrderResponse[] = [];
  loading = false;
  error = '';
  actionLoading: { [orderId: string]: boolean } = {};

  get pendingOrders(): OrderResponse[] {
    return this.orders.filter(o => o.status === 'STARTED');
  }

  get activeOrders(): OrderResponse[] {
    return this.orders.filter(o => o.status === 'ONGOING');
  }

  get historyOrders(): OrderResponse[] {
    return this.orders.filter(o => o.status === 'DELIVERED' || o.status === 'CANCELLED');
  }

  ngOnInit() { this.loadOrders(); }

  loadOrders() {
    this.loading = true;
    this.error = '';
    this.orderService.getSellerOrders().subscribe({
      next: (orders: OrderResponse[]) => { this.orders = orders; this.loading = false; },
      error: () => { this.error = 'Failed to load orders.'; this.loading = false; }
    });
  }

  markOngoing(order: OrderResponse) {
    this.actionLoading[order.id] = true;
    this.orderService.markOngoing(order.id).subscribe({
      next: (updated: OrderResponse) => { this.replaceOrder(updated); this.actionLoading[order.id] = false; },
      error: () => { this.error = 'Failed to update order.'; this.actionLoading[order.id] = false; }
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

  removeFromList(order: OrderResponse) {
    this.orders = this.orders.filter(o => o.id !== order.id);
  }

  private replaceOrder(updated: OrderResponse) {
    const idx = this.orders.findIndex(o => o.id === updated.id);
    if (idx !== -1) this.orders[idx] = updated; else this.orders.unshift(updated);
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = { ADDED: '🛒 In Cart', STARTED: '💳 Paid', ONGOING: '🚚 Ongoing', DELIVERED: '✅ Delivered', CANCELLED: '❌ Cancelled' };
    return map[status] ?? status;
  }
}

