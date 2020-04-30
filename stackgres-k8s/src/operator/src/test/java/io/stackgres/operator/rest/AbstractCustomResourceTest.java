/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.ResourceDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

abstract class AbstractCustomResourceTest<T extends ResourceDto, R extends CustomResource,
    S extends AbstractRestService<T, R>> {

  @Mock
  protected CustomResourceScanner<R> scanner;

  @Mock
  protected CustomResourceFinder<R> finder;

  @Mock
  protected CustomResourceScheduler<R> scheduler;

  protected CustomResourceList<R> customResources;
  protected T resourceDto;
  protected S service;
  protected AbstractResourceTransformer<T, R> transformer;

  @BeforeEach
  void setUp() {
    customResources = getCustomResourceList();
    resourceDto = getResourceDto();

    transformer = getTransformer();
    service = getService(scanner, finder, scheduler, transformer);
  }

  @Test
  void listShouldReturnAllDtos() {
    when(scanner.getResources()).thenReturn(customResources.getItems());

    List<T> resources = service.list();

    assertEquals(1, resources.size());
    checkDto(resources.get(0));
  }

  @Test
  void getOfAnExistingDtoShouldReturnTheExistingDto() {
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));

    T dto = service.get(getResourceNamespace(), getResourceName());

    checkDto(dto);
  }

  @Test
  void createShouldNotFail() {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        R customResource = invocation.getArgument(0);

        checkCustomResource(customResource, Operation.CREATE);

        return null;
      }
    }).when(scheduler).create(any());

    service.create(resourceDto);
  }

  @Test
  void updateShouldNotFail() {
    when(finder.findByNameAndNamespace(anyString(), anyString())).thenReturn(
        customResources.getItems().stream().findFirst());

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        R customResource = invocation.getArgument(0);

        checkCustomResource(customResource, Operation.UPDATE);

        return null;
      }
    }).when(scheduler).update(any());

    service.update(resourceDto);
  }

  @Test
  void deleteShouldNotFail() {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        R customResource = invocation.getArgument(0);

        checkCustomResource(customResource, Operation.DELETE);

        return null;
      }
    }).when(scheduler).delete(any());

    service.delete(resourceDto);
  }

  protected abstract CustomResourceList<R> getCustomResourceList();

  protected abstract T getResourceDto();

  protected abstract AbstractResourceTransformer<T, R> getTransformer();

  protected abstract S getService(
      CustomResourceScanner<R> scanner,
      CustomResourceFinder<R> finder,
      CustomResourceScheduler<R> scheduler,
      AbstractResourceTransformer<T, R> transformer);

  protected abstract String getResourceNamespace();

  protected abstract String getResourceName();

  protected abstract void checkDto(T resource);

  protected abstract void checkCustomResource(R resource, Operation operation);

  enum Operation {
    CREATE, UPDATE, DELETE;
  }
}