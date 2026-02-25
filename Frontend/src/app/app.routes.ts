import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { HomeComponent } from './components/home/home.component';
import { RegisterComponent } from './components/register/register.component';
import { ProductsComponent } from './components/products/products.component';
import { SellerDashboardComponent } from './components/seller-dashboard/seller-dashboard.component';
import { sellerGuard, authGuard } from './guards/auth.guard';
import { MediaGalleryComponent } from './components/media-gallery/media-gallery.component';
import { ProfileComponent } from './components/profile/profile.component';
import { CartComponent } from './components/cart/cart.component';
import { OrderDashboardComponent } from './components/order-dashboard/order-dashboard.component';
import { SellerOrdersComponent } from './components/seller-orders/seller-orders.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'login', component: LoginComponent },
  { path: 'products', component: ProductsComponent },
  { path: 'seller/dashboard', component: SellerDashboardComponent, canActivate: [sellerGuard] },
  { path: 'seller/orders', component: SellerOrdersComponent, canActivate: [sellerGuard] },
  { path: 'media', component: MediaGalleryComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'cart', component: CartComponent, canActivate: [authGuard] },
  { path: 'orders', component: OrderDashboardComponent, canActivate: [authGuard] }
];
