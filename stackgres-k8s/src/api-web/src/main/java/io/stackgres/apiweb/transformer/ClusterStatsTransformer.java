/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.dto.cluster.KubernetesPod;
import io.stackgres.apiweb.resource.PatroniStatsScripts;
import io.stackgres.apiweb.resource.PodStats;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterStatsTransformer
    extends AbstractDtoTransformer<ClusterStatsDto, StackGresCluster> {

  private final ClusterPodTransformer clusterPodTransformer;

  @Inject
  public ClusterStatsTransformer(ClusterPodTransformer clusterPodTransformer) {
    this.clusterPodTransformer = clusterPodTransformer;
  }

  public ClusterStatsDto toDtoWithAllPodStats(
      StackGresCluster source, List<PodStats> allPodStats) {
    ClusterStatsDto clusterStatsDto = toDto(source);

    clusterStatsDto.setPods(Seq.seq(allPodStats)
        .map(PodStats::v1)
        .map(clusterPodTransformer::toResource)
        .toList());

    clusterStatsDto.setPodsReady((int) clusterStatsDto.getPods()
        .stream()
        .filter(pod -> pod.getContainers().equals(pod.getContainersReady()))
        .count());

    setAllStats(clusterStatsDto, allPodStats);

    return clusterStatsDto;
  }

  @Override
  public ClusterStatsDto toDto(StackGresCluster source) {
    ClusterStatsDto transformation = new ClusterStatsDto();
    transformation.setMetadata(getDtoMetadata(source));
    return transformation;
  }

  private void setAllStats(ClusterStatsDto stats, List<PodStats> allPodStats) {
    stats.setPods(Seq.seq(stats.getPods())
        .zip(allPodStats)
        .peek(t -> setPodRequested(t.v1, t.v2))
        .peek(t -> setPodStats(t.v1, t.v2.v2))
        .map(Tuple2::v1)
        .toList());

    setGlobalRequested(stats, allPodStats);
    setGlobalStats(stats, allPodStats);
  }

  private void setPodStats(KubernetesPod pod, ImmutableMap<PatroniStatsScripts, String> podStats) {
    pod.setCpuFound(
        cpuStats(podStats)
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    pod.setCpuPsiAvg10(stats(
        podStats, PatroniStatsScripts.CPU_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setCpuPsiAvg60(stats(
        podStats, PatroniStatsScripts.CPU_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setCpuPsiAvg300(stats(
        podStats, PatroniStatsScripts.CPU_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setCpuPsiTotal(stats(
        podStats, PatroniStatsScripts.CPU_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setMemoryFound(stats(
        podStats, PatroniStatsScripts.MEMORY_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setMemoryUsed(stats(
        podStats, PatroniStatsScripts.MEMORY_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setMemoryPsiAvg10(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiAvg60(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiAvg300(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiTotal(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setMemoryPsiFullAvg10(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiFullAvg60(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiFullAvg300(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiFullTotal(stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setDiskFound(stats(
        podStats, PatroniStatsScripts.DISK_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setDiskUsed(stats(
        podStats, PatroniStatsScripts.DISK_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setDiskPsiAvg10(stats(
        podStats, PatroniStatsScripts.DISK_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiAvg60(stats(
        podStats, PatroniStatsScripts.DISK_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiAvg300(stats(
        podStats, PatroniStatsScripts.DISK_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiTotal(stats(
        podStats, PatroniStatsScripts.DISK_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setDiskPsiFullAvg10(stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiFullAvg60(stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiFullAvg300(stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiFullTotal(stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setAverageLoad1m(stats(
        podStats, PatroniStatsScripts.LOAD_1M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setAverageLoad5m(stats(
        podStats, PatroniStatsScripts.LOAD_5M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setAverageLoad10m(stats(
        podStats, PatroniStatsScripts.LOAD_10M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setConnections(stats(
        podStats, PatroniStatsScripts.CONNECTIONS,
        ResourceUtil::toBigInteger, String::valueOf));
  }

  private Optional<BigInteger> cpuStats(ImmutableMap<PatroniStatsScripts, String> podStats) {
    return stats(podStats, PatroniStatsScripts.CPU_QUOTA,
        ResourceUtil::toBigInteger)
    .flatMap(cpuQuota -> stats(podStats, PatroniStatsScripts.CPU_PERIOD, ResourceUtil::toBigInteger)
        .map(cpuPeriod -> Tuple.tuple(cpuQuota, cpuPeriod)))
    .flatMap(cpuStats -> stats(podStats, PatroniStatsScripts.CPU_FOUND, ResourceUtil::toBigInteger)
        .map(cpuFound -> cpuStats.concat(cpuFound)))
    .map(t -> t.v1.compareTo(BigInteger.ONE.negate()) != 0
      ? t.v1.multiply(ResourceUtil.MILLICPU_MULTIPLIER.toBigInteger()).divide(t.v2)
      : t.v3);
  }

  private void setPodRequested(KubernetesPod pod, PodStats podStats) {
    pod.setCpuRequested(getPodCpuRequested(podStats)
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    pod.setMemoryRequested(getPodMemoryRequested(podStats)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
    pod.setDiskRequested(getPodDiskRequested(podStats)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
  }

  private void setGlobalStats(ClusterStatsDto stats,
      List<PodStats> allPodStats) {
    stats.setCpuFound(
        Optional.of(Seq.seq(allPodStats)
            .map(podStats -> cpuStats(podStats.v2))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList())
        .filter(list -> !list.isEmpty())
        .map(list -> list.stream()
            .reduce(BigInteger::add)
            .orElse(BigInteger.ZERO))
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    stats.setCpuPsiAvg10(averageStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setCpuPsiAvg60(averageStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setCpuPsiAvg300(averageStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setCpuPsiTotal(sumStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setMemoryFound(sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setMemoryUsed(sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setMemoryPsiAvg10(averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiAvg60(averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiAvg300(averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiTotal(sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setMemoryPsiFullAvg10(averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiFullAvg60(averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiFullAvg300(averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiFullTotal(sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setDiskFound(sumStats(
        allPodStats, PatroniStatsScripts.DISK_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setDiskUsed(sumStats(
        allPodStats, PatroniStatsScripts.DISK_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setDiskPsiAvg10(averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiAvg60(averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiAvg300(averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiTotal(sumStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setDiskPsiFullAvg10(averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiFullAvg60(averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiFullAvg300(averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiFullTotal(sumStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setAverageLoad1m(averageStats(
        allPodStats, PatroniStatsScripts.LOAD_1M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setAverageLoad5m(averageStats(
        allPodStats, PatroniStatsScripts.LOAD_5M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setAverageLoad10m(averageStats(
        allPodStats, PatroniStatsScripts.LOAD_10M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setConnections(sumStats(
        allPodStats, PatroniStatsScripts.CONNECTIONS,
        ResourceUtil::toBigInteger, String::valueOf));
  }

  private void setGlobalRequested(ClusterStatsDto stats,
      List<PodStats> allPodStats) {
    stats.setCpuRequested(allPodStats
        .stream()
        .map(this::getPodCpuRequested)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    stats.setMemoryRequested(allPodStats
        .stream()
        .map(this::getPodMemoryRequested)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
    stats.setDiskRequested(allPodStats
        .stream()
        .map(this::getPodDiskRequested)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
  }

  protected String stats(
      ImmutableMap<PatroniStatsScripts, String> podStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper,
      Function<BigInteger, String> resultMapper) {
    return stats(podStats, patroniStatsScripts, statMapper)
        .map(resultMapper)
        .orElse(null);
  }

  protected Optional<BigInteger> stats(
      ImmutableMap<PatroniStatsScripts, String> podStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper) {
    return Optional.ofNullable(podStats
        .get(patroniStatsScripts))
        .flatMap(statMapper);
  }

  protected String sumStats(
      List<PodStats> allPodStats,
      PatroniStatsScripts patroniStatsScripts,
      Function<String, Optional<BigInteger>> statMapper,
      Function<BigInteger, String> resultMapper) {
    return sumStats(allPodStats, patroniStatsScripts, statMapper)
        .map(resultMapper)
        .orElse(null);
  }

  protected Optional<BigInteger> sumStats(
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

  protected String averageStats(
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

  private Optional<BigInteger> getPodCpuRequested(PodStats podStats) {
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

  private Optional<BigInteger> getPodMemoryRequested(PodStats podStats) {
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

  private Optional<BigInteger> getPodDiskRequested(PodStats podStats) {
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
