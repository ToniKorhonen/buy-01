import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { UserService } from '../../services/user.service';
import { ProductRequest, ProductResponse } from '../../models/product.model';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './seller-dashboard.component.html',
  styleUrls: ['./seller-dashboard.component.scss']
})
export class SellerDashboardComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  products: ProductResponse[] = [];
  loading = false;
  error = '';

  showForm = false;
  editingProduct: ProductResponse | null = null;

  formModel: ProductRequest = {
    name: '',
    description: '',
    price: 0,
    quantity: 0,
    userId: '',
    image: undefined
  };

  ngOnInit() {
    const user = this.userService.getCurrentUser();
    if (!user || user.role !== 'SELLER') {
      this.router.navigate(['/']);
      return;
    }
    this.formModel.userId = user.id;
    this.loadProducts();
  }

  loadProducts() {
    this.loading = true;
    this.error = '';

    this.productService.getAllProducts().subscribe({
      next: (products) => {
        // Filter to show only seller's own products
        // Backend stores email as userId (from JWT authentication)
        const userEmail = this.userService.getCurrentUser()?.email;
        this.products = products.filter(p => p.userId === userEmail);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load products';
        this.loading = false;
      }
    });
  }

  openCreateForm() {
    this.showForm = true;
    this.editingProduct = null;
    const user = this.userService.getCurrentUser();
    this.formModel = {
      name: '',
      description: '',
      price: 0,
      quantity: 0,
      userId: user?.id || '',
      image: undefined
    };
  }

  openEditForm(product: ProductResponse) {
    this.showForm = true;
    this.editingProduct = product;
    this.formModel = {
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
      userId: product.userId,
      image: product.image
    };
  }

  closeForm() {
    this.showForm = false;
    this.editingProduct = null;
  }

  submitForm() {
    this.error = '';

    if (this.editingProduct) {
      // Update existing product
      this.productService.updateProduct(this.editingProduct.id, this.formModel).subscribe({
        next: () => {
          this.closeForm();
          this.loadProducts();
        },
        error: (err) => {
          console.error('Error updating product:', err);
          this.error = err?.error?.message || 'Failed to update product';
        }
      });
    } else {
      // Create new product
      this.productService.createProduct(this.formModel).subscribe({
        next: () => {
          this.closeForm();
          this.loadProducts();
        },
        error: (err) => {
          console.error('Error creating product:', err);
          this.error = err?.error?.message || err?.error || 'Failed to create product';
        }
      });
    }
  }

  deleteProduct(product: ProductResponse) {
    if (!confirm(`Are you sure you want to delete "${product.name}"?`)) {
      return;
    }

    this.productService.deleteProduct(product.id).subscribe({
      next: () => {
        this.loadProducts();
      },
      error: (err) => {
        this.error = 'Failed to delete product';
      }
    });
  }

  logout() {
    this.userService.logout();
    this.router.navigate(['/']);
  }
}

