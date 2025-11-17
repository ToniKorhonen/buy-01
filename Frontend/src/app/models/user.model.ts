export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role?: 'CLIENT' | 'SELLER';
  avatar?: string;
}

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  role: 'CLIENT' | 'SELLER';
  avatar?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: 'CLIENT' | 'SELLER';
  };
}

