/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.dto.Cluster;
import io.stackgres.operator.resource.dto.ClusterStatus;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class CachedClusterScannerTest {

  private StackGresClusterList clusterList = JsonUtil.readFromJson("stackgres_cluster/list.json",
      StackGresClusterList.class);

  private ClusterStatus status = JsonUtil.readFromJson("stackgres_cluster/status.json",
      ClusterStatus.class);

  private CachedClusterScanner clusterScanner;

  @Mock
  KubernetesResourceScanner<StackGresClusterList> directScanner;

  @Mock
  KubernetesCustomResourceFinder<ClusterStatus> statusFinder;

  @BeforeEach
  void setUp() {

    clusterScanner = new CachedClusterScanner(directScanner, statusFinder);

  }

  @Test
  void onStart_shouldCallTheDirectClusterOnStartUp() {

    when(directScanner.findResources()).thenReturn(Optional.of(clusterList));
    when(directScanner.findResources(anyString())).thenReturn(Optional.of(clusterList));

    when(statusFinder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(status));

    clusterScanner.onStart(null);

    verify(directScanner).findResources();
    verify(directScanner, atLeastOnce()).findResources(anyString());
    verify(statusFinder, atLeastOnce()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void findResources_ShouldNotCallTheDirectScanner() {

    clusterScanner.findResources();

    verify(directScanner, never()).findResources();
    verify(directScanner, never()).findResources(anyString());

  }

  @Test
  void findResources_ShouldReturnNothingIfTheCacheIsCold() {

    Optional<List<Cluster>> resources = clusterScanner.findResources();
    assertFalse(resources.isPresent());

  }

  @Test
  void findResources_ShouldReturnSomethingIfTheCacheIsHot() {

    when(directScanner.findResources()).thenReturn(Optional.of(clusterList));
    when(directScanner.findResources(anyString())).thenReturn(Optional.of(clusterList));

    clusterScanner.onStart(null);

    Optional<List<Cluster>> resources = clusterScanner.findResources();

    assertTrue(resources.isPresent());

  }

  @Test
  void findResources_IfAClusterIsDeletedItShouldNotAppearInTheCacheAnymore() {


    StackGresClusterList emptyList = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class);
    emptyList.setItems(new ArrayList<>());


    when(directScanner.findResources()).thenReturn(Optional.of(clusterList))
        .thenReturn(Optional.of(emptyList));
    when(directScanner.findResources(anyString()))
        .thenReturn(Optional.of(clusterList));


    clusterScanner.onStart(null);

    Optional<List<Cluster>> resources = clusterScanner.findResources();

    assertTrue(resources.isPresent());

    resources.ifPresent(clusters -> assertEquals(clusterList.getItems().size(), clusters.size()));

    clusterScanner.refreshCache();

    resources = clusterScanner.findResources();

    resources.ifPresent(clusters -> assertTrue(clusters.isEmpty()));


  }

  @Test
  void testFindResources_ShouldNotCallTheDirectScanner() {

    clusterScanner.findResources(clusterList.getItems().get(0).getMetadata().getNamespace());

    verify(directScanner, never()).findResources();
    verify(directScanner, never()).findResources(anyString());

  }

  @Test
  void testFindResources_ShouldReturnNothingIfTheCacheIsCold() {

    Optional<List<Cluster>> resources = clusterScanner
        .findResources(clusterList.getItems().get(0).getMetadata().getNamespace());

    assertFalse(resources.isPresent());

  }

  @Test
  void testFindResources_ShouldReturnSomethingIfTheCacheIsHot() {

    when(directScanner.findResources()).thenReturn(Optional.of(clusterList));
    when(directScanner.findResources(anyString())).thenReturn(Optional.of(clusterList));

    clusterScanner.onStart(null);

    Optional<List<Cluster>> resources = clusterScanner
        .findResources(clusterList.getItems().get(0).getMetadata().getNamespace());

    assertTrue(resources.isPresent());
  }
}