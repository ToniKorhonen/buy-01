import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UserService } from '../../services/user.service';
import { MediaService } from '../../services/media.service';
import { RegisterRequest } from '../../models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  private readonly userService = inject(UserService);
  private readonly mediaService = inject(MediaService);
  private readonly router = inject(Router);

  model: RegisterRequest = { name: '', email: '', password: '', role: 'CLIENT' };
  loading = false;
  message = '';
  error = '';
  warning = '';
  selectedFile: File | null = null;
  avatarPreview: string | null = null;
  uploadingAvatar = false;

  // File validation constants
  private readonly MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
  private readonly ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif'];
  private readonly CONVERTED_TYPES = ['image/webp', 'image/bmp', 'image/tiff'];

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      this.warning = '';
      this.error = '';

      // Validate file size
      if (file.size > this.MAX_FILE_SIZE) {
        const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
        this.error = `File size (${sizeMB}MB) exceeds the maximum allowed size of 2MB. Please choose a smaller image.`;
        this.selectedFile = null;
        this.avatarPreview = null;
        input.value = ''; // Clear the input
        return;
      }

      // Validate file type
      if (!this.ALLOWED_TYPES.includes(file.type)) {
        if (this.CONVERTED_TYPES.includes(file.type)) {
          this.warning = `⚠️ ${file.type.split('/')[1].toUpperCase()} format will be converted to JPG/PNG. This may affect image quality.`;
        } else {
          this.error = `File type "${file.type}" is not supported. Please upload PNG, JPG, or GIF images only.`;
          this.selectedFile = null;
          this.avatarPreview = null;
          input.value = '';
          return;
        }
      }

      this.selectedFile = file;

      // Create preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.avatarPreview = e.target?.result as string;
      };
      reader.readAsDataURL(this.selectedFile);
    }
  }

  submit() {
    this.loading = true;
    this.message = '';
    this.error = '';

    // Register user first
    this.registerUser();
  }

  private registerUser() {
    this.userService.register(this.model).subscribe({
      next: () => {
        // If seller with avatar, auto-login and upload avatar
        if (this.model.role === 'SELLER' && this.selectedFile) {
          this.autoLoginAndUploadAvatar();
        } else {
          this.message = `Registered successfully! Redirecting to login...`;
          this.loading = false;
          this.uploadingAvatar = false;
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        }
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Registration failed';
        this.loading = false;
        this.uploadingAvatar = false;
      }
    });
  }

  private autoLoginAndUploadAvatar() {
    // Auto-login to get JWT token
    this.userService.login({ email: this.model.email, password: this.model.password }).subscribe({
      next: () => {
        // Now upload avatar with authentication
        this.uploadingAvatar = true;
        this.mediaService.uploadMedia(this.selectedFile!).subscribe({
          next: (mediaResponse) => {
            // Update user profile with avatar ID
            this.userService.updateProfile({ avatarId: mediaResponse.id }).subscribe({
              next: () => {
                this.message = `Registered successfully with avatar! Redirecting to login...`;
                this.loading = false;
                this.uploadingAvatar = false;
                setTimeout(() => {
                  this.router.navigate(['/login']);
                }, 2000);
              },
              error: (err: any) => {
                console.error('Failed to update profile with avatar:', err);
                this.message = `Registered successfully, but avatar update failed. Redirecting to login...`;
                this.loading = false;
                this.uploadingAvatar = false;
                setTimeout(() => {
                  this.router.navigate(['/login']);
                }, 2000);
              }
            });
          },
          error: (err: any) => {
            console.error('Upload error:', err);
            this.message = `Registered successfully, but avatar upload failed. Redirecting to login...`;
            this.loading = false;
            this.uploadingAvatar = false;
            setTimeout(() => {
              this.router.navigate(['/login']);
            }, 2000);
          }
        });
      },
      error: (err: any) => {
        console.error('Auto-login failed:', err);
        this.message = `Registered successfully! Please login manually.`;
        this.loading = false;
        this.uploadingAvatar = false;
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      }
    });
  }
}

