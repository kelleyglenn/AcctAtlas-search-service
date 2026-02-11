package com.accountabilityatlas.searchservice.service;

import com.accountabilityatlas.searchservice.client.VideoDetail;
import com.accountabilityatlas.searchservice.client.VideoServiceClient;
import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

  private final SearchVideoRepository searchVideoRepository;
  private final VideoServiceClient videoServiceClient;

  @Transactional
  public void indexVideo(UUID videoId) {
    log.info("Indexing video {}", videoId);

    var videoOpt = videoServiceClient.getVideo(videoId);
    if (videoOpt.isEmpty()) {
      log.warn("Video {} not found, skipping indexing", videoId);
      return;
    }

    VideoDetail video = videoOpt.get();
    if (!"APPROVED".equals(video.status())) {
      log.warn("Video {} is not approved (status={}), skipping indexing", videoId, video.status());
      return;
    }

    SearchVideo searchVideo = searchVideoRepository.findById(videoId).orElseGet(SearchVideo::new);

    mapVideoToSearchVideo(video, searchVideo);
    searchVideoRepository.save(searchVideo);

    log.info("Successfully indexed video {}", videoId);
  }

  @Transactional
  public void removeVideo(UUID videoId) {
    if (searchVideoRepository.existsById(videoId)) {
      searchVideoRepository.deleteById(videoId);
      log.info("Removed video {} from index", videoId);
    } else {
      log.debug("Video {} not found in index, nothing to remove", videoId);
    }
  }

  private void mapVideoToSearchVideo(VideoDetail video, SearchVideo searchVideo) {
    searchVideo.setId(video.id());
    searchVideo.setYoutubeId(video.youtubeId());
    searchVideo.setTitle(video.title());
    searchVideo.setDescription(video.description());
    searchVideo.setThumbnailUrl(video.thumbnailUrl());
    searchVideo.setDurationSeconds(video.durationSeconds());
    searchVideo.setChannelId(video.channelId());
    searchVideo.setChannelName(video.channelName());
    searchVideo.setVideoDate(video.videoDate());
    searchVideo.setAmendments(
        video.amendments() != null ? video.amendments().toArray(new String[0]) : new String[0]);
    searchVideo.setParticipants(
        video.participants() != null ? video.participants().toArray(new String[0]) : new String[0]);
    searchVideo.setIndexedAt(Instant.now());

    // Find primary location
    if (video.locations() != null) {
      video.locations().stream()
          .filter(VideoDetail.VideoLocationDetail::isPrimary)
          .findFirst()
          .ifPresent(
              loc -> {
                if (loc.location() != null) {
                  searchVideo.setPrimaryLocationId(loc.location().id());
                  searchVideo.setPrimaryLocationName(loc.location().displayName());
                  searchVideo.setPrimaryLocationCity(loc.location().city());
                  searchVideo.setPrimaryLocationState(loc.location().state());
                  if (loc.location().coordinates() != null) {
                    searchVideo.setPrimaryLocationLat(loc.location().coordinates().latitude());
                    searchVideo.setPrimaryLocationLng(loc.location().coordinates().longitude());
                  }
                }
              });
    }
  }
}
