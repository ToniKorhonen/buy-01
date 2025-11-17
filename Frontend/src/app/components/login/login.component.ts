import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { UserService } from '../../services/user.service';
import { LoginRequest } from '../../models/user.model';

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

  submit() {
    this.loading = true;
    this.error = '';

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
        this.error = 'Invalid email or password';
        this.loading = false;
      }
    });
  }
}

