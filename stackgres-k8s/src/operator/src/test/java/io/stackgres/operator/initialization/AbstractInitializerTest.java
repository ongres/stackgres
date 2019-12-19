/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

public abstract class AbstractInitializerTest<T extends CustomResource> {

  @Mock
  private KubernetesCustomResourceFinder<T> resourceFinder;

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
    initializer = getInstance();
    initializer.setResourceFinder(resourceFinder);
    initializer.setResourceScheduler(resourceScheduler);
    initializer.setResourceFactory(resourceFactory);
    initializer.setQueue(queue);
    defaultCustomResource = getDefaultCR();
    resourceName = defaultCustomResource.getMetadata().getName();
    resourceNamespace = defaultCustomResource.getMetadata().getNamespace();
  }

  abstract AbstractDefaultCustomResourceInitializer<T> getInstance();

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
