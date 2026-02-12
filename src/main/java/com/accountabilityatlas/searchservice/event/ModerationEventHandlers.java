package com.accountabilityatlas.searchservice.event;

import com.accountabilityatlas.searchservice.service.IndexingService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SQS listener for moderation events.
 *
 * <p>Handles VideoApproved and VideoRejected events from the moderation-events SQS queue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationEventHandlers {

  private final IndexingService indexingService;

  /**
   * Handles moderation events by routing to the appropriate handler based on event type.
   *
   * @param event the moderation event (VideoApproved or VideoRejected)
   */
  @SqsListener("${app.sqs.moderation-events-queue:moderation-events}")
  public void handleModerationEvent(ModerationEvent event) {
    switch (event) {
      case VideoApprovedEvent approved -> handleVideoApproved(approved);
      case VideoRejectedEvent rejected -> handleVideoRejected(rejected);
    }
  }

  private void handleVideoApproved(VideoApprovedEvent event) {
    log.info("Received VideoApproved event for video {}", event.videoId());
    try {
      indexingService.indexVideo(event.videoId());
    } catch (Exception e) {
      log.error("Failed to index video {}: {}", event.videoId(), e.getMessage());
      throw e; // Re-throw to trigger retry/DLQ
    }
  }

  private void handleVideoRejected(VideoRejectedEvent event) {
    log.info("Received VideoRejected event for video {}", event.videoId());
    try {
      indexingService.removeVideo(event.videoId());
    } catch (Exception e) {
      log.error("Failed to remove video {}: {}", event.videoId(), e.getMessage());
      throw e;
    }
  }
}
