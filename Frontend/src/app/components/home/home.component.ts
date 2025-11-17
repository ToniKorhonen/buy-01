import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html'
})
export class HomeComponent {
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  logout() {
    this.userService.logout();
    this.router.navigate(['/']);
  }

  goToProfile() {
    // TODO: Implement profile navigation
  }
}

