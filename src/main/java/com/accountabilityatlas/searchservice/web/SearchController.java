package com.accountabilityatlas.searchservice.web;

import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.service.SearchResult;
import com.accountabilityatlas.searchservice.service.SearchService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

  private final SearchService searchService;

  @GetMapping
  public ResponseEntity<?> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Set<String> amendments,
      @RequestParam(required = false) Set<String> participants,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String bbox,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    if (size > 100) {
      size = 100;
    }
    Pageable pageable = PageRequest.of(page, size);

    Double minLng = null;
    Double minLat = null;
    Double maxLng = null;
    Double maxLat = null;

    if (bbox != null && !bbox.isBlank()) {
      String[] parts = bbox.split(",");
      if (parts.length != 4) {
        return ResponseEntity.badRequest()
            .body("Invalid bbox format. Expected: minLng,minLat,maxLng,maxLat");
      }
      try {
        minLng = Double.parseDouble(parts[0]);
        minLat = Double.parseDouble(parts[1]);
        maxLng = Double.parseDouble(parts[2]);
        maxLat = Double.parseDouble(parts[3]);
      } catch (NumberFormatException e) {
        return ResponseEntity.badRequest()
            .body("Invalid bbox format. Expected: minLng,minLat,maxLng,maxLat");
      }
    }

    SearchResult result =
        searchService.search(
            q, amendments, participants, state, minLng, minLat, maxLng, maxLat, pageable);

    SearchResponse response =
        new SearchResponse(
            result.videos().stream().map(this::toVideoResult).toList(),
            new Pagination(
                result.page(), result.size(), result.totalElements(), result.totalPages()),
            result.queryTimeMs(),
            q);

    return ResponseEntity.ok(response);
  }

  private VideoSearchResult toVideoResult(SearchVideo video) {
    LocationSummary location = null;
    if (video.getPrimaryLocationId() != null) {
      location =
          new LocationSummary(
              video.getPrimaryLocationId(),
              video.getPrimaryLocationName(),
              video.getPrimaryLocationCity(),
              video.getPrimaryLocationState(),
              video.getPrimaryLocationLat() != null && video.getPrimaryLocationLng() != null
                  ? new Coordinates(video.getPrimaryLocationLat(), video.getPrimaryLocationLng())
                  : null);
    }

    return new VideoSearchResult(
        video.getId(),
        video.getYoutubeId(),
        video.getTitle(),
        video.getDescription(),
        video.getThumbnailUrl(),
        video.getDurationSeconds(),
        video.getChannelId(),
        video.getChannelName(),
        video.getVideoDate(),
        video.getAmendments() != null ? Set.of(video.getAmendments()) : Set.of(),
        video.getParticipants() != null ? Set.of(video.getParticipants()) : Set.of(),
        location != null ? List.of(location) : List.of());
  }

  // Response DTOs
  public record SearchResponse(
      List<VideoSearchResult> results, Pagination pagination, long queryTime, String query) {}

  public record VideoSearchResult(
      UUID id,
      String youtubeId,
      String title,
      String description,
      String thumbnailUrl,
      Integer durationSeconds,
      String channelId,
      String channelName,
      LocalDate videoDate,
      Set<String> amendments,
      Set<String> participants,
      List<LocationSummary> locations) {}

  public record LocationSummary(
      UUID id, String displayName, String city, String state, Coordinates coordinates) {}

  public record Coordinates(double latitude, double longitude) {}

  public record Pagination(int page, int size, long totalElements, int totalPages) {}
}
