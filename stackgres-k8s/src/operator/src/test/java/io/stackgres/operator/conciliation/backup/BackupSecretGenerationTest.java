/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Map;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class BackupSecretGenerationTest extends GeneratorTest {

  @Test
  @DisplayName("Test backup secret without backup configuration for 0.9.5")
  void testBackupSecretForV095() {
    var expectedSecret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(CLUSTER_NAMESPACE)
        .withName(CLUSTER_NAME + "-backup")
        .withAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .endMetadata()
        .withStringData(Map.of(
            "MD5SUM", "D41D8CD98F00B204E9800998ECF8427E"
        ))
        .withType("Opaque")
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .generatedResourceShouldBeEqualTo(expectedSecret);
  }
}
