package com.accountabilityatlas.searchservice.service;

import com.accountabilityatlas.searchservice.domain.Amendment;
import com.accountabilityatlas.searchservice.domain.Participant;
import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClusterService {

  private static final Set<String> VALID_AMENDMENTS =
      Arrays.stream(Amendment.values()).map(Enum::name).collect(Collectors.toSet());

  private static final Set<String> VALID_PARTICIPANTS =
      Arrays.stream(Participant.values()).map(Enum::name).collect(Collectors.toSet());

  private final SearchVideoRepository searchVideoRepository;

  @Transactional(readOnly = true)
  public ClusterResult findClusters(
      double minLat,
      double maxLat,
      double minLng,
      double maxLng,
      int zoom,
      Set<String> amendments,
      Set<String> participants) {

    double cellSize = 180.0 / Math.pow(2, zoom);
    String amendmentsArray = toValidatedPostgresArray(amendments, VALID_AMENDMENTS);
    String participantsArray = toValidatedPostgresArray(participants, VALID_PARTICIPANTS);

    List<Object[]> rows =
        searchVideoRepository.findClustersInBoundingBox(
            minLat, maxLat, minLng, maxLng, cellSize, amendmentsArray, participantsArray);

    List<ClusterResult.Cluster> clusters =
        rows.stream()
            .map(
                row -> {
                  double lat = ((Number) row[0]).doubleValue();
                  double lng = ((Number) row[1]).doubleValue();
                  long count = ((Number) row[2]).longValue();
                  long latCell = ((Number) row[3]).longValue();
                  long lngCell = ((Number) row[4]).longValue();
                  double boundsMinLat = ((Number) row[5]).doubleValue();
                  double boundsMaxLat = ((Number) row[6]).doubleValue();
                  double boundsMinLng = ((Number) row[7]).doubleValue();
                  double boundsMaxLng = ((Number) row[8]).doubleValue();

                  String id = "cluster_" + zoom + "_" + latCell + "_" + lngCell;

                  return new ClusterResult.Cluster(
                      id, lat, lng, count, boundsMinLat, boundsMaxLat, boundsMinLng, boundsMaxLng);
                })
            .toList();

    long totalLocations = clusters.stream().mapToLong(ClusterResult.Cluster::count).sum();

    return new ClusterResult(clusters, totalLocations, zoom);
  }

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
