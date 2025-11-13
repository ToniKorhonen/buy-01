import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Media, MediaUploadResponse } from '../models/media.model';

@Injectable({
  providedIn: 'root'
})
export class MediaService {
  private apiUrl = 'http://localhost:8083/api/media';

  constructor(private http: HttpClient) {}

  uploadMedia(file: File, uploaderId?: string): Observable<MediaUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('uploaderId', uploaderId || 'anonymous');

    return this.http.post<MediaUploadResponse>(`${this.apiUrl}/upload`, formData);
  }

  getAllMedia(): Observable<Media[]> {
    return this.http.get<Media[]>(this.apiUrl);
  }

  getMediaByUploaderId(uploaderId: string): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.apiUrl}/uploader/${uploaderId}`);
  }

  getMediaUrl(mediaId: string): string {
    return `${this.apiUrl}/${mediaId}`;
  }

  deleteMedia(mediaId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${mediaId}`);
  }
}

