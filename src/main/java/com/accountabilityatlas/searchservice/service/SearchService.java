package com.accountabilityatlas.searchservice.service;

import com.accountabilityatlas.searchservice.domain.Amendment;
import com.accountabilityatlas.searchservice.domain.Participant;
import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

  private static final Set<String> VALID_AMENDMENTS =
      Arrays.stream(Amendment.values()).map(Enum::name).collect(Collectors.toSet());

  private static final Set<String> VALID_PARTICIPANTS =
      Arrays.stream(Participant.values()).map(Enum::name).collect(Collectors.toSet());

  private final SearchVideoRepository searchVideoRepository;

  @Transactional(readOnly = true)
  public SearchResult search(
      String query,
      Set<String> amendments,
      Set<String> participants,
      String state,
      Double minLng,
      Double minLat,
      Double maxLng,
      Double maxLat,
      Pageable pageable) {

    long startTime = System.currentTimeMillis();

    String amendmentsArray = toValidatedPostgresArray(amendments, VALID_AMENDMENTS);
    String participantsArray = toValidatedPostgresArray(participants, VALID_PARTICIPANTS);
    String searchQuery = query != null && !query.isBlank() ? query.trim() : null;

    Page<SearchVideo> page =
        searchVideoRepository.searchWithFilters(
            searchQuery,
            amendmentsArray,
            participantsArray,
            state,
            minLat,
            maxLat,
            minLng,
            maxLng,
            pageable);

    long queryTime = System.currentTimeMillis() - startTime;

    return new SearchResult(
        page.getContent(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize(),
        queryTime);
  }

  /**
   * Converts a set of values to a PostgreSQL array string, filtering to only valid enum values.
   * This prevents SQL injection by ensuring only known-safe values are included.
   */
  private String toValidatedPostgresArray(Set<String> values, Set<String> validValues) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    Set<String> validated =
        values.stream().filter(validValues::contains).collect(Collectors.toSet());
    if (validated.isEmpty()) {
      return null;
    }
    return "{" + String.join(",", validated) + "}";
  }
}
