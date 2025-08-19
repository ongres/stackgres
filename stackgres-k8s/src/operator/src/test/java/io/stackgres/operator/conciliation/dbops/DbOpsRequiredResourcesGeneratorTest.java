/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtlInstance;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.ProfileFinder;
import io.stackgres.common.resource.ResourceScanner;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class DbOpsRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ProfileFinder profileFinder;

  @InjectMock
  ResourceScanner<Pod> podScanner;

  @InjectMock
  PatroniCtl patorniCtl;

  PatroniCtlInstance patroniCtlInstance;

  @Inject
  DbOpsRequiredResourcesGenerator generator;

  private StackGresConfig config;
  private StackGresDbOps dbOps;
  private StackGresCluster cluster;
  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    dbOps = Fixtures.dbOps().loadRestart().get();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().getLatestVersion());
    cluster.getMetadata().setNamespace(dbOps.getMetadata().getNamespace());
    cluster.getMetadata().setName(dbOps.getSpec().getSgCluster());
    profile = Fixtures.instanceProfile().loadSizeS().get();
    patroniCtlInstance = Mockito.mock(PatroniCtlInstance.class);
  }

  @Test
  void givenValidDbOps_shouldPass() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    when(patorniCtl.instanceFor(any()))
        .thenReturn(patroniCtlInstance);

    generator.getRequiredResources(dbOps);
  }

}
