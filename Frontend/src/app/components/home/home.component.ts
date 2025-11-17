import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

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
    this.router.navigate(['/']);
  }
}

