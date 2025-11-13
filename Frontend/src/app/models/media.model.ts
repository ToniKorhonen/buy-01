export interface Media {
  id: string;
  filename: string;
  contentType: string;
  fileSize: number;
  uploaderId: string;
  uploadDate: string;
  downloadUrl: string;
}

export interface MediaUploadResponse {
  id: string;
  message: string;
  downloadUrl: string;
}

