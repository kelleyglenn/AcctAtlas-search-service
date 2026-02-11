# Search Service - Technical Documentation

## Service Overview

The Search Service provides full-text and faceted search capabilities across all video content in AccountabilityAtlas. It uses PostgreSQL FTS in Phase 1-2 with a denormalized `search_videos` table and provides fast, relevance-ranked results.

## Responsibilities

- Execute full-text search queries with weighted ranking (title > channel > description)
- Provide faceted filtering (amendments, participants, state)
- Index approved videos from moderation-events queue
- Search result ranking and relevance tuning

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.4.x |
| Language | Java 21 |
| Build | Gradle 9.x |
| Database | PostgreSQL 15 (FTS) |
| Messaging | Spring Cloud Stream with SQS |
| API Client | WebClient (for video-service) |

## Architecture

```
moderation-service                     search-service                    video-service
       │                                     │                                │
       │  VideoApproved                      │                                │
       ├────────────────────────────────────►│                                │
       │  (moderation-events queue)          │  GET /videos/{id}              │
       │                                     ├───────────────────────────────►│
       │  Consumer<VideoApprovedEvent>       │◄───────────────────────────────┤
       │                                     │  Index video                   │
       │                                     │                                │
       │  VideoRejected                      │                                │
       ├────────────────────────────────────►│                                │
       │  (moderation-events queue)          │  Remove from index             │
       │                                     │                                │
```

## Domain Model

### SearchVideo Entity

Denormalized video data optimized for search:

```java
@Entity
@Table(name = "search_videos", schema = "search")
public class SearchVideo {
  @Id private UUID id;
  private String youtubeId;
  private String title;
  private String description;
  private String thumbnailUrl;
  private Integer durationSeconds;
  private String channelId;
  private String channelName;
  private LocalDate videoDate;
  private String[] amendments;
  private String[] participants;
  private UUID primaryLocationId;
  private String primaryLocationName;
  private String primaryLocationCity;
  private String primaryLocationState;
  private Double primaryLocationLat;
  private Double primaryLocationLng;
  private Instant indexedAt;
  private String searchVector; // tsvector, managed by trigger
}
```

## Database Schema

The service uses a dedicated `search` schema with a denormalized `search_videos` table:

```sql
CREATE TABLE search.search_videos (
    id UUID PRIMARY KEY,
    youtube_id VARCHAR(11) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    duration_seconds INTEGER,
    channel_id VARCHAR(50),
    channel_name VARCHAR(255),
    video_date DATE,
    amendments VARCHAR(20)[] NOT NULL DEFAULT '{}',
    participants VARCHAR(20)[] NOT NULL DEFAULT '{}',
    primary_location_id UUID,
    primary_location_name VARCHAR(200),
    primary_location_city VARCHAR(100),
    primary_location_state VARCHAR(50),
    primary_location_lat DOUBLE PRECISION,
    primary_location_lng DOUBLE PRECISION,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    search_vector TSVECTOR
);
```

### Trigger for search_vector

```sql
CREATE OR REPLACE FUNCTION search.update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.channel_name, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### Indexes

- GIN index on `search_vector` for full-text search
- GIN indexes on `amendments` and `participants` arrays for filtering
- B-tree indexes on `youtube_id`, `channel_id`, `video_date`, `primary_location_state`

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /search | Public | Execute search query with filters |

### Query Parameters (GET /search)

| Parameter | Type | Description |
|-----------|------|-------------|
| q | String | Search query text |
| amendments | String[] | Filter by amendments (e.g., FIRST, FOURTH) |
| participants | String[] | Filter by participants (e.g., POLICE, CITIZEN) |
| state | String | Filter by US state |
| page | Int | Page number (0-indexed) |
| size | Int | Page size (default: 20, max: 100) |

### Search Response

```json
{
  "results": [
    {
      "id": "uuid",
      "youtubeId": "dQw4w9WgXcQ",
      "title": "Video Title",
      "description": "Description...",
      "thumbnailUrl": "https://...",
      "durationSeconds": 120,
      "channelId": "UC...",
      "channelName": "Channel Name",
      "videoDate": "2024-01-15",
      "amendments": ["FIRST", "FOURTH"],
      "participants": ["POLICE", "CITIZEN"],
      "locations": [
        {
          "id": "uuid",
          "displayName": "Location Name",
          "city": "City",
          "state": "CA",
          "coordinates": { "latitude": 34.05, "longitude": -118.25 }
        }
      ]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  },
  "queryTime": 15,
  "query": "first amendment"
}
```

## Events Consumed

| Event | Source | Action |
|-------|--------|--------|
| VideoApproved | moderation-events queue | Fetch video from video-service, index in search_videos |
| VideoRejected | moderation-events queue | Remove video from search_videos |

### Event Handlers

```java
@Configuration
public class ModerationEventHandlers {

  @Bean
  public Consumer<VideoApprovedEvent> handleVideoApproved() {
    return event -> indexingService.indexVideo(event.videoId());
  }

  @Bean
  public Consumer<VideoRejectedEvent> handleVideoRejected() {
    return event -> indexingService.removeVideo(event.videoId());
  }
}
```

## Spring Cloud Stream Configuration

```yaml
spring:
  cloud:
    stream:
      bindings:
        handleVideoApproved-in-0:
          destination: moderation-events
          group: search-service
        handleVideoRejected-in-0:
          destination: moderation-events
          group: search-service
      function:
        definition: handleVideoApproved;handleVideoRejected
```

## Dependencies

- **PostgreSQL**: Search index storage via `search.search_videos` table
- **SQS**: Event consumption from `moderation-events` queue
- **video-service**: HTTP client for fetching full video details on indexing

## Documentation Index

| Document | Status | Description |
|----------|--------|-------------|
| [api-specification.yaml](api-specification.yaml) | Complete | OpenAPI 3.1 specification |
| [../../../docs/05-DataArchitecture.md](../../../docs/05-DataArchitecture.md) | Reference | Full-text search configuration |

## Local Development

```bash
# Start dependencies
docker-compose up -d postgres localstack

# Run service
./gradlew bootRun

# Service available at http://localhost:8084
```

### Port Assignments

| Service | Port |
|---------|------|
| search-service | 8084 |
| PostgreSQL | 5436 (local docker-compose) |
| LocalStack SQS | 4566 |

## Project Structure

```
src/main/java/com/accountabilityatlas/searchservice/
├── SearchServiceApplication.java
├── client/
│   ├── VideoDetail.java          # DTO for video-service response
│   └── VideoServiceClient.java   # WebClient for video-service
├── config/
│   └── SecurityConfig.java       # All search endpoints public
├── domain/
│   ├── Amendment.java
│   ├── Participant.java
│   └── SearchVideo.java          # JPA entity
├── event/
│   ├── ModerationEventHandlers.java  # Spring Cloud Stream consumers
│   ├── VideoApprovedEvent.java
│   └── VideoRejectedEvent.java
├── repository/
│   └── SearchVideoRepository.java    # JPA + native FTS queries
├── service/
│   ├── IndexingService.java      # Index/remove videos
│   ├── SearchResult.java
│   └── SearchService.java        # Search with filters
└── web/
    └── SearchController.java     # REST endpoint
```
