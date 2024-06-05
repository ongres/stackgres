/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.stackgres.apiweb.resource.PatroniStatsScripts;
import io.stackgres.apiweb.resource.PodStats;
import io.stackgres.common.resource.ResourceUtil;
import org.jooq.lambda.Seq;

public interface PodStatsUtil {

  static String stats(
      ImmutableMap<PatroniStatsScripts, String> podStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper,
      Function<BigInteger, String> resultMapper) {
    return stats(podStats, patroniStatsScripts, statMapper)
        .map(resultMapper)
        .orElse(null);
  }

  static Optional<BigInteger> stats(
      ImmutableMap<PatroniStatsScripts, String> podStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper) {
    return Optional.ofNullable(podStats
        .get(patroniStatsScripts))
        .flatMap(statMapper);
  }

  static String sumStats(
      List<PodStats> allPodStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper,
      Function<BigInteger, String> resultMapper) {
    return sumStats(allPodStats, patroniStatsScripts, statMapper)
        .map(resultMapper)
        .orElse(null);
  }

  static Optional<BigInteger> sumStats(
      List<PodStats> allPodStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper) {
    return Optional.of(Seq.seq(allPodStats)
        .map(PodStats::v2)
        .map(podStats -> stats(podStats, patroniStatsScripts, statMapper))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList())
        .filter(list -> !list.isEmpty())
        .map(list -> list.stream()
            .reduce(BigInteger::add)
            .orElse(BigInteger.ZERO));
  }

  static String averageStats(
      List<PodStats> allPodStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper,
      Function<BigInteger, String> resultMapper) {
    return Optional.of(Seq.seq(allPodStats)
        .map(PodStats::v2)
        .map(podStats -> stats(podStats, patroniStatsScripts, statMapper))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList())
        .filter(list -> !list.isEmpty())
        .map(list -> list.stream()
            .reduce(BigInteger::add)
            .orElse(BigInteger.ZERO)
            .divide(BigInteger.valueOf(list.size())))
        .map(resultMapper)
        .orElse(null);
  }

  static Optional<BigInteger> getPodCpuRequested(PodStats podStats) {
    return podStats.v1.getSpec().getContainers().stream()
        .filter(c -> Optional.ofNullable(c.getResources())
            .map(ResourceRequirements::getRequests)
            .map(requests -> requests.containsKey("cpu"))
            .orElse(false))
        .map(c -> c.getResources().getRequests().get("cpu"))
        .map(q -> q.getFormat() == null || q.getFormat().isEmpty()
        ? new BigDecimal(q.getAmount()) : Quantity.getAmountInBytes(q))
        .map(q -> q.multiply(ResourceUtil.MILLICPU_MULTIPLIER))
        .map(BigDecimal::toBigInteger)
        .reduce(BigInteger::add);
  }

  static Optional<BigInteger> getPodMemoryRequested(PodStats podStats) {
    return podStats.v1.getSpec().getContainers().stream()
        .filter(c -> Optional.ofNullable(c.getResources())
            .map(ResourceRequirements::getRequests)
            .map(requests -> requests.containsKey("memory"))
            .orElse(false))
        .map(c -> c.getResources().getRequests().get("memory"))
        .map(q -> q.getFormat() == null || q.getFormat().isEmpty()
        ? new BigDecimal(q.getAmount()) : Quantity.getAmountInBytes(q))
        .map(BigDecimal::toBigInteger)
        .reduce(BigInteger::add);
  }

  static Optional<BigInteger> getPodDiskRequested(PodStats podStats) {
    return podStats.v3.map(PersistentVolumeClaim::getSpec)
    .map(PersistentVolumeClaimSpec::getResources)
    .map(ResourceRequirements::getRequests)
    .filter(r -> r.containsKey("storage"))
    .map(r -> r.get("storage"))
    .map(q -> q.getFormat() == null || q.getFormat().isEmpty()
    ? new BigDecimal(q.getAmount()) : Quantity.getAmountInBytes(q))
    .map(BigDecimal::toBigInteger);
  }

}
