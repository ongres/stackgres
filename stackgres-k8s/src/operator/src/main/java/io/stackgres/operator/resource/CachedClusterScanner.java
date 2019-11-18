/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.dto.Cluster;
import io.stackgres.operator.resource.dto.ClusterSpec;
import io.stackgres.operator.resource.dto.ClusterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CachedClusterScanner implements KubernetesResourceScanner<List<Cluster>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedClusterScanner.class);

  private KubernetesResourceScanner<StackGresClusterList> directClusterScanner;

  private KubernetesCustomResourceFinder<ClusterStatus> statusFinder;

  private LoadingCache<String, List<Cluster>> clusterCache;

  @Inject
  CachedClusterScanner(KubernetesResourceScanner<StackGresClusterList> directClusterScanner,
                       KubernetesCustomResourceFinder<ClusterStatus> statusFinder) {
    this.directClusterScanner = directClusterScanner;
    this.statusFinder = statusFinder;
  }

  @Scheduled(every = "10s")
  void refreshCache() {
    LOGGER.trace("Refreshing cluster cache");
    if (clusterCache == null) {
      return;
    }
    directClusterScanner.findResources().ifPresent(clusters -> {
      List<StackGresCluster> clusterItems = clusters.getItems();

      if (clusterItems != null && !clusterItems.isEmpty()) {

        Set<String> allNamespaces = clusterItems.stream()
            .map(c -> c.getMetadata().getNamespace())
            .collect(Collectors.toSet());

        Set<String> cachedNamespaces = clusterCache.asMap().keySet();

        allNamespaces.forEach(namespace -> {
          try {
            clusterCache.get(namespace);
          } catch (ExecutionException e) {
            LOGGER.warn("could not warm up namespace cache " + namespace);
          }
        });

        Set<String> namespacesNoLongerCached = cachedNamespaces.stream()
            .filter(cn -> !allNamespaces.contains(cn)).collect(Collectors.toSet());

        namespacesNoLongerCached.forEach(namespace -> {
          clusterCache.invalidate(namespace);
        });

      } else {
        clusterCache.invalidateAll();
      }

    });
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Starting cached cluster scanner");
    clusterCache = CacheBuilder.newBuilder()
        .refreshAfterWrite(10, TimeUnit.SECONDS)
        .build(new CacheLoader<String, List<Cluster>>() {
          @Override
          public List<Cluster> load(String key) {

            return directClusterScanner.findResources(key)
                .map(stackGresClusterList -> stackGresClusterList.getItems()
                    .stream()
                    .map(stackGresCluster -> {

                      Cluster cluster = new Cluster();
                      cluster.setApiVersion(stackGresCluster.getApiVersion());
                      cluster.setKind(stackGresCluster.getKind());
                      cluster.setMetadata(stackGresCluster.getMetadata());

                      ClusterSpec spec = new ClusterSpec();
                      spec.setConnectionPoolingConfig(stackGresCluster
                          .getSpec().getConnectionPoolingConfig());
                      spec.setEnvoyVersion(spec.getEnvoyVersion());
                      spec.setInstances(stackGresCluster.getSpec().getInstances());
                      spec.setPostgresConfig(stackGresCluster.getSpec().getPostgresConfig());
                      spec.setPostgresExporterVersion(stackGresCluster
                          .getSpec().getPostgresExporterVersion());
                      spec.setPostgresVersion(stackGresCluster.getSpec().getPostgresVersion());
                      spec.setPrometheusAutobind(stackGresCluster
                          .getSpec().getPrometheusAutobind());
                      spec.setResourceProfile(stackGresCluster.getSpec().getResourceProfile());
                      spec.setSidecars(stackGresCluster.getSpec().getSidecars());
                      spec.setStorageClass(stackGresCluster.getSpec().getStorageClass());
                      spec.setVolumeSize(stackGresCluster.getSpec().getVolumeSize());
                      cluster.setSpec(spec);

                      Optional<ClusterStatus> status = statusFinder.findByNameAndNamespace(
                          cluster.getMetadata().getName(),
                          cluster.getMetadata().getNamespace());

                      status.ifPresent(cluster::setStatus);

                      return cluster;
                    }).collect(Collectors.toList()))
                .orElseThrow(() ->
                    new IllegalArgumentException("No clusters in namespace " + key));

          }

        });

    refreshCache();

    LOGGER.info("Cached cluster scanner started");
  }

  @Override
  public Optional<List<Cluster>> findResources() {

    return Optional.ofNullable(clusterCache).flatMap(clusterCache -> {
      Map<String, List<Cluster>> clusterMap = clusterCache.asMap();

      if (clusterMap.keySet().isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(clusterMap.values().stream()
          .reduce(new ArrayList<>(), (l1, l2) -> {
            l1.addAll(l2);
            return l1;
          }));
    });

  }

  @Override
  public Optional<List<Cluster>> findResources(String namespace) {

    return Optional.ofNullable(clusterCache).flatMap(clusterCache -> {
      try {
        List<Cluster> clusterList = clusterCache.get(namespace);
        return Optional.of(clusterList);
      } catch (ExecutionException e) {
        LOGGER.error("Error while looking for clusters in namespace " + namespace, e);
        return Optional.empty();
      }
    });

  }
}
