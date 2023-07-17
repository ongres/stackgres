/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class MockKubeDb {

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
  private static final String KEY_FORMAT = "%s/%s";
  private static final String PENDING_FAILURES = "pendingFailures";

  private final Map<Tuple2<Class<?>, String>, CustomResource<?, ?>> customResourceMap;
  private final Map<Tuple2<Class<?>, String>, List<Consumer<CustomResource<?, ?>>>>
      customResourceWatchers;

  public MockKubeDb() {
    this.customResourceMap = new HashMap<>();
    customResourceWatchers = new HashMap<>();
    JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JSON_MAPPER.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    JSON_MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
  }

  private <T extends CustomResource<?, ?>> Tuple2<Class<?>, String> getResourceKey(
      T resource, Class<T> customResourceClass) {
    return getResourceKey(resource.getMetadata().getName(), resource.getMetadata().getNamespace(),
        customResourceClass);
  }

  private <T extends CustomResource<?, ?>> Tuple2<Class<?>, String> getResourceKey(
      String name, String namespace, Class<T> customResourceClass) {
    return Tuple.tuple(customResourceClass, String.format(KEY_FORMAT, namespace, name));
  }

  private <T extends CustomResource<?, ?>> T getCustomResource(T customResource,
      Class<T> customResourceClass) {
    var key = getResourceKey(customResource.getMetadata().getName(),
        customResource.getMetadata().getNamespace(), customResourceClass);
    return customResourceClass.cast(customResourceMap.get(key));
  }

  private <T extends CustomResource<?, ?>> T getCustomResource(String name, String namespace,
      Class<T> customResourceClass) {
    var key = getResourceKey(name, namespace, customResourceClass);
    return customResourceClass.cast(customResourceMap.get(key));
  }

  private <T extends CustomResource<?, ?>> T copy(T source, Class<T> clazz) {
    JsonNode jsonValue = JSON_MAPPER.valueToTree(source);
    try {
      T customResourceCopy = JSON_MAPPER.treeToValue(jsonValue, clazz);
      if (customResourceCopy != null) {
        customResourceCopy.getMetadata().getAdditionalProperties().remove(PENDING_FAILURES);
      }
      return customResourceCopy;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private <T extends CustomResource<?, ?>> T copyCustomResource(String name, String namespace,
      Class<T> customResourceClass) {
    return copy(getCustomResource(name, namespace, customResourceClass), customResourceClass);
  }

  private <T extends CustomResource<?, ?>> T addOrReplaceCustomResource(T customResource,
      Class<T> customResourceClass) {
    final T storedCustomResource = getCustomResource(customResource, customResourceClass);
    final T customResourceCopy = copy(customResource, customResourceClass);
    var customResourceKey = getResourceKey(customResource, customResourceClass);
    if (customResourceMap.containsKey(customResourceKey)) {
      Optional<Integer> pendingFailures = Optional.ofNullable((Integer) storedCustomResource
          .getMetadata().getAdditionalProperties().get(PENDING_FAILURES));
      if (pendingFailures.orElse(0) > 0) {
        storedCustomResource.getMetadata().getAdditionalProperties()
            .put(PENDING_FAILURES, pendingFailures.get() - 1);
        throw new RuntimeException("Simulated failure");
      }
      var oldCustomResource = getCustomResource(customResource, customResourceClass);
      var oldVersion = oldCustomResource.getMetadata().getResourceVersion();
      var newVersion = customResourceCopy.getMetadata().getResourceVersion();
      if (oldVersion.equals(newVersion)) {
        int updatedVersion = Integer.parseInt(oldVersion) + 1;
        customResourceCopy.getMetadata().setResourceVersion(Integer.toString(updatedVersion));
      } else {
        throw new IllegalArgumentException(
            customResourceClass.getSimpleName() + " override with data loss");
      }
    } else {
      customResourceCopy.getMetadata().setResourceVersion("1");
      customResourceCopy.getMetadata().setUid(UUID.randomUUID().toString());
    }
    customResourceMap.put(customResourceKey, customResourceCopy);

    if (customResourceWatchers.containsKey(customResourceKey)) {
      customResourceWatchers.get(customResourceKey)
          .forEach(consumer -> consumer.accept(customResourceCopy));
    }

    return customResourceCopy;

  }

  private <T extends CustomResource<?, ?>> void watchCustomResource(String name, String namespace,
      Consumer<T> consumer, Class<T> customResourceClass) {
    var customResourceKey = getResourceKey(name, namespace, customResourceClass);
    if (!customResourceWatchers.containsKey(customResourceKey)) {
      customResourceWatchers.put(customResourceKey, new ArrayList<>());
    }
    customResourceWatchers.get(customResourceKey).add(customResource -> consumer
        .accept(customResourceClass.cast(customResource)));
  }

  private <T extends CustomResource<?, ?>> void delete(T customResource,
      Class<T> customResourceClass) {
    var customResourceKey = getResourceKey(customResource, customResourceClass);
    var deleted = customResourceMap.remove(customResourceKey);
    if (customResourceWatchers.containsKey(customResourceKey)) {
      customResourceWatchers.get(customResourceKey).forEach(consumer -> consumer.accept(deleted));
    }
  }

  public void delete(StackGresCluster cluster) {
    delete(cluster, StackGresCluster.class);
  }

  public void delete(StackGresDbOps dbOps) {
    delete(dbOps, StackGresDbOps.class);
  }

  public StackGresCluster getCluster(String name, String namespace) {
    return copyCustomResource(name, namespace, StackGresCluster.class);
  }

  public StackGresCluster addOrReplaceCluster(StackGresCluster cluster) {
    return addOrReplaceCustomResource(cluster, StackGresCluster.class);
  }

  public void watchCluster(String name, String namespace, Consumer<StackGresCluster> consumer) {
    watchCustomResource(name, namespace, consumer, StackGresCluster.class);
  }

  public StackGresDbOps getDbOps(String name, String namespace) {
    return copyCustomResource(name, namespace, StackGresDbOps.class);
  }

  public StackGresDbOps addOrReplaceDbOps(StackGresDbOps cluster) {
    return addOrReplaceCustomResource(cluster, StackGresDbOps.class);
  }

  public void watchDbOps(String name, String namespace, Consumer<StackGresDbOps> consumer) {
    watchCustomResource(name, namespace, consumer, StackGresDbOps.class);
  }

  public void introduceReplaceFailures(int i, StackGresCluster cluster) {
    StackGresCluster storedCluster = getCustomResource(cluster, StackGresCluster.class);
    int pendingFailures =
        Optional.ofNullable((Integer) storedCluster
            .getMetadata().getAdditionalProperties().get(PENDING_FAILURES))
            .orElse(0) + 1;
    storedCluster.getMetadata().getAdditionalProperties().put(PENDING_FAILURES, pendingFailures);
  }

}
