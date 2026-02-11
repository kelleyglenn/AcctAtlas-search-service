package com.accountabilityatlas.searchservice.client;

import java.util.UUID;

/**
 * Thrown when video-service is unavailable or returns an unexpected error. This is retryable - the
 * message should be sent to DLQ for later processing.
 */
public class VideoServiceException extends RuntimeException {

  private final UUID videoId;
  private final boolean retryable;

  public VideoServiceException(UUID videoId, String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.videoId = videoId;
    this.retryable = retryable;
  }

  public VideoServiceException(UUID videoId, String message, boolean retryable) {
    super(message);
    this.videoId = videoId;
    this.retryable = retryable;
  }

  public UUID getVideoId() {
    return videoId;
  }

  public boolean isRetryable() {
    return retryable;
  }
}
