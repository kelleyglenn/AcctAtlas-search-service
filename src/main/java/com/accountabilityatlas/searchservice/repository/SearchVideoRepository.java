package com.accountabilityatlas.searchservice.repository;

import com.accountabilityatlas.searchservice.domain.SearchVideo;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchVideoRepository extends JpaRepository<SearchVideo, UUID> {

  @Query(
      value =
          """
          SELECT v.*, ts_rank_cd(v.search_vector, plainto_tsquery('english', :query)) AS rank
          FROM search.search_videos v
          WHERE (:query IS NULL OR :query = '' OR v.search_vector @@ plainto_tsquery('english', :query))
            AND (:amendments IS NULL OR v.amendments && CAST(:amendments AS VARCHAR[]))
            AND (:participants IS NULL OR v.participants && CAST(:participants AS VARCHAR[]))
            AND (:state IS NULL OR v.primary_location_state = :state)
            AND (:minLat IS NULL OR (v.primary_location_lat BETWEEN :minLat AND :maxLat
                 AND v.primary_location_lng BETWEEN :minLng AND :maxLng))
          ORDER BY CASE WHEN :query IS NULL OR :query = '' THEN 0 ELSE ts_rank_cd(v.search_vector, plainto_tsquery('english', :query)) END DESC,
                   v.indexed_at DESC
          """,
      countQuery =
          """
          SELECT COUNT(*)
          FROM search.search_videos v
          WHERE (:query IS NULL OR :query = '' OR v.search_vector @@ plainto_tsquery('english', :query))
            AND (:amendments IS NULL OR v.amendments && CAST(:amendments AS VARCHAR[]))
            AND (:participants IS NULL OR v.participants && CAST(:participants AS VARCHAR[]))
            AND (:state IS NULL OR v.primary_location_state = :state)
            AND (:minLat IS NULL OR (v.primary_location_lat BETWEEN :minLat AND :maxLat
                 AND v.primary_location_lng BETWEEN :minLng AND :maxLng))
          """,
      nativeQuery = true)
  Page<SearchVideo> searchWithFilters(
      String query,
      String amendments,
      String participants,
      String state,
      Double minLat,
      Double maxLat,
      Double minLng,
      Double maxLng,
      Pageable pageable);
}
