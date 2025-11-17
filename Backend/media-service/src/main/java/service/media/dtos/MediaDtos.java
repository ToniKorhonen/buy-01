package service.media.dtos;

public class MediaDtos {
    public record MediaResponse(
            String id,
            String uploaderId,
            String downloadUrl
    ) {}

    public record MediaUploadResponse(
            String id,
            String message,
            String downloadUrl
    ) {}

    public record MessageResponse(
            String message
    ) {}
}
