package com.accountabilityatlas.searchservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "search_videos", schema = "search")
@Getter
@Setter
@NoArgsConstructor
public class SearchVideo {

  @Id private UUID id;

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

  @Column(
      name = "search_vector",
      insertable = false,
      updatable = false,
      columnDefinition = "tsvector")
  private String searchVector;
}
