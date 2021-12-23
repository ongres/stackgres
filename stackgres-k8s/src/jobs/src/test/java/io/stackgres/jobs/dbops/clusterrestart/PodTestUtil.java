/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.PatchUtils;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.LabelFactoryForDbOps;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.testutil.StringUtils;

@ApplicationScoped
public class PodTestUtil {

  private static final String POD_NAME_FORMAT = "%s-%d";
  private static final String JOB_NAME_FORMAT = "%s-%s-%d-%s";

  @Inject
  LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  LabelFactoryForDbOps dbOpsLabelFactory;

  @Inject
  KubernetesClient client;

  public static void assertPodEquals(Pod expected, Pod actual) {
    var pm = PatchUtils.patchMapper();
    String expectedJson = pm.valueToTree(expected).toPrettyString();
    String actualJson = pm.valueToTree(actual).toPrettyString();
    assertEquals(expectedJson, actualJson);
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
        .withName(pod.getMetadata().getName())
        .create(pod);
  }

  public List<Pod> getClusterPods(StackGresCluster cluster) {
    return client.pods().inNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labelFactory.patroniClusterLabels(cluster))
        .list()
        .getItems()
        .stream().filter(pod -> !pod.getMetadata()
            .getLabels()
            .containsKey(StackGresContext.DB_OPS_KEY))
        .collect(Collectors.toUnmodifiableList());
  }

  public Pod buildPrimaryPod(StackGresCluster cluster, int index) {
    final Map<String, String> labels = labelFactory.patroniPrimaryLabels(cluster);
    return buildPod(cluster, index, labels);
  }

  public Pod buildNonDisruptablePrimaryPod(StackGresCluster cluster, int index) {
    final Map<String, String> labels = labelFactory.patroniPrimaryLabels(cluster);
    return buildPod(cluster, index, ImmutableMap.<String, String>builder()
        .putAll(labels)
        .put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE)
        .build());
  }

  public Pod buildReplicaPod(StackGresCluster cluster, int index) {
    final Map<String, String> labels = labelFactory.patroniReplicaLabels(cluster);
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
