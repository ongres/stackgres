/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer
@QuarkusTest
class PatroniApiMetadataFinderTest {

  @Inject
  KubernetesClient client;

  @Inject
  PatroniCtlFinder patroniApiFinder;

  String clusterName;
  String namespace;
  StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    clusterName = StringUtils.getRandomClusterName();
    namespace = StringUtils.getRandomNamespace();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(namespace);

    client.resource(cluster)
        .create();
  }

  @Test
  void givenAValidClusterAndNamespace_shouldBeAbleToReturnThePatroniCtl() {
    var cluster =
        patroniApiFinder.findCluster(clusterName, namespace);
    assertEquals(this.cluster, cluster);
  }

  @Test
  void givenABadClusterName_shouldThrowAnException() {
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.findCluster(StringUtils.getRandomClusterName(), namespace));
    assertEquals("Can not find SGCluster", ex.getMessage());
  }

  @Test
  void givenABadClusterNamespace_shouldThrowAnException() {
    var ex = assertThrows(RuntimeException.class,
        () -> patroniApiFinder.findCluster(clusterName, StringUtils.getRandomClusterName()));
    assertEquals("Can not find SGCluster", ex.getMessage());
  }

}
