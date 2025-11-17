import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UserService } from '../../services/user.service';
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
  private readonly router = inject(Router);

  model: RegisterRequest = { name: '', email: '', password: '', role: 'CLIENT' };
  loading = false;
  message = '';
  error = '';

  submit() {
    this.loading = true;
    this.message = '';
    this.error = '';
    this.userService.register(this.model).subscribe({
      next: (res) => {
        this.message = `Registered successfully! Redirecting to login...`;
        this.loading = false;
        // Redirect to login after successful registration
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Registration failed';
        this.loading = false;
      }
    });
  }
}

