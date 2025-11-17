export interface Media {
  id: string;
  uploaderId?: string;
  productId?: string;
  downloadUrl: string;
}

export interface MediaUploadResponse {
  id: string;
  message: string;
  downloadUrl: string;
}

