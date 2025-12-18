export interface Media {
  id: string;
  productId?: string;
  downloadUrl: string;
}

export interface MediaUploadResponse {
  id: string;
  message: string;
  downloadUrl: string;
}

