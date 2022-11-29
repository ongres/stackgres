/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

public abstract class AbstractCustomResourceTest
      <T extends ResourceDto, R extends CustomResource<?, ?>,
      S extends AbstractRestService<T, R>, N extends AbstractNamespacedRestService<T, R>> {

  @Mock
  protected CustomResourceScanner<R> scanner;

  @Mock
  protected CustomResourceFinder<R> finder;

  @Mock
  protected CustomResourceScheduler<R> scheduler;

  protected DefaultKubernetesResourceList<R> customResources;
  protected T dto;
  protected S service;
  protected N namespacedService;
  protected AbstractResourceTransformer<T, R> transformer;

  @BeforeEach
  void setUp() {
    customResources = getCustomResourceList();
    dto = getDto();

    transformer = getTransformer();
    service = getService();
    service.finder = finder;
    service.scanner = scanner;
    service.scheduler = scheduler;
    service.transformer = transformer;
    namespacedService = getNamespacedService();
    namespacedService.finder = finder;
    namespacedService.transformer = transformer;
  }

  @Test
  void listShouldReturnAllDtos() {
    when(scanner.getResources()).thenReturn(customResources.getItems());

    List<T> resources = service.list();

    assertEquals(customResources.getItems().size(), resources.size());

    Seq.zip(customResources.getItems(), resources).forEach(tuple -> {
      checkDto(tuple.v2, tuple.v1);
    });
  }

  @Test
  void getOfAnExistingDtoShouldReturnTheExistingDto() {
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));

    T dto = namespacedService.get(getResourceNamespace(), getResourceName());

    checkDto(dto, customResources.getItems().get(0));
  }

  @Test
  void createShouldNotFail() {
    doAnswer((Answer<Void>) invocation -> {
      R customResource = invocation.getArgument(0);

      checkCustomResource(customResource, dto, Operation.CREATE);

      return null;
    }).when(scheduler).create(any());

    service.create(dto);
  }

  @Test
  void updateShouldNotFail() {
    when(finder.findByNameAndNamespace(anyString(), anyString())).thenReturn(
        customResources.getItems().stream().findFirst());

    doAnswer((Answer<Void>) invocation -> {
      R customResource = invocation.getArgument(0);

      checkCustomResource(customResource, dto, Operation.UPDATE);

      return null;
    }).when(scheduler).update(any(), any());

    service.update(dto);
  }

  @Test
  void deleteShouldNotFail() {
    doAnswer((Answer<Void>) invocation -> {
      R customResource = invocation.getArgument(0);

      checkCustomResource(customResource, dto, Operation.DELETE);

      return null;
    }).when(scheduler).delete(any());

    service.delete(dto);
  }

  protected abstract DefaultKubernetesResourceList<R> getCustomResourceList();

  protected abstract T getDto();

  protected abstract AbstractResourceTransformer<T, R> getTransformer();

  protected abstract S getService();

  protected abstract N getNamespacedService();

  protected abstract String getResourceNamespace();

  protected abstract String getResourceName();

  protected abstract void checkDto(T dto, R resource);

  protected abstract void checkCustomResource(R resource, T resourceDto, Operation operation);

  enum Operation {
    CREATE, UPDATE, DELETE;
  }
}
