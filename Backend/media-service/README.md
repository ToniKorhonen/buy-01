# Media Service

A microservice for handling media file uploads and storage using Spring Boot and MongoDB.

## Features

- Upload pictures and other media files
- Store files on disk with metadata in MongoDB
- Retrieve all uploaded media
- Filter media by uploader ID
- Download/view media files
- Delete media files

## Prerequisites

- Java 17 or higher
- MongoDB running on localhost:27017
- Maven (wrapper included)

## Configuration

The service runs on port **8083** by default. Configuration can be found in `src/main/resources/application.properties`:

```properties
server.port=8083
spring.application.name=media-service

# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=media_db

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Media Storage Path
media.storage.path=./uploads
```

## Running the Service

### Option 1: Using Maven Wrapper (Recommended)

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### Option 2: Using Compiled JAR

```bash
# Build the project
mvnw.cmd clean install

# Run the JAR
java -jar target/media-0.0.1-SNAPSHOT.jar
```

## Testing the Service

### Option 1: Using the Test HTML Page

1. Start the media service
2. Open `test-upload.html` in your web browser
3. Upload pictures and view all uploaded media

### Option 2: Using cURL

**Upload a file:**
```bash
curl -X POST http://localhost:8083/api/media/upload \
  -F "file=@path/to/your/image.jpg" \
  -F "uploaderId=user123"
```

**Get all media:**
```bash
curl http://localhost:8083/api/media
```

**Get media by uploader:**
```bash
curl http://localhost:8083/api/media/uploader/user123
```

**View a specific media file:**
```bash
curl http://localhost:8083/api/media/{mediaId}
```

**Get media metadata:**
```bash
curl http://localhost:8083/api/media/{mediaId}/info
```

**Delete media:**
```bash
curl -X DELETE http://localhost:8083/api/media/{mediaId}
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/media/upload` | Upload a new media file |
| GET | `/api/media` | Get all uploaded media (metadata) |
| GET | `/api/media/uploader/{uploaderId}` | Get media by uploader ID |
| GET | `/api/media/{id}` | Download/view media file |
| GET | `/api/media/{id}/info` | Get media metadata |
| DELETE | `/api/media/{id}` | Delete media file |

## Architecture

The service follows the standard Spring Boot architecture used across the project:

```
service/media/
├── MediaServiceApplication.java  # Main application class
├── controllers/
│   └── MediaController.java      # REST endpoints
├── services/
│   └── MediaService.java         # Business logic
├── models/
│   └── Media.java                # MongoDB entity
├── mongo_repo/
│   └── MediaRepository.java      # MongoDB repository
└── dtos/
    └── MediaDtos.java            # Data transfer objects
```

## Storage

- **File storage**: Files are stored in the `./uploads` directory (configurable)
- **Metadata storage**: File metadata is stored in MongoDB collection `media`
- Each file is given a unique filename (UUID) to prevent conflicts

## Response Examples

**Upload Response:**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "message": "File uploaded successfully",
  "downloadUrl": "/api/media/507f1f77bcf86cd799439011"
}
```

**Media List Response:**
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "filename": "my-picture.jpg",
    "contentType": "image/jpeg",
    "fileSize": 245678,
    "uploaderId": "user123",
    "uploadDate": "2025-11-13T10:30:00",
    "downloadUrl": "/api/media/507f1f77bcf86cd799439011"
  }
]
```

## Troubleshooting

**Service won't start:**
- Make sure MongoDB is running
- Check if port 8083 is already in use
- Verify Java 17+ is installed: `java -version`

**Cannot upload files:**
- Check file size (max 10MB by default)
- Ensure the `uploads` directory is writable
- Verify MongoDB is accessible

**Cannot see uploaded files:**
- Check MongoDB connection
- Verify files exist in the `uploads` directory
- Check application logs for errors

