export type OrderStatus = 'STARTED' | 'ONGOING' | 'DELIVERED' | 'CANCELLED';

export interface OrderRequest {
  productId: string;
  quantity: number;
}

export interface OrderResponse {
  id: string;
  buyerId: string;
  sellerId: string;
  productId: string;
  productName: string;
  quantity: number;
  totalPrice: number;
  status: OrderStatus;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateQuantityRequest {
  quantity: number;
}

