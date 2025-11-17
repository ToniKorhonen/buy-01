import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet, RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from './services/user.service';
import { MediaService } from './services/media.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterModule, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly userService = inject(UserService);
  protected readonly mediaService = inject(MediaService);

  avatarUrl: string | null = null;

  ngOnInit() {
    // Load avatar if user is authenticated and has an avatarId
    this.loadUserAvatar();
  }

  loadUserAvatar() {
    const user = this.userService.getCurrentUser();
    if (user?.avatarId) {
      this.mediaService.getMediaInfo(user.avatarId).subscribe({
        next: (media) => {
          this.avatarUrl = media.downloadUrl;
        },
        error: () => {
          // Avatar loading failed, just use default
          this.avatarUrl = null;
        }
      });
    }
  }

  get isAuthenticated() {
    return this.userService.isAuthenticated();
  }

  get currentUser() {
    return this.userService.getCurrentUser();
  }

  get isSeller() {
    return this.userService.isSeller();
  }

  logout() {
    this.userService.logout();
    this.avatarUrl = null;
  }
}
