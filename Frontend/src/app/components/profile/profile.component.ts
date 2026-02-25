import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';
import { UserResponse } from '../../models/user.model';
import { OrderResponse } from '../../models/order.model';

// User Statistics Interfaces
interface PurchasedProduct {
  id: string;
  name: string;
  imageUrl?: string;
  price: number;
  purchaseCount: number;
  totalSpent: number;
}

interface UserStatistics {
  topPurchasedProducts: PurchasedProduct[];
  mostBoughtProducts: PurchasedProduct[];
  totalMoneySpent: number;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly mediaService = inject(MediaService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  user: UserResponse | null = null;
  avatarUrl: string | null = null;
  error = '';
  loading = true;
  isEditing = false;
  updating = false;
  showDeleteConfirm = false;

  // Form fields
  editName = '';
  editEmail = '';
  editPassword = '';
  editPasswordConfirm = '';

  // Top-up
  showTopUp = false;
  topUpAmount: number | null = null;
  topUpLoading = false;
  topUpError = '';
  topUpSuccess = '';

  // User Statistics (placeholder data - will be populated from backend later)
  userStats: UserStatistics = {
    topPurchasedProducts: [],
    mostBoughtProducts: [],
    totalMoneySpent: 0
  };

  ngOnInit() {
    this.loadUserProfile();
  }

  loadUserProfile() {
    this.loading = true;
    this.error = '';

    this.userService.getProfile().subscribe({
      next: (user) => {
        this.user = user;
        this.loading = false;

        // Load avatar if user has one
        if (user.avatarId) {
          this.loadAvatar(user.avatarId);
        }

        // Sync totalMoneySpent from the live user object
        this.userStats.totalMoneySpent = user.moneySpent ?? 0;

        // Build per-product breakdown from delivered order history
        if (user.role === 'CLIENT') {
          this.loadOrderStats();
        }
      },
      error: (err) => {
        this.error = 'Failed to load profile';
        this.loading = false;
        console.error('Error loading profile:', err);
      }
    });
  }

  private loadOrderStats(): void {
    this.orderService.getMyOrders().subscribe({
      next: (orders) => {
        const delivered = orders.filter(o => o.status === 'DELIVERED');
        this.userStats = this.computeBuyerStats(delivered);
      },
      error: () => {
        // Stats unavailable — keep totals already set from user profile
      }
    });
  }

  private computeBuyerStats(orders: OrderResponse[]): UserStatistics {
    const map = new Map<string, PurchasedProduct>();

    for (const order of orders) {
      const unitPrice = order.quantity > 0 ? order.totalPrice / order.quantity : 0;
      const existing = map.get(order.productId);
      if (existing) {
        existing.purchaseCount += order.quantity;
        existing.totalSpent += order.totalPrice;
      } else {
        map.set(order.productId, {
          id: order.productId,
          name: order.productName,
          price: unitPrice,
          purchaseCount: order.quantity,
          totalSpent: order.totalPrice
        });
      }
    }

    const products = Array.from(map.values());
    return {
      totalMoneySpent: this.user?.moneySpent ?? 0,
      topPurchasedProducts: [...products].sort((a, b) => b.totalSpent - a.totalSpent).slice(0, 5),
      mostBoughtProducts: [...products].sort((a, b) => b.purchaseCount - a.purchaseCount).slice(0, 5)
    };
  }

  loadAvatar(avatarId: string) {
    this.mediaService.getMediaInfo(avatarId).subscribe({
      next: (media) => {
        this.avatarUrl = media.downloadUrl;
      },
      error: () => {
        // Avatar not found, that's okay
        this.avatarUrl = null;
      }
    });
  }

  startEdit() {
    if (!this.user) return;
    this.isEditing = true;
    this.editName = this.user.name;
    this.editEmail = this.user.email;
    this.editPassword = '';
    this.editPasswordConfirm = '';
    this.error = '';
  }

  cancelEdit() {
    this.isEditing = false;
    this.editName = '';
    this.editEmail = '';
    this.editPassword = '';
    this.editPasswordConfirm = '';
    this.error = '';
  }

  saveProfile() {
    if (!this.user) return;

    // Validation
    if (this.editName?.trim().length < 2) {
      this.error = 'Name must be at least 2 characters';
      return;
    }

    if (!this.editEmail?.includes('@')) {
      this.error = 'Please enter a valid email';
      return;
    }

    if (this.editPassword) {
      if (this.editPassword.length < 8) {
        this.error = 'Password must be at least 8 characters';
        return;
      }
      if (this.editPassword !== this.editPasswordConfirm) {
        this.error = 'Passwords do not match';
        return;
      }
    }

    this.updating = true;
    this.error = '';

    const updates: any = {
      name: this.editName,
      email: this.editEmail
    };

    if (this.editPassword) {
      updates.password = this.editPassword;
    }

    this.userService.updateProfile(updates).subscribe({
      next: (updatedUser) => {
        this.user = updatedUser;
        this.updating = false;
        this.isEditing = false;
        this.editPassword = '';
        this.editPasswordConfirm = '';
      },
      error: (err) => {
        this.updating = false;
        if (err.status === 409) {
          this.error = 'Email already in use';
        } else {
          this.error = 'Failed to update profile';
        }
        console.error('Error updating profile:', err);
      }
    });
  }

  confirmDelete() {
    this.showDeleteConfirm = true;
  }

  cancelDelete() {
    this.showDeleteConfirm = false;
  }

  deleteAccount() {
    if (!confirm('Are you absolutely sure? This action cannot be undone!')) {
      return;
    }

    this.updating = true;
    this.error = '';

    this.userService.deleteAccount().subscribe({
      next: () => {
        alert('Your account has been deleted successfully.');
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.updating = false;
        this.error = 'Failed to delete account';
        console.error('Error deleting account:', err);
      }
    });
  }

  openTopUp() {
    this.showTopUp = true;
    this.topUpAmount = null;
    this.topUpError = '';
    this.topUpSuccess = '';
  }

  closeTopUp() {
    this.showTopUp = false;
    this.topUpAmount = null;
    this.topUpError = '';
    this.topUpSuccess = '';
  }

  submitTopUp() {
    const amount = this.topUpAmount;
    if (!amount || amount <= 0) {
      this.topUpError = 'Please enter a positive amount.';
      return;
    }
    this.topUpLoading = true;
    this.topUpError = '';
    this.topUpSuccess = '';
    this.userService.topUp(amount).subscribe({
      next: (updated) => {
        this.user = updated;
        this.topUpLoading = false;
        this.topUpSuccess = `$${amount.toFixed(2)} added to your wallet.`;
        this.topUpAmount = null;
      },
      error: () => {
        this.topUpLoading = false;
        this.topUpError = 'Failed to add funds. Please try again.';
      }
    });
  }

  logout() {
    this.userService.logout();
    this.router.navigate(['/login']);
  }
}

