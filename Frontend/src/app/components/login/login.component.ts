import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { UserService } from '../../services/user.service';
import { LoginRequest } from '../../models/user.model';

interface ValidationError {
  field: string;
  message: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  model: LoginRequest = { email: '', password: '' };
  loading = false;
  error = '';
  validationErrors: ValidationError[] = [];
  fieldErrors: { [key: string]: string } = {};

  submit() {
    this.loading = true;
    this.error = '';
    this.validationErrors = [];
    this.fieldErrors = {};

    this.userService.login(this.model).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
        const user = this.userService.getCurrentUser();

        // Navigate based on user role
        if (user?.role === 'SELLER') {
          this.router.navigate(['/seller/dashboard']);
        } else {
          this.router.navigate([returnUrl]);
        }
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.extractErrorMessages(err);
      }
    });
  }

  private extractErrorMessages(err: any): void {
    // Handle validation errors from backend
    if (err?.error?.errors) {
      this.validationErrors = err.error.errors;
      this.validationErrors.forEach((err: ValidationError) => {
        this.fieldErrors[err.field] = err.message;
      });
      this.error = 'Please fix the errors below';
    } else if (err?.error?.message) {
      // Handle specific error messages
      const msg = err.error.message;
      if (msg.includes('Email') || msg.includes('email')) {
        this.fieldErrors['email'] = msg;
      } else if (msg.includes('Password') || msg.includes('password')) {
        this.fieldErrors['password'] = msg;
      } else {
        this.error = msg;
      }
    } else if (err?.status === 401) {
      this.error = 'Invalid email or password. Please check your credentials.';
    } else if (err?.status === 400) {
      this.error = 'Invalid input. Please check your email and password.';
    } else {
      this.error = 'Login failed. Please try again later.';
    }
  }
}

