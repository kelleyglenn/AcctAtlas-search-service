# Search Service - Technical Documentation

## Service Overview

The Search Service provides full-text and faceted search capabilities across all video content in AccountabilityAtlas. It maintains a search index synchronized with the video database and provides fast, relevance-ranked results.

## Responsibilities

- Index video documents from database events
- Execute full-text search queries
- Provide faceted filtering (amendments, participants, location, date)
- Autocomplete suggestions
- Search result ranking and relevance tuning
- Synonym handling for common terms

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.x |
| Language | Java 21 |
| Build | Gradle |
| Search Engine | OpenSearch 2.x |
| Client | OpenSearch Java Client |

## Dependencies

- **OpenSearch**: Search index storage and querying
- **SQS**: Event consumption (VideoApproved, VideoUpdated, VideoDeleted)

## Documentation Index

| Document | Status | Description |
|----------|--------|-------------|
| [api-specification.yaml](api-specification.yaml) | Planned | OpenAPI 3.0 specification |
| [index-mapping.md](index-mapping.md) | Planned | OpenSearch index configuration |
| [query-patterns.md](query-patterns.md) | Planned | Search query implementations |
| [relevance-tuning.md](relevance-tuning.md) | Planned | Ranking and scoring rules |

## Search Index Schema

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "youtubeId": { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "video_analyzer",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "description": { "type": "text", "analyzer": "video_analyzer" },
      "amendments": { "type": "keyword" },
      "participants": { "type": "keyword" },
      "videoDate": { "type": "date" },
      "submitterName": { "type": "keyword" },
      "locations": {
        "type": "nested",
        "properties": {
          "id": { "type": "keyword" },
          "coordinates": { "type": "geo_point" },
          "city": { "type": "keyword" },
          "state": { "type": "keyword" }
        }
      },
      "createdAt": { "type": "date" },
      "suggest": { "type": "completion" }
    }
  }
}
```

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /search | Public | Execute search query |
| GET | /search/suggest | Public | Autocomplete suggestions |
| GET | /search/facets | Public | Get available facet values |

## Query Parameters (GET /search)

| Parameter | Type | Description |
|-----------|------|-------------|
| q | String | Search query text |
| amendments | String[] | Filter by amendments |
| participants | String[] | Filter by participants |
| dateFrom | Date | Filter videos after date |
| dateTo | Date | Filter videos before date |
| state | String | Filter by state |
| bbox | String | Geo filter: minLng,minLat,maxLng,maxLat |
| sort | String | Sort by: relevance, date, distance |
| page | Int | Page number (0-indexed) |
| size | Int | Page size (default: 20, max: 100) |

## Search Response

```java
public record SearchResponse(
    List<VideoSearchResult> results,
    SearchFacets facets,
    Pagination pagination,
    Duration queryTime
) { }

public record VideoSearchResult(
    UUID id,
    String youtubeId,
    String title,
    String description,
    String thumbnailUrl,
    Set<Amendment> amendments,
    Set<Participant> participants,
    List<LocationSummary> locations,
    float score,
    Map<String, List<String>> highlights
) { }

public record SearchFacets(
    Map<Amendment, Integer> amendments,
    Map<Participant, Integer> participants,
    Map<String, Integer> states,
    Map<Integer, Integer> years
) { }
```

## Synonym Configuration

```yaml
synonyms:
  - "cop, police, officer, law enforcement, LEO"
  - "1a, first amendment, free speech, freedom of speech"
  - "2a, second amendment, gun rights, right to bear arms"
  - "4a, fourth amendment, search and seizure, unreasonable search"
  - "5a, fifth amendment, self incrimination, right to remain silent"
  - "audit, auditor, auditing"
  - "tyrant, tyranny, abuse of power"
```

## Events Consumed

| Event | Action |
|-------|--------|
| VideoApproved | Index new document |
| VideoUpdated | Update existing document |
| VideoDeleted | Remove document from index |

## Reindexing

Full reindex capability for:
- Schema changes
- Analyzer updates
- Disaster recovery

```bash
# Trigger reindex via admin endpoint
POST /admin/reindex
Authorization: Bearer <admin_token>

# Returns job ID for tracking
{ "jobId": "uuid", "status": "IN_PROGRESS", "documentCount": 0 }
```

## Local Development

```bash
# Start dependencies
docker-compose up -d opensearch

# Wait for OpenSearch to be ready
curl -s http://localhost:9200/_cluster/health | jq .status

# Run service
./gradlew bootRun

# Service available at http://localhost:8084
```
