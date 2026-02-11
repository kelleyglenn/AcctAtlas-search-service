package com.accountabilityatlas.searchservice.service;

import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

  private final SearchVideoRepository searchVideoRepository;

  @Transactional(readOnly = true)
  public SearchResult search(
      String query,
      Set<String> amendments,
      Set<String> participants,
      String state,
      Pageable pageable) {

    long startTime = System.currentTimeMillis();

    String amendmentsArray =
        amendments != null && !amendments.isEmpty() ? toPostgresArray(amendments) : null;
    String participantsArray =
        participants != null && !participants.isEmpty() ? toPostgresArray(participants) : null;
    String searchQuery = query != null && !query.isBlank() ? query.trim() : null;

    Page<SearchVideo> page =
        searchVideoRepository.searchWithFilters(
            searchQuery, amendmentsArray, participantsArray, state, pageable);

    long queryTime = System.currentTimeMillis() - startTime;

    return new SearchResult(
        page.getContent(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize(),
        queryTime);
  }

  private String toPostgresArray(Set<String> values) {
    return "{" + String.join(",", values) + "}";
  }
}
