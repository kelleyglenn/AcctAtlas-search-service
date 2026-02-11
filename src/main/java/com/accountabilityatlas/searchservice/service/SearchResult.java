package com.accountabilityatlas.searchservice.service;

import com.accountabilityatlas.searchservice.domain.SearchVideo;
import java.util.List;

public record SearchResult(
    List<SearchVideo> videos,
    long totalElements,
    int totalPages,
    int page,
    int size,
    long queryTimeMs) {}
