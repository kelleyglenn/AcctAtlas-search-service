package com.accountabilityatlas.searchservice.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.accountabilityatlas.searchservice.service.IndexingService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModerationEventHandlersTest {

  @Mock private IndexingService indexingService;
  @InjectMocks private ModerationEventHandlers handlers;

  @Test
  void handleModerationEvent_videoApproved_callsIndexVideo() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoApprovedEvent event = new VideoApprovedEvent(videoId, UUID.randomUUID(), Instant.now());

    // Act
    handlers.handleModerationEvent(event);

    // Assert
    verify(indexingService).indexVideo(videoId);
  }

  @Test
  void handleModerationEvent_videoRejected_callsRemoveVideo() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoRejectedEvent event =
        new VideoRejectedEvent(videoId, UUID.randomUUID(), "OFF_TOPIC", Instant.now());

    // Act
    handlers.handleModerationEvent(event);

    // Assert
    verify(indexingService).removeVideo(videoId);
  }

  @Test
  void handleModerationEvent_videoApproved_indexingFailure_rethrowsException() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoApprovedEvent event = new VideoApprovedEvent(videoId, UUID.randomUUID(), Instant.now());

    RuntimeException indexingException = new RuntimeException("OpenSearch unavailable");
    doThrow(indexingException).when(indexingService).indexVideo(videoId);

    // Act & Assert
    assertThatThrownBy(() -> handlers.handleModerationEvent(event)).isSameAs(indexingException);
  }

  @Test
  void handleModerationEvent_videoRejected_removalFailure_rethrowsException() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoRejectedEvent event =
        new VideoRejectedEvent(videoId, UUID.randomUUID(), "OFF_TOPIC", Instant.now());

    RuntimeException removalException = new RuntimeException("OpenSearch unavailable");
    doThrow(removalException).when(indexingService).removeVideo(videoId);

    // Act & Assert
    assertThatThrownBy(() -> handlers.handleModerationEvent(event)).isSameAs(removalException);
  }
}
