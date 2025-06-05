/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ClusterPostgresVersionResourceTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  @InjectMock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  private StackGresCluster cluster;

  @BeforeEach
  public void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));
    mockServer.getClient().resource(cluster).create();
  }

  @Test
  void ifClusterUsesLatestVersion_itShouldReturnTheArrayOfLatestVersions() {
    List<String> versions = StackGresUtil.getPostgresFlavorComponent(cluster)
        .get(cluster)
        .streamOrderedVersions()
        .toList();
    given()
        .when()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/namespaces/test-namespace/sgclusters/test/version/postgresql")
        .then().statusCode(200)
        .body(
            "",
            Matchers.hasSize(versions.size()));
  }

}
