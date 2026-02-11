package com.accountabilityatlas.searchservice.client;

import java.util.UUID;
import lombok.Getter;

/** Thrown when a video is not found in video-service (404). This is not retryable. */
@Getter
public class VideoNotFoundException extends RuntimeException {

  private final UUID videoId;

  public VideoNotFoundException(UUID videoId) {
    super("Video not found: " + videoId);
    this.videoId = videoId;
  }
}
