package com.accountabilityatlas.searchservice.client;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VideoDetail(
    UUID id,
    String youtubeId,
    String title,
    String description,
    String thumbnailUrl,
    Integer durationSeconds,
    String channelId,
    String channelName,
    LocalDate videoDate,
    List<String> amendments,
    List<String> participants,
    String status,
    OffsetDateTime createdAt,
    List<VideoLocationDetail> locations) {

  public record VideoLocationDetail(
      UUID id, UUID locationId, boolean isPrimary, LocationSummary location) {}

  public record LocationSummary(
      UUID id, String displayName, String city, String state, Coordinates coordinates) {}

  public record Coordinates(double latitude, double longitude) {}
}
