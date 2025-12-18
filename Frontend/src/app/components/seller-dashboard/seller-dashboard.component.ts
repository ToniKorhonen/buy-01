import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { UserService } from '../../services/user.service';
import { MediaService } from '../../services/media.service';
import { ProductRequest, ProductResponse } from '../../models/product.model';
import { forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

// Extended product interface for display
interface ProductWithMedia extends ProductResponse {
  imageUrl?: string;
}

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
  private readonly mediaService = inject(MediaService);
  private readonly router = inject(Router);

  products: ProductWithMedia[] = [];
  loading = false;
  error = '';

  showForm = false;
  editingProduct: ProductResponse | null = null;

  formModel: ProductRequest = {
    name: '',
    description: '',
    price: 0,
    quantity: 0
  };

  selectedFile: File | null = null;
  imagePreview: string | null = null;
  uploadingImage = false;

  ngOnInit() {
    const user = this.userService.getCurrentUser();
    if (!user || user.role !== 'SELLER') {
      this.router.navigate(['/']);
      return;
    }
    this.loadProducts();
  }

  loadProducts() {
    this.loading = true;
    this.error = '';

    this.productService.getMyProducts().subscribe({
      next: (products) => {
        // Load media for each product
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


  openCreateForm() {
    this.showForm = true;
    this.editingProduct = null;
    this.selectedFile = null;
    this.imagePreview = null;
    this.formModel = {
      name: '',
      description: '',
      price: 0,
      quantity: 0
    };
  }

  openEditForm(product: ProductResponse) {
    this.showForm = true;
    this.editingProduct = product;
    this.selectedFile = null;
    this.imagePreview = null;

    // Load existing image for this product
    this.mediaService.getMediaByProductId(product.id).subscribe({
      next: (mediaList) => {
        if (mediaList.length > 0) {
          this.imagePreview = mediaList[0].downloadUrl;
        }
      },
      error: () => {
        // No image found, that's okay
      }
    });

    this.formModel = {
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity
    };
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];

      // Create preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreview = e.target?.result as string;
      };
      reader.readAsDataURL(this.selectedFile);
    }
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
        next: (updatedProduct) => {
          // If there's a new image, upload it
          if (this.selectedFile) {
            this.uploadProductImage(updatedProduct.id);
          } else {
            this.closeForm();
            this.loadProducts();
          }
        },
        error: (err) => {
          console.error('Error updating product:', err);
          this.error = err?.error?.message || 'Failed to update product';
        }
      });
    } else {
      // Create new product first, then upload image
      this.productService.createProduct(this.formModel).subscribe({
        next: (newProduct) => {
          // If there's an image, upload it with the product ID
          if (this.selectedFile) {
            this.uploadProductImage(newProduct.id);
          } else {
            this.closeForm();
            this.loadProducts();
          }
        },
        error: (err) => {
          console.error('Error creating product:', err);
          this.error = err?.error?.message || err?.error || 'Failed to create product';
        }
      });
    }
  }

  private uploadProductImage(productId: string) {
    if (!this.selectedFile) {
      this.closeForm();
      this.loadProducts();
      return;
    }

    this.uploadingImage = true;

    this.mediaService.uploadMedia(this.selectedFile, productId).subscribe({
      next: () => {
        this.uploadingImage = false;
        this.closeForm();
        this.loadProducts();
      },
      error: (err) => {
        console.error('Error uploading image:', err);
        this.error = 'Product saved but failed to upload image';
        this.uploadingImage = false;
        // Still reload products since product was created/updated
        this.loadProducts();
      }
    });
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

