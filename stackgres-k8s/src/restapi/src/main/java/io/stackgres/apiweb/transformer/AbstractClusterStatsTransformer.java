/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.dto.cluster.ClusterStats;
import io.stackgres.apiweb.dto.cluster.KubernetesPod;
import io.stackgres.apiweb.resource.PatroniStatsScripts;
import io.stackgres.apiweb.resource.PodStats;
import io.stackgres.apiweb.transformer.util.PodStatsUtil;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractClusterStatsTransformer<
    T extends ResourceDto, R extends CustomResource<?, ?>>
    extends AbstractDtoTransformer<T, R> {

  private final ClusterPodTransformer clusterPodTransformer;

  protected AbstractClusterStatsTransformer(ClusterPodTransformer clusterPodTransformer) {
    this.clusterPodTransformer = clusterPodTransformer;
  }

  public AbstractClusterStatsTransformer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.clusterPodTransformer = null;
  }

  protected void setAllStats(ClusterStats clusterStats, List<PodStats> allPodStats) {
    clusterStats.setPods(Seq.seq(allPodStats)
        .map(PodStats::v1)
        .map(clusterPodTransformer::toResource)
        .toList());

    clusterStats.setPodsReady((int) clusterStats.getPods()
        .stream()
        .filter(pod -> pod.getContainers().equals(pod.getContainersReady()))
        .count());

    clusterStats.setPods(Seq.seq(clusterStats.getPods())
        .zip(allPodStats)
        .peek(t -> setPodRequested(t.v1, t.v2))
        .peek(t -> setPodStats(t.v1, t.v2.v2))
        .map(Tuple2::v1)
        .toList());

    setGlobalRequested(clusterStats, allPodStats);
    setGlobalStats(clusterStats, allPodStats);
  }

  protected void setPodStats(KubernetesPod pod,
      ImmutableMap<PatroniStatsScripts, String> podStats) {
    pod.setCpuFound(
        cpuStats(podStats)
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    pod.setCpuPsiAvg10(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.CPU_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setCpuPsiAvg60(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.CPU_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setCpuPsiAvg300(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.CPU_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setCpuPsiTotal(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.CPU_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setMemoryFound(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setMemoryUsed(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setMemoryPsiAvg10(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiAvg60(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiAvg300(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiTotal(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setMemoryPsiFullAvg10(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiFullAvg60(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiFullAvg300(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setMemoryPsiFullTotal(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.MEMORY_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setDiskFound(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setDiskUsed(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    pod.setDiskPsiAvg10(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiAvg60(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiAvg300(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiTotal(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setDiskPsiFullAvg10(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiFullAvg60(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiFullAvg300(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setDiskPsiFullTotal(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.DISK_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    pod.setAverageLoad1m(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.LOAD_1M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setAverageLoad5m(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.LOAD_5M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setAverageLoad10m(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.LOAD_10M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    pod.setConnections(PodStatsUtil.stats(
        podStats, PatroniStatsScripts.CONNECTIONS,
        ResourceUtil::toBigInteger, String::valueOf));
  }

  protected Optional<BigInteger> cpuStats(ImmutableMap<PatroniStatsScripts, String> podStats) {
    return PodStatsUtil.stats(podStats, PatroniStatsScripts.CPU_QUOTA,
        ResourceUtil::toBigInteger)
    .flatMap(cpuQuota -> PodStatsUtil
        .stats(podStats, PatroniStatsScripts.CPU_PERIOD, ResourceUtil::toBigInteger)
        .map(cpuPeriod -> Tuple.tuple(cpuQuota, cpuPeriod)))
    .flatMap(cpuStats -> PodStatsUtil
        .stats(podStats, PatroniStatsScripts.CPU_FOUND, ResourceUtil::toBigInteger)
        .map(cpuFound -> cpuStats.concat(cpuFound)))
    .map(t -> t.v1.compareTo(BigInteger.ONE.negate()) != 0
      ? t.v1.multiply(ResourceUtil.MILLICPU_MULTIPLIER.toBigInteger()).divide(t.v2)
      : t.v3);
  }

  protected void setPodRequested(KubernetesPod pod, PodStats podStats) {
    pod.setCpuRequested(PodStatsUtil.getPodCpuRequested(podStats)
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    pod.setMemoryRequested(PodStatsUtil.getPodMemoryRequested(podStats)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
    pod.setDiskRequested(PodStatsUtil.getPodDiskRequested(podStats)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
  }

  protected void setGlobalStats(ClusterStats stats,
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
    stats.setCpuPsiAvg10(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setCpuPsiAvg60(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setCpuPsiAvg300(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setCpuPsiTotal(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.CPU_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setMemoryFound(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setMemoryUsed(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setMemoryPsiAvg10(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiAvg60(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiAvg300(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiTotal(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setMemoryPsiFullAvg10(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiFullAvg60(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiFullAvg300(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setMemoryPsiFullTotal(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.MEMORY_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setDiskFound(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.DISK_FOUND,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setDiskUsed(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.DISK_USED,
        ResourceUtil::toBigInteger, ResourceUtil::asBytesWithUnit));
    stats.setDiskPsiAvg10(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiAvg60(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiAvg300(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiTotal(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setDiskPsiFullAvg10(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_AVG10,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiFullAvg60(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_AVG60,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiFullAvg300(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_AVG300,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setDiskPsiFullTotal(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.DISK_PSI_FULL_TOTAL,
        ResourceUtil::toBigInteger, String::valueOf));
    stats.setAverageLoad1m(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.LOAD_1M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setAverageLoad5m(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.LOAD_5M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setAverageLoad10m(PodStatsUtil.averageStats(
        allPodStats, PatroniStatsScripts.LOAD_10M,
        ResourceUtil::toMilliload, ResourceUtil::asLoad));
    stats.setConnections(PodStatsUtil.sumStats(
        allPodStats, PatroniStatsScripts.CONNECTIONS,
        ResourceUtil::toBigInteger, String::valueOf));
  }

  protected void setGlobalRequested(ClusterStats stats,
      List<PodStats> allPodStats) {
    stats.setCpuRequested(allPodStats
        .stream()
        .map(PodStatsUtil::getPodCpuRequested)
        .flatMap(Optional::stream)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    stats.setMemoryRequested(allPodStats
        .stream()
        .map(PodStatsUtil::getPodMemoryRequested)
        .flatMap(Optional::stream)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
    stats.setDiskRequested(allPodStats
        .stream()
        .map(PodStatsUtil::getPodDiskRequested)
        .flatMap(Optional::stream)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
  }

}
