/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForDbOps;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PodTestUtil {

  private static final String POD_NAME_FORMAT = "%s-%d";
  private static final String JOB_NAME_FORMAT = "%s-%s-%d-%s";

  @Inject
  LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  LabelFactoryForDbOps labelFactoryForDbOps;

  @Inject
  LabelFactoryForDbOps dbOpsLabelFactory;

  @Inject
  KubernetesClient client;

  public static void assertPodEquals(Pod expected, Pod actual) {
    expected = JsonUtil.copy(expected);
    expected.getMetadata().setCreationTimestamp(null);
    expected.getMetadata().setGeneration(null);
    expected.getMetadata().setResourceVersion(null);
    expected.getMetadata().setUid(null);
    actual = JsonUtil.copy(actual);
    actual.getMetadata().setCreationTimestamp(null);
    actual.getMetadata().setGeneration(null);
    actual.getMetadata().setResourceVersion(null);
    actual.getMetadata().setUid(null);
    JsonNode expectedJson = JsonUtil.toJson(expected);
    JsonNode actualJson = JsonUtil.toJson(actual);
    JsonUtil.assertJsonEquals(expectedJson, actualJson);
  }

  public void preparePods(StackGresCluster cluster, int primaryIndex, int... replicaIndexes) {
    createPod(buildPrimaryPod(cluster, primaryIndex));
    createPod(buildJobPod(cluster, primaryIndex));

    Arrays.stream(replicaIndexes)
        .forEach(replicaIndex -> createPod(buildReplicaPod(cluster, replicaIndex)));
  }

  public void preparePodsWithNoRoles(StackGresCluster cluster, int primaryIndex,
      int... replicaIndexes) {
    Pod primary = buildPrimaryPod(cluster, primaryIndex);
    primary.getMetadata().getLabels().remove(PatroniUtil.ROLE_KEY);
    createPod(primary);
    createPod(buildJobPod(cluster, primaryIndex));

    Arrays.stream(replicaIndexes)
        .forEach(replicaIndex -> {
          Pod replica = buildReplicaPod(cluster, replicaIndex);
          replica.getMetadata().getLabels().remove(PatroniUtil.ROLE_KEY);
          createPod(replica);
        });
  }

  public void createPod(Pod pod) {
    client.pods()
        .inNamespace(pod.getMetadata().getNamespace())
        .resource(pod)
        .create();
  }

  public List<Pod> getClusterPods(StackGresCluster cluster) {
    return client.pods().inNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labelFactory.clusterLabels(cluster))
        .list()
        .getItems()
        .stream().filter(pod -> !pod.getMetadata()
            .getLabels()
            .containsKey(labelFactoryForDbOps.labelMapper().resourceNameKey(null)))
        .collect(Collectors.toUnmodifiableList());
  }

  public Pod buildPrimaryPod(StackGresCluster cluster, int index) {
    final Map<String, String> labels = labelFactory.clusterPrimaryLabels(cluster);
    return buildPod(cluster, index, labels);
  }

  public Pod buildNonDisruptablePrimaryPod(StackGresCluster cluster, int index) {
    final Map<String, String> labels = labelFactory.clusterPrimaryLabels(cluster);
    return buildPod(cluster, index, ImmutableMap.<String, String>builder()
        .putAll(labels)
        .put(labelFactory.labelMapper().disruptibleKey(cluster),
            StackGresContext.WRONG_VALUE)
        .build());
  }

  public Pod buildReplicaPod(StackGresCluster cluster, int index) {
    final Map<String, String> labels = labelFactory.clusterReplicaLabels(cluster);
    return buildPod(cluster, index, labels);
  }

  public Pod buildJobPod(StackGresCluster cluster, int index) {
    String namespace = cluster.getMetadata().getNamespace();
    String clusterName = cluster.getMetadata().getName();
    StackGresDbOps dbOps = new StackGresDbOps();
    dbOps.setMetadata(cluster.getMetadata());
    final Map<String, String> labels = dbOpsLabelFactory.dbOpsPodLabels(dbOps);
    return new PodBuilder()
        .withNewMetadata()
        .withName(String.format(JOB_NAME_FORMAT, clusterName, clusterName, index,
            StringUtils.getRandomString(5)))
        .withNamespace(namespace)
        .withLabels(labels)
        .endMetadata()
        .build();
  }

  public Pod buildPod(StackGresCluster cluster, int index, Map<String, String> labels) {
    String namespace = cluster.getMetadata().getNamespace();
    String clusterName = cluster.getMetadata().getName();
    return new PodBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(String.format(POD_NAME_FORMAT, clusterName, index))
        .addToLabels(labels)
        .endMetadata()
        .build();
  }
}
