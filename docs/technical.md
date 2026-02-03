# Search Service - Technical Documentation

## Service Overview

The Search Service provides full-text and faceted search capabilities across all video content in AccountabilityAtlas. It uses a pluggable search backend — PostgreSQL FTS in Phases 1-2, OpenSearch in Phase 3+ — and provides fast, relevance-ranked results.

## Responsibilities

- Execute full-text search queries with weighted ranking (title > channel > description)
- Provide faceted filtering (amendments, participants, location, date)
- Autocomplete suggestions (Phase 1-2: prefix matching via SQL `LIKE`; Phase 3+: OpenSearch completion suggester)
- Search result ranking and relevance tuning
- Synonym handling for common terms (e.g., "1A" → "First Amendment", "cop" → "police")
- Index video documents from SQS events (Phase 3+: OpenSearch indexing; Phase 1-2: handled by PostgreSQL trigger)

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.x |
| Language | Java 21 |
| Build | Gradle |
| Search Engine | PostgreSQL FTS (Phase 1-2), OpenSearch 2.x (Phase 3+) |
| Client | Spring Data JPA (Phase 1-2), OpenSearch Java Client (Phase 3+) |

## Architecture

The search backend is abstracted behind a `SearchRepository` interface:

```
SearchRepository (interface)
├── PostgresSearchRepository  — active in Phase 1-2 (Spring profile: "fts")
└── OpenSearchRepository      — active in Phase 3+  (Spring profile: "opensearch")
```

The active implementation is selected via Spring profile (`spring.profiles.active=fts` or `spring.profiles.active=opensearch`). Both implementations expose the same search, suggest, and facet operations through the API endpoints below.

## Dependencies

- **PostgreSQL** (Phase 1-2): Search queries via `tsvector`/`tsquery` against the `content.videos` table
- **OpenSearch** (Phase 3+): Dedicated search index storage and querying
- **SQS**: Event consumption (VideoApproved, VideoUpdated, VideoDeleted)

## Documentation Index

| Document | Status | Description |
|----------|--------|-------------|
| [api-specification.yaml](api-specification.yaml) | Complete | OpenAPI 3.1 specification |
| [index-mapping.md](index-mapping.md) | Planned | OpenSearch index configuration (Phase 3+) |
| [query-patterns.md](query-patterns.md) | Planned | Search query implementations (both backends) |
| [relevance-tuning.md](relevance-tuning.md) | Planned | Ranking and scoring rules |

## Search Index Schema

### Phase 1-2: PostgreSQL FTS

Search is powered by a `search_vector tsvector` column on `content.videos` with a GIN index. Weighted ranking: title (A), channel_name (B), description (C). A custom `acct_atlas` text search configuration provides domain-specific synonyms.

See [05-DataArchitecture.md](../../../docs/05-DataArchitecture.md#full-text-search-configuration-phase-1-2) for full DDL, trigger definition, and example queries.

### Phase 3+: OpenSearch Index

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
      "channelId": { "type": "keyword" },
      "channelName": {
        "type": "text",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "publishedAt": { "type": "date" },
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

| Event | Phase 1-2 Action | Phase 3+ Action |
|-------|------------------|-----------------|
| VideoApproved | No action (tsvector trigger handles indexing) | Index new document in OpenSearch |
| VideoUpdated | No action (tsvector trigger handles updates) | Update existing document in OpenSearch |
| VideoDeleted | No action (CASCADE delete removes row) | Remove document from OpenSearch |

## Reindexing

### Phase 1-2 (PostgreSQL FTS)

Reindex by regenerating the `search_vector` column:

```sql
UPDATE content.videos SET search_vector =
    setweight(to_tsvector('acct_atlas', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('acct_atlas', COALESCE(channel_name, '')), 'B') ||
    setweight(to_tsvector('acct_atlas', COALESCE(description, '')), 'C');
```

### Phase 3+ (OpenSearch)

Full reindex capability for schema changes, analyzer updates, and disaster recovery:

```bash
# Trigger reindex via admin endpoint
POST /admin/reindex
Authorization: Bearer <admin_token>

# Returns job ID for tracking
{ "jobId": "uuid", "status": "IN_PROGRESS", "documentCount": 0 }
```

## Local Development

```bash
# Phase 1-2: Start with PostgreSQL FTS (default)
docker-compose up -d postgres
./gradlew bootRun --args='--spring.profiles.active=local,fts'

# Phase 3+: Start with OpenSearch
docker-compose up -d postgres opensearch
# Wait for OpenSearch to be ready
curl -s http://localhost:9200/_cluster/health | jq .status
./gradlew bootRun --args='--spring.profiles.active=local,opensearch'

# Service available at http://localhost:8084
```
