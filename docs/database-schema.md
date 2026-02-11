# Search Service - Database Schema

## Overview

This document describes the database schema for the Search Service, focusing on JPA entity mappings and service-specific implementation details.

**Authoritative SQL Schema:** See [05-DataArchitecture.md](../../docs/05-DataArchitecture.md) for complete SQL definitions, including table creation statements, constraints, and indexes.

### Tables Owned by Search Service

| Table | Temporal | Description |
|-------|----------|-------------|
| `search.search_videos` | No | Denormalized search index for videos |

The service uses Spring Data JPA with native queries for PostgreSQL full-text search.

---

## JPA Entity Mappings

All entities use Lombok `@Getter` and `@Setter` annotations to reduce boilerplate.

### SearchVideo Entity

```java
@Entity
@Table(name = "search_videos", schema = "search")
@Getter
@Setter
@NoArgsConstructor
public class SearchVideo {

    @Id
    private UUID id;  // Same as video ID from video-service

    @Column(name = "youtube_id", nullable = false, length = 11)
    private String youtubeId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "channel_id", length = 50)
    private String channelId;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "video_date")
    private LocalDate videoDate;

    @Column(name = "amendments", columnDefinition = "VARCHAR(20)[]")
    private String[] amendments;

    @Column(name = "participants", columnDefinition = "VARCHAR(20)[]")
    private String[] participants;

    // Denormalized primary location fields
    @Column(name = "primary_location_id")
    private UUID primaryLocationId;

    @Column(name = "primary_location_name")
    private String primaryLocationName;

    @Column(name = "primary_location_city")
    private String primaryLocationCity;

    @Column(name = "primary_location_state")
    private String primaryLocationState;

    @Column(name = "primary_location_lat")
    private Double primaryLocationLat;

    @Column(name = "primary_location_lng")
    private Double primaryLocationLng;

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;

    // PostgreSQL tsvector - read-only, populated by trigger
    @Column(name = "search_vector", insertable = false, updatable = false)
    private String searchVector;
}
```

**Notes:**
- `id` matches the video ID from video-service (not auto-generated)
- `amendments` and `participants` use PostgreSQL arrays for efficient filtering with GIN indexes
- `searchVector` is read-only; a custom PostgreSQL trigger maintains it automatically
- Location fields are denormalized from the video's primary location for fast filtering

---

## Temporal vs Non-Temporal Decisions

| Table | Temporal | Rationale |
|-------|----------|-----------|
| `search_videos` | No | This is a derived search index, not a source of truth. The video-service owns the canonical data with full history. Rebuilding the index from video-service events is the recovery strategy. |

**Storage implications:** The search index is optimized for read performance, not audit trails. If data inconsistencies occur, the index can be rebuilt by replaying moderation events from the video-service.

---

## Full-Text Search Implementation

### Search Vector Trigger

The `search_vector` column is a `tsvector` maintained by a trigger with weighted ranking:

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

CREATE TRIGGER search_videos_vector_trigger
    BEFORE INSERT OR UPDATE OF title, channel_name, description ON search.search_videos
    FOR EACH ROW EXECUTE FUNCTION search.update_search_vector();
```

**Weight hierarchy:**
- **A (highest):** Title matches - most relevant
- **B (medium):** Channel name matches - useful for finding content from specific creators
- **C (lowest):** Description matches - broad context

### Ranking Algorithm

Queries use `ts_rank_cd()` (cover density ranking) which considers:
- Lexeme proximity (words closer together rank higher)
- Weight hierarchy (A > B > C)
- Document length normalization

---

## Index Strategy

| Index | Column(s) | Type | Purpose |
|-------|-----------|------|---------|
| `idx_search_videos_youtube_id` | `youtube_id` | B-tree | Duplicate detection, direct lookup |
| `idx_search_videos_channel_id` | `channel_id` | B-tree | Channel-based queries |
| `idx_search_videos_video_date` | `video_date` | B-tree | Date range filtering, sorting |
| `idx_search_videos_amendments` | `amendments` | GIN | Array overlap queries (`&&` operator) |
| `idx_search_videos_participants` | `participants` | GIN | Array overlap queries (`&&` operator) |
| `idx_search_videos_state` | `primary_location_state` | B-tree | State-based filtering |
| `idx_search_videos_search_vector` | `search_vector` | GIN | Full-text search (`@@` operator) |

**GIN indexes** are essential for:
- Full-text search on `tsvector` columns
- PostgreSQL array containment/overlap operators

**Guidance:** The current index set covers all query patterns. Don't add indexes speculatively; measure query performance first.

---

## Common Query Patterns

### Full-text search with filters

```java
@Query(value = """
    SELECT v.*, ts_rank_cd(v.search_vector, plainto_tsquery('english', :query)) AS rank
    FROM search.search_videos v
    WHERE (:query IS NULL OR :query = '' OR v.search_vector @@ plainto_tsquery('english', :query))
      AND (:amendments IS NULL OR v.amendments && CAST(:amendments AS VARCHAR[]))
      AND (:participants IS NULL OR v.participants && CAST(:participants AS VARCHAR[]))
      AND (:state IS NULL OR v.primary_location_state = :state)
    ORDER BY CASE WHEN :query IS NULL OR :query = '' THEN 0
             ELSE ts_rank_cd(v.search_vector, plainto_tsquery('english', :query)) END DESC,
             v.indexed_at DESC
    """, nativeQuery = true)
Page<SearchVideo> searchWithFilters(String query, String amendments, String participants,
                                     String state, Pageable pageable);
```

**Query behavior:**
- When `query` is null/empty: returns all videos, sorted by `indexed_at` (newest first)
- When filters are null: that filter is skipped (not "match nothing")
- Array filters use `&&` (overlap): matches if ANY filter value is in the array
- Results are ranked by FTS relevance, then by recency

### Array filter format

Amendments and participants are passed as PostgreSQL array literals:

```java
// Service layer converts Set<String> to PostgreSQL array format
String amendments = "{FIRST,FOURTH}";  // Matches videos with FIRST OR FOURTH amendment
```

---

## Migration Notes

- **Flyway naming:** `V{version}__{description}.sql` (e.g., `V1__create_search_schema.sql`)
- **Trigger updates:** When modifying the search vector trigger, test that existing rows are re-indexed correctly
- **Array columns:** Use `NOT NULL DEFAULT '{}'` to avoid null-handling complexity in queries
- **Testing migrations:** Run `./gradlew bootRun` against local PostgreSQL; Flyway runs automatically on startup
