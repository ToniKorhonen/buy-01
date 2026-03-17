import { Component, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { switchMap, catchError, of } from 'rxjs';
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
export class RegisterComponent implements OnDestroy {
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

  private readonly MAX_FILE_SIZE = 2 * 1024 * 1024;
  private readonly ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif'];
  private readonly CONVERTED_TYPES = ['image/webp', 'image/bmp', 'image/tiff'];

  private redirectTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnDestroy(): void {
    if (this.redirectTimer) clearTimeout(this.redirectTimer);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    this.warning = '';
    this.error = '';

    if (!file) return;

    if (file.size > this.MAX_FILE_SIZE) {
      const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
      this.error = `File size (${sizeMB}MB) exceeds the 2MB limit. Please choose a smaller image.`;
      this.clearFileSelection(input);
      return;
    }

    if (!this.ALLOWED_TYPES.includes(file.type)) {
      if (this.CONVERTED_TYPES.includes(file.type)) {
        this.warning = `⚠️ ${file.type.split('/')[1].toUpperCase()} format will be converted to JPG/PNG.`;
      } else {
        this.error = `File type "${file.type}" is not supported. Please upload PNG, JPG, or GIF images only.`;
        this.clearFileSelection(input);
        return;
      }
    }

    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = (e) => { this.avatarPreview = e.target?.result as string; };
    reader.readAsDataURL(file);
  }

  submit(): void {
    this.loading = true;
    this.message = '';
    this.error = '';

    const needsAvatarUpload = this.model.role === 'SELLER' && !!this.selectedFile;

    if (needsAvatarUpload) {
      this.registerWithAvatar();
    } else {
      this.registerOnly();
    }
  }

  private registerOnly(): void {
    this.userService.register(this.model).subscribe({
      next: () => this.redirectWithMessage('Registered successfully! Redirecting to login...'),
      error: (err: any) => this.handleError(err, 'Registration failed')
    });
  }

  private registerWithAvatar(): void {
    this.userService.register(this.model).pipe(
      switchMap(() => this.userService.login({ email: this.model.email, password: this.model.password })),
      switchMap(() => {
        this.uploadingAvatar = true;
        return this.mediaService.uploadMedia(this.selectedFile!);
      }),
      switchMap((mediaResponse) => this.userService.updateProfile({ avatarId: mediaResponse.id }).pipe(
        catchError((err) => {
          console.error('Failed to update profile with avatar:', err);
          return of(null);
        })
      )),
      catchError((err) => {
        console.error('Registration with avatar failed:', err);
        return of(null);
      })
    ).subscribe(() => {
      this.redirectWithMessage('Registered successfully! Redirecting to login...');
    });
  }

  private redirectWithMessage(msg: string): void {
    this.message = msg;
    this.loading = false;
    this.uploadingAvatar = false;
    this.redirectTimer = setTimeout(() => this.router.navigate(['/login']), 2000);
  }

  private handleError(err: any, fallback: string): void {
    this.error = err?.error?.message || fallback;
    this.loading = false;
    this.uploadingAvatar = false;
  }

  private clearFileSelection(input: HTMLInputElement): void {
    this.selectedFile = null;
    this.avatarPreview = null;
    input.value = '';
  }
}
