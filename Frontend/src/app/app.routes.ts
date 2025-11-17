import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { RegisterComponent } from './components/register/register.component';
import { LoginComponent } from './components/login/login.component';
import { ProductsComponent } from './components/products/products.component';
import { SellerDashboardComponent } from './components/seller-dashboard/seller-dashboard.component';
import { sellerGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'login', component: LoginComponent },
  { path: 'products', component: ProductsComponent },
  { path: 'seller/dashboard', component: SellerDashboardComponent, canActivate: [sellerGuard] }
];
