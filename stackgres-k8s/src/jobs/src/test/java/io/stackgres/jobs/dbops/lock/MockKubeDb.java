/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;

@ApplicationScoped
public class MockKubeDb {

  public static final JsonMapper JSON_MAPPER = new JsonMapper();
  private final Map<String, StoredCluster> clusterMap;
  private final Map<String, StackGresDbOps> dbOpsMap;
  private final Map<String, List<Consumer<StackGresCluster>>> clusterWatchers;
  String KEY_FORMAT = "%s/%s";

  public MockKubeDb() {
    this.clusterMap = new HashMap<>();
    this.dbOpsMap = new HashMap<>();
    clusterWatchers = new HashMap<>();
    JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JSON_MAPPER.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    JSON_MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
  }

  public StackGresCluster getCluster(String name, String namespace) {
    String key = getResourceKey(name, namespace);
    return copy(clusterMap.get(key).getCluster(), StackGresCluster.class);
  }

  public StackGresDbOps getDbOps(String name, String namespace) {
    String key = getResourceKey(name, namespace);
    return copy(dbOpsMap.get(key), StackGresDbOps.class);
  }

  private <T> T copy(T source, Class<T> clazz) {
    JsonNode jsonValue = JSON_MAPPER.valueToTree(source);
    try {
      return JSON_MAPPER.treeToValue(jsonValue, clazz);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public StackGresCluster addOrReplaceCluster(StackGresCluster cluster) {

    final String clusterKey = getResourceKey(cluster);
    if (clusterMap.containsKey(clusterKey)) {
      clusterMap.get(clusterKey).replace(cluster);
    } else {
      clusterMap.put(clusterKey, new StoredCluster(cluster));
    }

    StackGresCluster replacedCluster = clusterMap.get(clusterKey).getCluster();

    if (clusterWatchers.containsKey(clusterKey)) {
      clusterWatchers.get(clusterKey).forEach(consumer -> consumer.accept(replacedCluster));
    }

    return replacedCluster;

  }

  public void watchCluster(String name, String namespace, Consumer<StackGresCluster> consumer) {

    String clusterKey = getResourceKey(name, namespace);
    if (!clusterWatchers.containsKey(clusterKey)) {
      clusterWatchers.put(clusterKey, new ArrayList<>());
    }
    clusterWatchers.get(clusterKey).add(consumer);
  }

  public StackGresDbOps addOrReplaceDbOps(StackGresDbOps dbOp) {
    final StackGresDbOps resourceCopy = copy(dbOp, StackGresDbOps.class);
    final String key = getResourceKey(dbOp);
    if (dbOpsMap.containsKey(key)) {
      var oldCluster = dbOpsMap.get(key);
      var oldVersion = oldCluster.getMetadata().getResourceVersion();
      var newVersion = resourceCopy.getMetadata().getResourceVersion();
      if (oldVersion.equals(newVersion)) {
        int updatedVersion = Integer.parseInt(oldVersion) + 1;
        resourceCopy.getMetadata().setResourceVersion(Integer.toString(updatedVersion));
      } else {
        throw new IllegalArgumentException("DbOps override with data loss");
      }
    } else {
      resourceCopy.getMetadata().setResourceVersion("1");
    }

    dbOpsMap.put(key, resourceCopy);
    return resourceCopy;

  }

  public void introduceReplaceFailures(int numberOfFailures, StackGresCluster cluster) {

    String resourceKey = getResourceKey(cluster);

    clusterMap.get(resourceKey).introduceFailures(numberOfFailures);
  }

  private String getResourceKey(HasMetadata resource) {
    return getResourceKey(resource.getMetadata().getName(), resource.getMetadata().getNamespace());
  }

  private String getResourceKey(String name, String namespace) {
    return String.format(KEY_FORMAT, namespace, name);
  }

  public void delete(StackGresCluster cluster) {
    String key = getResourceKey(cluster);
    var deleted = clusterMap.remove(key);
    if (clusterWatchers.containsKey(key)) {
      clusterWatchers.get(key).forEach(consumer -> consumer.accept(deleted.getCluster()));
    }
  }

  public void delete(StackGresDbOps dbOp) {
    String key = getResourceKey(dbOp);
    dbOpsMap.remove(key);
  }

  private class StoredCluster {
    private StackGresCluster cluster;
    private int pendingFailures;

    public StoredCluster(StackGresCluster cluster) {
      this.cluster = copy(cluster, StackGresCluster.class);
      this.pendingFailures = 0;
      this.cluster.getMetadata().setResourceVersion("1");
    }

    public StackGresCluster getCluster() {
      return cluster;
    }

    public void introduceFailures(int failures) {
      pendingFailures += failures;
    }

    synchronized void replace(StackGresCluster newCluster) {
      var oldVersion = cluster.getMetadata().getResourceVersion();
      if (pendingFailures > 0) {
        pendingFailures--;
        final StackGresCluster clusterCopy = copy(cluster, StackGresCluster.class);
        int updatedVersion = Integer.parseInt(oldVersion) + 1;
        clusterCopy.getMetadata().setResourceVersion(Integer.toString(updatedVersion));
        throw new RuntimeException("Simulated failure");
      }
      final StackGresCluster newClusterCopy = copy(newCluster, StackGresCluster.class);
      var newVersion = newClusterCopy.getMetadata().getResourceVersion();

      if (oldVersion.equals(newVersion)) {
        int updatedVersion = Integer.parseInt(oldVersion) + 1;
        newClusterCopy.getMetadata().setResourceVersion(Integer.toString(updatedVersion));
      } else {
        throw new IllegalArgumentException("Cluster override with data loss");
      }
      cluster = newClusterCopy;
    }

  }
}
