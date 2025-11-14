import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { RegisterRequest } from '../../models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  private readonly userService = inject(UserService);

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
        this.message = `Registered ${res.email}`;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error || 'Registration failed';
        this.loading = false;
      }
    });
  }
}

