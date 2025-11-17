import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';
import { LoginRequest } from '../../models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  model: LoginRequest = { email: '', password: '' };
  message = '';
  error = '';

  get token() { return this.userService.token; }

  login() {
    this.message = '';
    this.error = '';
    this.userService.login(this.model).subscribe({
      next: () => {
        this.message = 'Logged in';
        this.router.navigate(['/home']);
      },
      error: (err) => this.error = err?.error || 'Login failed'
    });
  }

  logout() {
    this.userService.logout();
    this.message = 'Logged out';
  }
}

