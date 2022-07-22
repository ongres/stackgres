/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfiguration;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.DefaultComparator;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DistributedLogsRequiredResourcesGeneratorTest {

  @Inject
  DistributedLogsRequiredResourcesGenerator generator;

  @InjectMock
  PostgresConfigFinder postgresConfigFinder;

  @InjectMock
  ProfileConfigFinder profileConfigFinder;

  @InjectMock
  ConnectedClustersScannerImpl clusterScanner;

  @Inject
  @ReconciliationScope(value = StackGresDistributedLogs.class, kind = "StatefulSet")
  DistributedLogsStatefulSetComparator stsComparator;

  DefaultComparator configMapComparator = new DefaultComparator();

  List<StackGresCluster> connectedClusters;

  private StackGresDistributedLogs distributedLogs;
  private StackGresPostgresConfig postgresConfig;
  private StackGresProfile instanceProfile;

  String randomNamespace = StringUtils.getRandomNamespace();
  String randomName = StringUtils.getRandomClusterName();
  String clusterUid = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    randomNamespace = StringUtils.getRandomNamespace();
    randomName = StringUtils.getRandomClusterName();
    clusterUid = UUID.randomUUID().toString();
    connectedClusters = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class)
        .getItems();
    connectedClusters.forEach(c -> {
      c.getMetadata().setName(StringUtils.getRandomClusterName());
      c.getMetadata().setNamespace(randomNamespace);
    });

    lenient().when(clusterScanner.getConnectedClusters(any())).thenReturn(connectedClusters);

    distributedLogs = JsonUtil.readFromJson("distributedlogs/default.json",
        StackGresDistributedLogs.class);
    final String namespace = distributedLogs.getMetadata().getNamespace();

    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
    postgresConfig.getSpec()
        .setPostgresVersion(StackGresComponent.POSTGRESQL.getLatest()
            .getLatestMajorVersion());
    postgresConfig.getMetadata().setNamespace(namespace);

    instanceProfile =
        JsonUtil.readFromJson("stackgres_profiles/size-s.json", StackGresProfile.class);
    instanceProfile.getMetadata().setNamespace(namespace);
  }

  @Test
  void getRequiredResources_shouldNotFail() {
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final StackGresDistributedLogsSpec distributedLogsSpec = distributedLogs.getSpec();
    final StackGresDistributedLogsConfiguration distributedLogsConfiguration =
        distributedLogsSpec.getConfiguration();
    final String postgresConfigName = distributedLogsConfiguration.getPostgresConfig();
    final String resourceProfile = distributedLogsSpec.getResourceProfile();

    when(postgresConfigFinder.findByNameAndNamespace(postgresConfigName, namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.of(instanceProfile));

    generator.getRequiredResources(distributedLogs);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, namespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, namespace);
  }

  @Test
  void givenADistributedLogsWithoutInstanceProfile_shouldFail() {
    final String distributedLogsName = distributedLogs.getMetadata().getName();
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final StackGresDistributedLogsSpec distributedLogsSpec = distributedLogs.getSpec();
    final StackGresDistributedLogsConfiguration distributedLogsConfiguration =
        distributedLogsSpec.getConfiguration();
    final String postgresConfigName = distributedLogsConfiguration.getPostgresConfig();
    final String resourceProfile = distributedLogsSpec.getResourceProfile();

    when(postgresConfigFinder.findByNameAndNamespace(postgresConfigName, namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.empty());

    assertException("SGDistributedLogs " + namespace + "."
        + distributedLogsName + " have a non existent SGInstanceProfile " + resourceProfile);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, namespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, namespace);
  }

  @Test
  void givenADistributedLogsWithoutPostgresConfig_shouldFail() {
    final String distributedLogsName = distributedLogs.getMetadata().getName();
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final StackGresDistributedLogsSpec distributedLogsSpec = distributedLogs.getSpec();
    final StackGresDistributedLogsConfiguration distributedLogsConfiguration =
        distributedLogsSpec.getConfiguration();
    final String postgresConfigName = distributedLogsConfiguration.getPostgresConfig();

    when(postgresConfigFinder.findByNameAndNamespace(postgresConfigName, namespace))
        .thenReturn(Optional.empty());

    assertException("SGDistributedLogs " + namespace + "."
        + distributedLogsName + " have a non existent SGPostgresConfig " + postgresConfigName);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, namespace);
    verify(profileConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> generator.getRequiredResources(distributedLogs));
    assertEquals(message, ex.getMessage());
  }

}
