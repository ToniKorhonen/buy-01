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

// Extended interface with media for display purposes
export interface ProductWithMedia extends ProductResponse {
  imageUrl?: string; // URL of the first media item for this product
  mediaItems?: string[]; // All media URLs for this product
}
