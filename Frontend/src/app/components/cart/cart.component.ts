import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, concatMap } from 'rxjs/operators';
import { OrderService } from '../../services/order.service';
import { ProductService } from '../../services/product.service';
import { UserService } from '../../services/user.service';
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
  private readonly productService = inject(ProductService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  cartItems: OrderResponse[] = [];
  /** Maps productId -> current available stock */
  productStock: { [productId: string]: number } = {};
  loading = false;
  error = '';
  actionLoading: { [orderId: string]: boolean } = {};

  get totalPrice(): number {
    return this.cartItems.reduce((sum, item) => sum + item.totalPrice, 0);
  }

  get currentBalance(): number {
    return this.userService.getCurrentUser()?.balance ?? 0;
  }

  get insufficientFunds(): boolean {
    return this.cartItems.length > 0 && this.currentBalance < this.totalPrice;
  }

  get hasStockWarnings(): boolean {
    return this.cartItems.some(item => this.itemExceedsStock(item));
  }

  itemAffordable(item: OrderResponse): boolean {
    return this.currentBalance >= item.totalPrice;
  }

  itemExceedsStock(item: OrderResponse): boolean {
    const stock = this.productStock[item.productId];
    return stock !== undefined && item.quantity > stock;
  }

  availableStockFor(item: OrderResponse): number {
    return this.productStock[item.productId] ?? 0;
  }

  canPay(item: OrderResponse): boolean {
    return this.itemAffordable(item) && !this.itemExceedsStock(item);
  }

  ngOnInit() {
    this.userService.fetchUserProfile();
    this.loadCart();
  }

  loadCart() {
    this.loading = true;
    this.error = '';
    this.orderService.getMyCart().subscribe({
      next: (items: OrderResponse[]) => {
        this.cartItems = items;
        this.loading = false;
        this.loadStocks(items);
      },
      error: () => { this.error = 'Failed to load cart.'; this.loading = false; }
    });
  }

  private loadStocks(items: OrderResponse[]) {
    const uniqueProductIds = [...new Set(items.map(i => i.productId))];
    if (uniqueProductIds.length === 0) return;

    const requests = uniqueProductIds.map(id =>
      this.productService.getProduct(id).pipe(catchError(() => of(null)))
    );

    forkJoin(requests).subscribe(products => {
      products.forEach((product, idx) => {
        if (product) {
          this.productStock[uniqueProductIds[idx]] = product.quantity;
        }
      });
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
        this.error = '';
      },
      error: (err) => {
        this.error = err.error?.error ?? 'Failed to update quantity.';
        this.actionLoading[order.id] = false;
      }
    });
  }

  fixQuantityToStock(order: OrderResponse) {
    const available = this.availableStockFor(order);
    if (available <= 0) {
      this.removeFromCart(order);
    } else {
      this.updateQuantity(order, available);
    }
  }

  removeFromCart(order: OrderResponse) {
    this.actionLoading[order.id] = true;
    this.orderService.deleteOrder(order.id).subscribe({
      next: () => {
        this.cartItems = this.cartItems.filter(o => o.id !== order.id);
        this.actionLoading[order.id] = false;
        this.error = '';
      },
      error: () => { this.error = 'Failed to remove item.'; this.actionLoading[order.id] = false; }
    });
  }

  payOnDelivery(order: OrderResponse) {
    if (!this.canPay(order)) return;
    this.actionLoading[order.id] = true;
    this.error = '';
    this.orderService.placeOrder(order.id).subscribe({
      next: () => {
        this.cartItems = this.cartItems.filter(o => o.id !== order.id);
        this.actionLoading[order.id] = false;
        this.userService.fetchUserProfile();
      },
      error: (err) => {
        if (err.status === 402) {
          this.error = err.error?.error ?? 'Insufficient funds.';
        } else if (err.status === 409) {
          this.error = err.error?.error ?? 'Not enough stock to place this order.';
          // Refresh stock so the warning banner reflects current reality
          this.loadStocks(this.cartItems);
        } else {
          this.error = 'Failed to place order.';
        }
        this.actionLoading[order.id] = false;
        this.userService.fetchUserProfile();
      }
    });
  }

  payAllOnDelivery() {
    if (this.insufficientFunds || this.hasStockWarnings) return;
    this.error = '';
    // Place orders sequentially to avoid race conditions on shared product stock
    const payableItems = [...this.cartItems];
    payableItems.reduce<Observable<unknown>>(
      (chain, item) => chain.pipe(
        concatMap(() => this.orderService.placeOrder(item.id).pipe(
          catchError((err) => {
            if (err.status === 402) {
              this.error = err.error?.error ?? 'Insufficient funds.';
            } else if (err.status === 409) {
              this.error = err.error?.error ?? `Not enough stock for "${item.productName}".`;
              this.loadStocks(this.cartItems);
            } else {
              this.error = `Failed to place order for "${item.productName}".`;
            }
            // Stop the chain on first error
            throw err;
          })
        ))
      ),
      of(undefined)
    ).subscribe({
      error: () => {
        // Reload cart and balance to reflect partial success
        this.loadCart();
        this.userService.fetchUserProfile();
      },
      complete: () => {
        this.loadCart();
        this.userService.fetchUserProfile();
      }
    });
  }

  goToProducts() {
    this.router.navigate(['/products']);
  }
}






