/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

abstract class AbstractDependencyCustomResourceTest
    <T extends ResourceDto, R extends CustomResource<?, ?>,
    S extends AbstractCustomResourceServiceDependency<T, R>,
    N extends AbstractNamespacedRestServiceDependency<T, R>> {

  @Mock
  protected CustomResourceScanner<R> scanner;

  @Mock
  protected CustomResourceScanner<StackGresCluster> clusterScanner;

  @Mock
  protected CustomResourceFinder<R> finder;

  @Mock
  protected CustomResourceScheduler<R> scheduler;

  protected DefaultKubernetesResourceList<R> customResources;
  protected T resourceDto;
  protected StackGresClusterList clusters;
  protected S service;
  protected N namespacedService;
  protected AbstractDependencyResourceTransformer<T, R> transformer;

  @BeforeEach
  void setUp() {
    customResources = getCustomResourceList();
    resourceDto = getResourceDto();
    clusters = getClusterList();

    transformer = getTransformer();
    service = getService();
    service.scheduler = scheduler;
    service.clusterScanner = clusterScanner;
    service.scanner = scanner;
    service.finder = finder;
    service.transformer = transformer;
    namespacedService = getNamespacedService();
    namespacedService.finder = finder;
    namespacedService.clusterScanner = clusterScanner;
    namespacedService.transformer = transformer;
  }

  @Test
  void listShouldReturnAllDtos() {
    when(clusterScanner.getResources()).thenReturn(clusters.getItems());
    when(scanner.getResources()).thenReturn(customResources.getItems());

    List<T> resources = service.list();

    assertEquals(customResources.getItems().size(), resources.size());
    checkDto(resources.getFirst());
  }

  @Test
  void getOfAnExistingDtoShouldReturnTheExistingDto() {
    when(clusterScanner.getResources()).thenReturn(clusters.getItems());
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));

    T dto = namespacedService.get(getResourceNamespace(), getResourceName());

    checkDto(dto);
  }

  @Test
  void createShouldNotFail() {
    doAnswer(invocation -> {
      R customResource = invocation.getArgument(0);

      checkCustomResource(customResource, Operation.CREATE);

      return transformer.toCustomResource(resourceDto, null);
    }).when(scheduler).create(any(), anyBoolean());

    service.create(resourceDto, false);
  }

  @Test
  void updateShouldNotFail() {
    when(finder.findByNameAndNamespace(anyString(), anyString())).thenReturn(
        customResources.getItems().stream().findFirst());

    doAnswer(invocation -> {
      R customResource = invocation.getArgument(0);

      checkCustomResource(customResource, Operation.UPDATE);

      return transformer.toCustomResource(resourceDto, null);
    }).when(scheduler).update(any(), any());

    service.update(resourceDto, false);
  }

  @Test
  void deleteShouldNotFail() {
    doAnswer((Answer<Void>) invocation -> {
      R customResource = invocation.getArgument(0);

      checkCustomResource(customResource, Operation.DELETE);

      return null;
    }).when(scheduler).delete(any(), anyBoolean());

    service.delete(resourceDto, false);
  }

  protected abstract DefaultKubernetesResourceList<R> getCustomResourceList();

  protected abstract T getResourceDto();

  private StackGresClusterList getClusterList() {
    return Fixtures.clusterList().loadDefault().get();
  }

  protected abstract AbstractDependencyResourceTransformer<T, R> getTransformer();

  protected abstract S getService();

  protected abstract N getNamespacedService();

  protected abstract String getResourceNamespace();

  protected abstract String getResourceName();

  protected abstract void checkDto(T resource);

  protected abstract void checkCustomResource(R resource, Operation operation);

  enum Operation {
    CREATE, UPDATE, DELETE;
  }
}
