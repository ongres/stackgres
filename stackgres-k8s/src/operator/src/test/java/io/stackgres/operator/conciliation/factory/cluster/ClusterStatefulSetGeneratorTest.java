/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ClusterStatefulSetGeneratorTest extends GeneratorTest {

  @Test
  @DisplayName("Test StatefulSet generation for version 0.9.5")
  void testStatefulSetForV095() {

    StatefulSet expectedStatefulSet = getExpectedStatefulSet();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .andPodAnnotations(Map.of("podsAnnotation", "podsAnnotationValue"))
        .andInstanceProfile("500m", "128Mi")
        .andNumberOfInstances(2)
        .andStorageSize("500Mi")
        .generatedResourceShouldBeEqualTo(expectedStatefulSet);

  }

  @NotNull
  private StatefulSet getExpectedStatefulSet() {

    var default095StatefulSet = JsonUtil.readFromJson("statefulset/0.9.5-cluster.json",
        StatefulSet.class);

    // Build a StatefulSet starting from a base StatefulSet
    var expectedStatefulSet = new StatefulSetBuilder(default095StatefulSet)
        .withNewMetadata()
        .withName(CLUSTER_NAME)
        .withNamespace(CLUSTER_NAMESPACE)
        .withAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .endMetadata()
        .withSpec(new StatefulSetSpecBuilder(default095StatefulSet.getSpec())
            .withReplicas(2)
            .withServiceName(CLUSTER_NAME)
            .withNewSelector()
            .withMatchLabels(Map.of(
                "app", "StackGresCluster",
                "cluster", "true",
                "cluster-uid", CLUSTER_UID,
                "cluster-name", CLUSTER_NAME,
                "disruptible", "true"
            ))
            .endSelector()
            .withTemplate(new PodTemplateSpecBuilder(default095StatefulSet.getSpec().getTemplate())
                .withNewMetadata()
                .withAnnotations(Map.of(
                    "allResourceAnnotation", "allResourceValue",
                    "podsAnnotation", "podsAnnotationValue",
                    "stackgres.io/operatorVersion", "0.9.5"
                ))
                .withLabels(Map.of(
                    "app", "StackGresCluster",
                    "cluster", "true",
                    "cluster-uid", CLUSTER_UID,
                    "cluster-name", CLUSTER_NAME,
                    "disruptible", "true"
                ))
                .endMetadata()
                .withSpec(
                    new PodSpecBuilder(default095StatefulSet.getSpec().getTemplate().getSpec())
                        .withServiceAccountName(CLUSTER_NAME + "-patroni")
                        .build())
                .build())
            .withVolumeClaimTemplates(List.of(
                new PersistentVolumeClaimBuilder()
                    .withNewMetadata()
                    .withAnnotations(Map.of(
                        "allResourceAnnotation", "allResourceValue"
                    ))
                    .withLabels(Map.of(
                        "cluster-uid", CLUSTER_UID,
                        "cluster-name", CLUSTER_NAME,
                        "app", "StackGresCluster"
                    ))
                    .withName(CLUSTER_NAME + "-data")
                    .withNamespace(CLUSTER_NAMESPACE)
                    .endMetadata()
                    .withNewSpec()
                    .withAccessModes(List.of("ReadWriteOnce"))
                    .withNewResources()
                    .withRequests(Map.of(
                        "storage", new Quantity("500Mi")
                    ))
                    .endResources()
                    .endSpec()
                    .build()
            ))
            .build())
        .build();

    /*
     * Replace containers environment variables that use the cluster name,
     * with the generated cluster name
     */
    expectedStatefulSet.getSpec().getTemplate().getSpec().getContainers().stream()
        .filter(c -> c.getEnv() != null)
        .flatMap(c -> c.getEnv().stream())
        .peek(envVar -> {
          if (envVar.getName().equals("PG_EXPORTER_CONSTANT_LABELS")) {
            envVar.setValue("cluster_name=" + CLUSTER_NAME
                + ", namespace=" + CLUSTER_NAMESPACE);
          }
        })
        .filter(envVar -> envVar.getValueFrom() != null)
        .forEach(envVar -> {
          if (envVar.getValueFrom().getConfigMapKeyRef() != null
              && envVar.getValueFrom().getConfigMapKeyRef().getName().equals("test")) {
            envVar.getValueFrom().getConfigMapKeyRef().setName(CLUSTER_NAME);
          }
          if (envVar.getValueFrom().getSecretKeyRef() != null
              && envVar.getValueFrom().getSecretKeyRef().getName().equals("test")) {
            envVar.getValueFrom().getSecretKeyRef().setName(CLUSTER_NAME);
          }
        });

    /*
     * Replace init containers environment variables that use the cluster name,
     * with the generated cluster name
     */
    expectedStatefulSet.getSpec().getTemplate().getSpec().getContainers().stream()
        .filter(c -> c.getEnvFrom() != null)
        .flatMap(c -> c.getEnvFrom().stream())
        .forEach(envVarFrom -> {
          if (envVarFrom.getConfigMapRef() != null
              && envVarFrom.getConfigMapRef().getName().equals("test")) {
            envVarFrom.getConfigMapRef().setName(CLUSTER_NAME);
          }
          if (envVarFrom.getSecretRef() != null
              && envVarFrom.getSecretRef().getName().equals("test")) {
            envVarFrom.getSecretRef().setName(CLUSTER_NAME);
          }

        });

    /*
     * replace volume mounts names whose names are generated based in the cluster name with
     * the generated cluster name
     */
    expectedStatefulSet.getSpec().getTemplate().getSpec().getContainers().stream()
        .filter(c -> c.getVolumeMounts() != null)
        .flatMap(c -> c.getVolumeMounts().stream())
        .filter(vm -> vm.getName().startsWith("test-"))
        .forEach(vm -> vm.setName(ResourceUtil.cutVolumeName(vm.getName().replaceAll("^test-",
            CLUSTER_NAME + "-"))));

    /*
     * replace volumes names and configmaps references whose name cluster name based,
     * with the generated cluster name
     */
    expectedStatefulSet.getSpec().getTemplate().getSpec().getVolumes()
        .stream()
        .peek(v -> v.setName(
            ResourceUtil.cutVolumeName(
                v.getName().replaceAll("test-", CLUSTER_NAME + "-")
            )
        ))
        .map(Volume::getConfigMap)
        .filter(Objects::nonNull)
        .peek(cm -> {
          if (cm.getName().equals("test")) {
            cm.setName(CLUSTER_NAME);
          }
        })
        .filter(cm -> cm.getName().startsWith("test-"))
        .forEach(cm -> cm.setName(
            ResourceUtil.cutVolumeName(
                cm.getName().replaceAll("^test-", CLUSTER_NAME + "-")
            )
        ));

    /*
     * replace volumes secret references with whose names are cluster name based with
     * the generated cluster name
     */
    expectedStatefulSet.getSpec().getTemplate().getSpec().getVolumes()
        .stream()
        .map(Volume::getSecret)
        .filter(Objects::nonNull)
        .peek(s -> s.setSecretName(s.getSecretName().replaceAll("^test$", CLUSTER_NAME)))
        .filter(s -> s.getSecretName().startsWith("test-"))
        .forEach(v -> v.setSecretName(
            v.getSecretName().replaceAll("^test-", CLUSTER_NAME + "-")
        ));

    /*
     * replace init containers volume mount names that are cluster name based with
     * the generated cluster name
     */
    expectedStatefulSet.getSpec().getTemplate().getSpec().getInitContainers().stream()
        .filter(c -> c.getVolumeMounts() != null)
        .flatMap(c -> c.getVolumeMounts().stream())
        .forEach(vm -> vm.setName(vm.getName().replaceAll("^test-",
            CLUSTER_NAME + "-")));

    return expectedStatefulSet;
  }

}
