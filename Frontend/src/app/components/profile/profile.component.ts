import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { MediaService } from '../../services/media.service';
import { UserResponse } from '../../models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
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

  logout() {
    this.userService.logout();
    this.router.navigate(['/']);
  }
}

