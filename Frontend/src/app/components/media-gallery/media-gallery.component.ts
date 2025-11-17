import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MediaService } from '../../services/media.service';
import { Media, MediaUploadResponse } from '../../models/media.model';

@Component({
  selector: 'app-media-gallery',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './media-gallery.component.html',
  styleUrls: ['./media-gallery.component.scss']
})
export class MediaGalleryComponent implements OnInit {
  mediaList: Media[] = [];
  selectedFile: File | null = null;
  uploaderId: string = '';
  isLoading: boolean = false;
  uploadMessage: string = '';
  uploadSuccess: boolean = false;
  previewUrl: string | null = null;

  private readonly MAX_FILE_SIZE = 2 * 1024 * 1024;
  private readonly ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/gif'];

  constructor(private mediaService: MediaService) {}

  ngOnInit(): void {
    this.loadMedia();
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      if (!this.ALLOWED_TYPES.includes(file.type.toLowerCase())) {
        this.uploadMessage = '❌ Invalid file type. Only PNG, JPG, and GIF images are allowed.';
        this.uploadSuccess = false;
        this.selectedFile = null;
        this.previewUrl = null;
        event.target.value = ''; // Reset file input
        return;
      }

      // Validate file size
      if (file.size > this.MAX_FILE_SIZE) {
        this.uploadMessage = `❌ File size exceeds 2MB limit. Your file is ${this.formatBytes(file.size)}.`;
        this.uploadSuccess = false;
        this.selectedFile = null;
        this.previewUrl = null;
        event.target.value = ''; // Reset file input
        return;
      }

      // File is valid
      this.selectedFile = file;
      this.uploadMessage = ''; // Clear any previous error messages

      // Create preview for images
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.previewUrl = e.target.result;
        };
        reader.readAsDataURL(file);
      }
    }
  }

  uploadMedia(): void {
    if (!this.selectedFile) {
      this.uploadMessage = 'Please select a file first!';
      this.uploadSuccess = false;
      return;
    }

    this.isLoading = true;
    this.uploadMessage = '';

    this.mediaService.uploadMedia(this.selectedFile, this.uploaderId || 'anonymous')
      .subscribe({
        next: (response: MediaUploadResponse) => {
          this.uploadMessage = `✅ ${response.message}`;
          this.uploadSuccess = true;
          this.isLoading = false;
          this.selectedFile = null;
          this.previewUrl = null;
          this.uploaderId = '';

          // Reset file input
          const fileInput = document.getElementById('fileInput') as HTMLInputElement;
          if (fileInput) fileInput.value = '';

          // Reload media list
          this.loadMedia();
        },
        error: (error) => {
          this.uploadMessage = `❌ Upload failed: ${error.error?.message || error.message}`;
          this.uploadSuccess = false;
          this.isLoading = false;
        }
      });
  }

  loadMedia(): void {
    this.isLoading = true;
    this.mediaService.getAllMedia().subscribe({
      next: (media: Media[]) => {
        this.mediaList = media;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load media:', error);
        this.isLoading = false;
      }
    });
  }

  getMediaUrl(media: Media): string {
    return this.mediaService.getMediaUrl(media.id);
  }

  deleteMedia(mediaId: string): void {
    if (confirm('Are you sure you want to delete this media?')) {
      this.mediaService.deleteMedia(mediaId).subscribe({
        next: () => {
          this.uploadMessage = '✅ Media deleted successfully';
          this.uploadSuccess = true;
          this.loadMedia();
        },
        error: (error) => {
          this.uploadMessage = `❌ Failed to delete: ${error.message}`;
          this.uploadSuccess = false;
        }
      });
    }
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
}

