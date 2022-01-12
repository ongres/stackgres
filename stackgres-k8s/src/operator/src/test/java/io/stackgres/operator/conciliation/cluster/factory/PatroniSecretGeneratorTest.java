/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.factory;

import java.util.Map;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PatroniSecretGeneratorTest extends GeneratorTest {

  @Test
  @DisplayName("Test 0.9.5 Patroni Secret")
  void testSecretWithV095() {

    var expectedSecret = new SecretBuilder()
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
        .withData(Map.of(
            "authenticator-password", "ZGE3YS1iMGZhLTQ5YjYtOTQ3",
            "replication-password", "MGI4MC1iYjI1LTRkMWMtYjQ5",
            "restapi-password", "ZWJlNS0wMjFiLTRjMjAtOGVj",
            "superuser-password", "M2ExNC1mZTEzLTQ5NWUtOTcy"
        ))
        .withType("Opaque")
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .andDatabaseCredentials(new SecretBuilder()
            .withData(Map.of(
                "authenticator-password", "ZGE3YS1iMGZhLTQ5YjYtOTQ3",
                "replication-password", "MGI4MC1iYjI1LTRkMWMtYjQ5",
                "restapi-password", "ZWJlNS0wMjFiLTRjMjAtOGVj",
                "superuser-password", "M2ExNC1mZTEzLTQ5NWUtOTcy"
            ))
            .build())
        .generatedResourceShouldBeEqualTo(expectedSecret);
  }

}
