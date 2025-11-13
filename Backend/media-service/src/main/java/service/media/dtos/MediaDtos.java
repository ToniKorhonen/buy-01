package service.media.dtos;

import java.time.LocalDateTime;

public class MediaDtos {
    public record MediaResponse(
            String id,
            String filename,
            String contentType,
            Long fileSize,
            String uploaderId,
            LocalDateTime uploadDate,
            String downloadUrl
    ) {}

    public record MediaUploadResponse(
            String id,
            String message,
            String downloadUrl
    ) {}
}
