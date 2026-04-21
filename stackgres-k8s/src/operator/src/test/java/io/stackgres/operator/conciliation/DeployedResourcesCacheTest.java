/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Fix-verification tests for {@link DeployedResourcesCache}. Both tests assert the DESIRED
 * post-fix behavior:
 *
 * <ul>
 *   <li>T1: without {@code RECONCILIATION_CACHE_SIZE} / {@code RECONCILIATION_CACHE_EXPIRATION}
 *       set, the cache must still apply a sane default bound so it cannot grow without limit.</li>
 *   <li>T2: {@link DeployedResourcesCache#createDeployedResourcesSnapshot} must allocate
 *       proportionally to the CR being reconciled, not to the total cache size across all
 *       generators.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DeployedResourcesCacheTest {

  @Mock
  private OperatorPropertyContext propertyContext;

  private DeployedResourcesCache cache;

  @BeforeEach
  void setUp() {
    when(propertyContext.get(eq(OperatorProperty.RECONCILIATION_CACHE_SIZE)))
        .thenReturn(Optional.empty());
    when(propertyContext.get(eq(OperatorProperty.RECONCILIATION_CACHE_EXPIRATION)))
        .thenReturn(Optional.empty());
    cache = new DeployedResourcesCache(propertyContext, JsonUtil.jsonMapper());
  }

  private static HasMetadata generator(String name) {
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace("test")
        .withName(name)
        .endMetadata()
        .build();
  }

  private static HasMetadata configMap(String name) {
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace("test")
        .withName(name)
        .withResourceVersion("1")
        .endMetadata()
        .build();
  }

  private static void fill(DeployedResourcesCache cache, HasMetadata generator,
      String prefix, int count) {
    for (int i = 0; i < count; i++) {
      HasMetadata cm = configMap(prefix + "-" + i);
      cache.put(generator, cm, cm);
    }
  }

  @Test
  void t1_cacheHasDefaultBoundWithoutProperties() {
    final int attempted = 200_000;
    HasMetadata gen = generator("gen-t1");
    fill(cache, gen, "e", attempted);
    // Allow Caffeine to process its pending eviction tasks.
    long retained = cache.stream().count();
    System.out.println("[T1] attempted=" + attempted + " retained=" + retained);
    assertTrue(retained < attempted,
        "Expected default eviction: retained (" + retained
            + ") must be strictly less than attempted (" + attempted + ").");
    assertTrue(retained > 1_000,
        "Expected default bound to be non-pathological: retained (" + retained
            + ") must be > 1000.");
  }

  @Test
  void t2_snapshotAllocationDoesNotScaleWithUnrelatedEntries() {
    DeployedResourcesCache smallCache =
        new DeployedResourcesCache(propertyContext, JsonUtil.jsonMapper());
    DeployedResourcesCache bigCache =
        new DeployedResourcesCache(propertyContext, JsonUtil.jsonMapper());

    HasMetadata smallGen = generator("small-gen");
    HasMetadata bigGen = generator("big-gen");
    HasMetadata unrelatedGen = generator("unrelated-gen");

    fill(smallCache, smallGen, "s", 100);
    fill(bigCache, bigGen, "b", 10_000);

    com.sun.management.ThreadMXBean threadMx =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    long tid = Thread.currentThread().getId();

    // Warm-up to avoid JIT / class-loading noise.
    smallCache.createDeployedResourcesSnapshot(unrelatedGen, List.of(), List.of());
    bigCache.createDeployedResourcesSnapshot(unrelatedGen, List.of(), List.of());

    long beforeSmall = threadMx.getThreadAllocatedBytes(tid);
    smallCache.createDeployedResourcesSnapshot(unrelatedGen, List.of(), List.of());
    long afterSmall = threadMx.getThreadAllocatedBytes(tid);
    long smallDelta = afterSmall - beforeSmall;

    long beforeBig = threadMx.getThreadAllocatedBytes(tid);
    bigCache.createDeployedResourcesSnapshot(unrelatedGen, List.of(), List.of());
    long afterBig = threadMx.getThreadAllocatedBytes(tid);
    long bigDelta = afterBig - beforeBig;

    System.out.println("[T2] smallCache(100) createDeployedResourcesSnapshot delta = "
        + smallDelta + " bytes");
    System.out.println("[T2] bigCache(10000) createDeployedResourcesSnapshot delta = "
        + bigDelta + " bytes");
    System.out.println("[T2] ratio bigDelta/smallDelta = "
        + (smallDelta == 0 ? "inf" : ((double) bigDelta / (double) smallDelta)));

    assertTrue(bigDelta < smallDelta * 5L,
        "Expected bigDelta (" + bigDelta + ") < 5 * smallDelta (" + smallDelta
            + "): allocation of createDeployedResourcesSnapshot must be proportional only to this"
            + " generator's own entries, not to total cache size.");
  }

}
