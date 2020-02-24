/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterPodConfigDto;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operator.rest.transformer.ClusterTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterResourceTest {

  @Mock
  private CustomResourceFinder<StackGresCluster> finder;

  @Mock
  private CustomResourceScanner<StackGresCluster> scanner;

  @Mock
  private CustomResourceScheduler<StackGresCluster> scheduler;

  @Mock
  private CustomResourceFinder<ClusterResourceConsumtionDto> statusFinder;
  
  @Mock
  private CustomResourceFinder<ClusterPodConfigDto> detailsFinder;

  private StackGresClusterList clusters;

  private ClusterDto clusterDto;

  private ClusterResource resource;

  @BeforeEach
  void setUp() {
    clusters = JsonUtil
        .readFromJson("stackgres_cluster/list.json", StackGresClusterList.class);
    clusterDto = JsonUtil
        .readFromJson("stackgres_cluster/dto.json", ClusterDto.class);

    resource = new ClusterResource(scanner, finder, scheduler, new ClusterTransformer(),
        statusFinder, detailsFinder);
  }

  @Test
  void listShouldReturnAllClusters() {
    when(scanner.getResources()).thenReturn(clusters.getItems());

    List<ClusterDto> clusters = resource.list();

    assertEquals(1, clusters.size());

    assertNotNull(clusters.get(0).getMetadata());

    assertEquals("postgresql", clusters.get(0).getMetadata().getNamespace());

    assertEquals("stackgres", clusters.get(0).getMetadata().getName());
  }

  @Test
  void getOfAnExistingClusterShouldReturnTheExistingCluster() {
    when(finder.findByNameAndNamespace("stackgres", "postgresql"))
        .thenReturn(Optional.of(clusters.getItems().get(0)));

    ClusterDto cluster = resource.get("postgresql", "stackgres");

    assertNotNull(cluster.getMetadata());

    assertEquals("postgresql", cluster.getMetadata().getNamespace());

    assertEquals("stackgres", cluster.getMetadata().getName());
  }

  @Test
  void createShouldNotFail() {
    resource.create(clusterDto);
  }

  @Test
  void updateShouldNotFail() {
    resource.update(clusterDto);
  }

  @Test
  void deleteShouldNotFail() {
    resource.delete(clusterDto);
  }

}