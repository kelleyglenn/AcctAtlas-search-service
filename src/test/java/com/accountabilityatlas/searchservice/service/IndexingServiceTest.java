package com.accountabilityatlas.searchservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.accountabilityatlas.searchservice.client.VideoDetail;
import com.accountabilityatlas.searchservice.client.VideoNotFoundException;
import com.accountabilityatlas.searchservice.client.VideoServiceClient;
import com.accountabilityatlas.searchservice.client.VideoServiceException;
import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexingServiceTest {

  @Mock private SearchVideoRepository searchVideoRepository;
  @Mock private VideoServiceClient videoServiceClient;
  @InjectMocks private IndexingService indexingService;
  @Captor private ArgumentCaptor<SearchVideo> searchVideoCaptor;

  private UUID videoId;
  private VideoDetail approvedVideo;

  @BeforeEach
  void setUp() {
    videoId = UUID.randomUUID();
    approvedVideo = createVideoDetail(videoId, "APPROVED");
  }

  @Test
  void indexVideo_whenVideoNotFound_skipsIndexing() {
    when(videoServiceClient.getVideo(videoId)).thenThrow(new VideoNotFoundException(videoId));

    // Should not throw - VideoNotFoundException is handled gracefully
    indexingService.indexVideo(videoId);

    verify(searchVideoRepository, never()).save(any());
  }

  @Test
  void indexVideo_whenVideoServiceUnavailable_propagatesException() {
    VideoServiceException serviceException =
        new VideoServiceException(videoId, "Service unavailable", true);
    when(videoServiceClient.getVideo(videoId)).thenThrow(serviceException);

    // Should propagate to trigger retry/DLQ
    assertThatThrownBy(() -> indexingService.indexVideo(videoId))
        .isInstanceOf(VideoServiceException.class)
        .satisfies(
            ex -> {
              VideoServiceException vse = (VideoServiceException) ex;
              assertThat(vse.isRetryable()).isTrue();
            });

    verify(searchVideoRepository, never()).save(any());
  }

  @Test
  void indexVideo_whenVideoNotApproved_skipsIndexing() {
    VideoDetail pendingVideo = createVideoDetail(videoId, "PENDING");
    when(videoServiceClient.getVideo(videoId)).thenReturn(pendingVideo);

    indexingService.indexVideo(videoId);

    verify(searchVideoRepository, never()).save(any());
  }

  @Test
  void indexVideo_whenApproved_savesNewVideo() {
    when(videoServiceClient.getVideo(videoId)).thenReturn(approvedVideo);
    when(searchVideoRepository.findById(videoId)).thenReturn(Optional.empty());

    indexingService.indexVideo(videoId);

    verify(searchVideoRepository).save(searchVideoCaptor.capture());
    SearchVideo saved = searchVideoCaptor.getValue();

    assertThat(saved.getId()).isEqualTo(videoId);
    assertThat(saved.getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
    assertThat(saved.getTitle()).isEqualTo("Test Video Title");
    assertThat(saved.getDescription()).isEqualTo("Test description");
    assertThat(saved.getChannelName()).isEqualTo("Test Channel");
    assertThat(saved.getAmendments()).containsExactly("FIRST", "FOURTH");
    assertThat(saved.getParticipants()).containsExactly("POLICE", "CITIZEN");
    assertThat(saved.getIndexedAt()).isNotNull();
  }

  @Test
  void indexVideo_whenApproved_updatesExistingVideo() {
    SearchVideo existing = new SearchVideo();
    existing.setId(videoId);
    existing.setTitle("Old Title");

    when(videoServiceClient.getVideo(videoId)).thenReturn(approvedVideo);
    when(searchVideoRepository.findById(videoId)).thenReturn(Optional.of(existing));

    indexingService.indexVideo(videoId);

    verify(searchVideoRepository).save(searchVideoCaptor.capture());
    SearchVideo saved = searchVideoCaptor.getValue();

    assertThat(saved.getId()).isEqualTo(videoId);
    assertThat(saved.getTitle()).isEqualTo("Test Video Title");
  }

  @Test
  void indexVideo_withPrimaryLocation_mapsLocationData() {
    UUID locationId = UUID.randomUUID();
    VideoDetail videoWithLocation = createVideoDetailWithLocation(videoId, locationId);

    when(videoServiceClient.getVideo(videoId)).thenReturn(videoWithLocation);
    when(searchVideoRepository.findById(videoId)).thenReturn(Optional.empty());

    indexingService.indexVideo(videoId);

    verify(searchVideoRepository).save(searchVideoCaptor.capture());
    SearchVideo saved = searchVideoCaptor.getValue();

    assertThat(saved.getPrimaryLocationId()).isEqualTo(locationId);
    assertThat(saved.getPrimaryLocationName()).isEqualTo("City Hall");
    assertThat(saved.getPrimaryLocationCity()).isEqualTo("Austin");
    assertThat(saved.getPrimaryLocationState()).isEqualTo("TX");
    assertThat(saved.getPrimaryLocationLat()).isEqualTo(30.2672);
    assertThat(saved.getPrimaryLocationLng()).isEqualTo(-97.7431);
  }

  @Test
  void indexVideo_withNullAmendments_setsEmptyArray() {
    VideoDetail videoWithNulls =
        new VideoDetail(
            videoId,
            "abc123",
            "Title",
            null,
            null,
            null,
            null,
            null,
            null,
            null, // null amendments
            null, // null participants
            "APPROVED",
            OffsetDateTime.now(ZoneOffset.UTC),
            null);

    when(videoServiceClient.getVideo(videoId)).thenReturn(videoWithNulls);
    when(searchVideoRepository.findById(videoId)).thenReturn(Optional.empty());

    indexingService.indexVideo(videoId);

    verify(searchVideoRepository).save(searchVideoCaptor.capture());
    SearchVideo saved = searchVideoCaptor.getValue();

    assertThat(saved.getAmendments()).isEmpty();
    assertThat(saved.getParticipants()).isEmpty();
  }

  @Test
  void removeVideo_whenExists_deletesVideo() {
    when(searchVideoRepository.existsById(videoId)).thenReturn(true);

    indexingService.removeVideo(videoId);

    verify(searchVideoRepository).deleteById(videoId);
  }

  @Test
  void removeVideo_whenNotExists_doesNothing() {
    when(searchVideoRepository.existsById(videoId)).thenReturn(false);

    indexingService.removeVideo(videoId);

    verify(searchVideoRepository, never()).deleteById(any());
  }

  private VideoDetail createVideoDetail(UUID id, String status) {
    return new VideoDetail(
        id,
        "dQw4w9WgXcQ",
        "Test Video Title",
        "Test description",
        "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
        300,
        "UC123",
        "Test Channel",
        LocalDate.of(2024, 1, 15),
        List.of("FIRST", "FOURTH"),
        List.of("POLICE", "CITIZEN"),
        status,
        OffsetDateTime.now(ZoneOffset.UTC),
        null);
  }

  private VideoDetail createVideoDetailWithLocation(UUID videoId, UUID locationId) {
    VideoDetail.Coordinates coords = new VideoDetail.Coordinates(30.2672, -97.7431);
    VideoDetail.LocationSummary location =
        new VideoDetail.LocationSummary(locationId, "City Hall", "Austin", "TX", coords);
    VideoDetail.VideoLocationDetail videoLocation =
        new VideoDetail.VideoLocationDetail(UUID.randomUUID(), locationId, true, location);

    return new VideoDetail(
        videoId,
        "dQw4w9WgXcQ",
        "Test Video Title",
        "Test description",
        "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
        300,
        "UC123",
        "Test Channel",
        LocalDate.of(2024, 1, 15),
        List.of("FIRST"),
        List.of("POLICE"),
        "APPROVED",
        OffsetDateTime.now(ZoneOffset.UTC),
        List.of(videoLocation));
  }
}
