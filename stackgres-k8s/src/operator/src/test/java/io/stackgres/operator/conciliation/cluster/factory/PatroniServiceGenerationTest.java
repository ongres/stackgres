/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.factory;

import java.util.Map;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PatroniServiceGenerationTest extends GeneratorTest {

  @Test
  @DisplayName("Test primary service for 0.9.5")
  void testPrimaryServiceForV095() {

    Service expectedService = new ServiceBuilder()
        .withNewMetadata()
        .withAnnotations(
            Map.of(
                "primaryServiceAnnotation", "primaryServiceValue",
                "allResourceAnnotation", "allResourceValue",
                "servicesAnnotations", "servicesAnnotationsValue"
            )
        )
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .withNamespace(CLUSTER_NAMESPACE)
        .withName(CLUSTER_NAME + "-primary")
        .endMetadata()
        .withNewSpec()
        .withExternalName(CLUSTER_NAME + "." + CLUSTER_NAMESPACE + StackGresUtil.domainSearchPath())
        .withType("ExternalName")
        .endSpec()
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .andPrimaryServiceAnnotations(Map.of("primaryServiceAnnotation", "primaryServiceValue"))
        .andServiceAnnotations(Map.of("servicesAnnotations", "servicesAnnotationsValue"))
        .generatedResourceShouldBeEqualTo(expectedService);
  }

  @Test
  @DisplayName("Test config service for 0.9.5")
  void testConfigServiceForV095() {
    Service expectedService = new ServiceBuilder()
        .withNewMetadata()
        .withAnnotations(
            Map.of(
                "allResourceAnnotation", "allResourceValue",
                "servicesAnnotations", "servicesAnnotationsValue"
            )
        )
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .withNamespace(CLUSTER_NAMESPACE)
        .withName(CLUSTER_NAME + "-config")
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .andPrimaryServiceAnnotations(Map.of("primaryServiceAnnotation", "primaryServiceValue"))
        .andServiceAnnotations(Map.of("servicesAnnotations", "servicesAnnotationsValue"))
        .generatedResourceShouldBeEqualTo(expectedService);
  }

  @Test
  @DisplayName("Test rest service for 0.9.5")
  void testRestServiceForV095() {
    Service expectedService = new ServiceBuilder()
        .withNewMetadata()
        .withAnnotations(
            Map.of(
                "allResourceAnnotation", "allResourceValue",
                "servicesAnnotations", "servicesAnnotationsValue"
            )
        )
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .withNamespace(CLUSTER_NAMESPACE)
        .withName(CLUSTER_NAME + "-rest")
        .endMetadata()
        .withNewSpec()
        .withPorts(new ServicePortBuilder()
            .withName("patroniport")
            .withPort(8008)
            .withProtocol("TCP")
            .withTargetPort(new IntOrString("patroniport"))
            .build())
        .withSelector(Map.of(
            "cluster", "true",
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .withType("ClusterIP")
        .endSpec()
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .andPrimaryServiceAnnotations(Map.of("primaryServiceAnnotation", "primaryServiceValue"))
        .andServiceAnnotations(Map.of("servicesAnnotations", "servicesAnnotationsValue"))
        .generatedResourceShouldBeEqualTo(expectedService);
  }

}
