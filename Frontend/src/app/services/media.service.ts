import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Media, MediaUploadResponse } from '../models/media.model';
import { environment } from '../environment';

@Injectable({
  providedIn: 'root'
})
export class MediaService {
  private apiUrl = `${environment.apiBaseUrl}/media`;

  constructor(private http: HttpClient) {}

  uploadMedia(file: File, productId?: string): Observable<MediaUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (productId) {
      formData.append('productId', productId);
    }

    return this.http.post<MediaUploadResponse>(`${this.apiUrl}/upload`, formData);
  }

  getAllMedia(): Observable<Media[]> {
    return this.http.get<Media[]>(this.apiUrl);
  }


  getMediaByProductId(productId: string): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.apiUrl}/product/${productId}`);
  }

  getMediaInfo(mediaId: string): Observable<Media> {
    return this.http.get<Media>(`${this.apiUrl}/${mediaId}/info`);
  }

  getMediaUrl(mediaId: string): string {
    return `${this.apiUrl}/${mediaId}`;
  }

  deleteMedia(mediaId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${mediaId}`);
  }
}

