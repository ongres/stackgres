/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class AbstractInitializerTest<T extends CustomResource> {

  @Mock
  private CustomResourceFinder<T> resourceFinder;

  @Mock
  private CustomResourceScheduler<T> resourceScheduler;

  @Mock
  private DefaultCustomResourceFactory<T> resourceFactory;

  private InitializationQueue queue = Runnable::run;

  private AbstractDefaultCustomResourceInitializer<T> initializer;

  private T defaultCustomResource;

  private String resourceName;
  private String resourceNamespace;

  @BeforeEach
  void init() {
    initializer = getInstance(resourceFinder, resourceScheduler, resourceFactory, queue);
    defaultCustomResource = getDefaultCR();
    resourceName = defaultCustomResource.getMetadata().getName();
    resourceNamespace = defaultCustomResource.getMetadata().getNamespace();
  }

  abstract AbstractDefaultCustomResourceInitializer<T> getInstance(
      CustomResourceFinder<T> resourceFinder, CustomResourceScheduler<T> resourceScheduler,
      DefaultCustomResourceFactory<T> resourceFactory, InitializationQueue queue);

  abstract T getDefaultCR();

  @Test
  void givenNoResourceCreated_itShouldCreateANewOne() {

    T defaultCustomResource = getDefaultCR();

    when(resourceFinder.findByNameAndNamespace(resourceName, resourceNamespace))
        .thenReturn(Optional.empty());

    when(resourceFactory.buildResource()).thenReturn(defaultCustomResource);


    doNothing().when(resourceScheduler).create(defaultCustomResource);

    initializer.initialize();

    verify(resourceFinder).findByNameAndNamespace(anyString(), anyString());
    verify(resourceFactory).buildResource();
    verify(resourceScheduler).create(defaultCustomResource);

  }

  @Test
  void givenAResourceAlreadyCreated_itShouldDoNothing() {

    when(resourceFinder.findByNameAndNamespace(resourceName, resourceNamespace))
        .thenReturn(Optional.of(defaultCustomResource));

    when(resourceFactory.buildResource()).thenReturn(defaultCustomResource);

    initializer.initialize();

    verify(resourceFinder).findByNameAndNamespace(anyString(), anyString());
    verify(resourceFactory).buildResource();
    verify(resourceScheduler, never()).create(any());
  }
}
