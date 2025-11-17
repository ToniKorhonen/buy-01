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
  selectedFile: File | null = null;
  avatarPreview: string | null = null;
  uploadingAvatar = false;

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];

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

    // If seller and has avatar, upload it first
    if (this.model.role === 'SELLER' && this.selectedFile) {
      this.uploadingAvatar = true;
      this.mediaService.uploadMedia(this.selectedFile, this.model.email).subscribe({
        next: (mediaResponse) => {
          // Store the media ID, not the URL
          this.model.avatarId = mediaResponse.id;
          this.registerUser();
        },
        error: (err) => {
          this.error = 'Failed to upload avatar';
          this.loading = false;
          this.uploadingAvatar = false;
        }
      });
    } else {
      this.registerUser();
    }
  }

  private registerUser() {
    this.userService.register(this.model).subscribe({
      next: (res) => {
        this.message = `Registered successfully! Redirecting to login...`;
        this.loading = false;
        this.uploadingAvatar = false;
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Registration failed';
        this.loading = false;
        this.uploadingAvatar = false;
      }
    });
  }
}

