/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

abstract class AbstractCustomResourceTest<T extends ResourceDto, R extends CustomResource> {

  @Mock
  private CustomResourceScanner<R> scanner;

  @Mock
  private CustomResourceFinder<R> finder;

  @Mock
  private CustomResourceScheduler<R> scheduler;

  private CustomResourceList<R> customResources;
  private T resourceDto;
  private AbstractRestService<T, R> service;
  private AbstractResourceTransformer<T, R> transformer;

  @BeforeEach
  void setUp() {
    customResources = getCustomResourceList();
    resourceDto = getResourceDto();

    transformer = getTransformer();
    service = getService(scanner, finder, scheduler, transformer);
  }

  @Test
  void listShouldReturnAllBackupConfigs() {
    when(scanner.getResources()).thenReturn(customResources.getItems());

    List<T> resources = service.list();

    assertEquals(1, resources.size());
    checkBackupConfig(resources.get(0));
  }

  @Test
  void getOfAnExistingBackupConfigShouldReturnTheExistingBackupConfig() {
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));

    T backupConfig = service.get(getResourceNamespace(), getResourceName());

    checkBackupConfig(backupConfig);
  }

  @Test
  void createShouldNotFail() {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        R customResource = invocation.getArgument(0);

        checkBackupConfig(customResource);

        return null;
      }
    }).when(scheduler).create(any());

    service.create(resourceDto);
  }

  @Test
  void updateShouldNotFail() {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        R customResource = invocation.getArgument(0);

        checkBackupConfig(customResource);

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

        checkBackupConfig(customResource);

        return null;
      }
    }).when(scheduler).delete(any());

    service.delete(resourceDto);
  }

  protected abstract CustomResourceList<R> getCustomResourceList();

  protected abstract T getResourceDto();

  protected abstract AbstractResourceTransformer<T, R> getTransformer();

  protected abstract AbstractRestService<T, R> getService(
      CustomResourceScanner<R> scanner,
      CustomResourceFinder<R> finder,
      CustomResourceScheduler<R> scheduler,
      AbstractResourceTransformer<T, R> transformer);

  protected abstract String getResourceNamespace();

  protected abstract String getResourceName();

  protected abstract void checkBackupConfig(T resource);

  protected abstract void checkBackupConfig(R resource);

}