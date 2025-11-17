import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from './services/user.service';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterModule, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly userService = inject(UserService);
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
  }
}
