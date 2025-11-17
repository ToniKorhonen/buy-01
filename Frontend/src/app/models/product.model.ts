export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId: string;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId: string;
}
