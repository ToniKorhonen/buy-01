import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { MediaService } from '../../services/media.service';
import { UserResponse } from '../../models/user.model';

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
      },
      error: (err) => {
        this.error = 'Failed to load profile';
        this.loading = false;
        console.error('Error loading profile:', err);
      }
    });
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
    if (!this.editName || this.editName.trim().length < 2) {
      this.error = 'Name must be at least 2 characters';
      return;
    }

    if (!this.editEmail || !this.editEmail.includes('@')) {
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

  logout() {
    this.userService.logout();
    this.router.navigate(['/']);
  }
}

