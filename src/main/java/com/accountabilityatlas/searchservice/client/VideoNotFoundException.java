package com.accountabilityatlas.searchservice.client;

import lombok.Getter;

import java.util.UUID;

/** Thrown when a video is not found in video-service (404). This is not retryable. */
@Getter
public class VideoNotFoundException extends RuntimeException {

  private final UUID videoId;

  public VideoNotFoundException(UUID videoId) {
    super("Video not found: " + videoId);
    this.videoId = videoId;
  }

}
