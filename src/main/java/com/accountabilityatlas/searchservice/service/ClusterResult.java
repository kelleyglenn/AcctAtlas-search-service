package com.accountabilityatlas.searchservice.service;

import java.util.List;

public record ClusterResult(List<Cluster> clusters, long totalLocations, int zoom) {

  public record Cluster(
      String id,
      double latitude,
      double longitude,
      long count,
      double minLat,
      double maxLat,
      double minLng,
      double maxLng) {}
}
