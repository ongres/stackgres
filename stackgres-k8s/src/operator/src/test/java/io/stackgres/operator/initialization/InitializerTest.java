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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class InitializerTest<T extends CustomResource> {

  @Mock
  private CustomResourceScheduler<T> customResourceScheduler;

  @Mock
  private DefaultCustomResourceFactory<T> resourceFactory;

  @Mock
  private DefaultFactoryProvider<DefaultCustomResourceFactory<T>> factoryProvider;

  @Mock
  private CustomResourceScanner<T> resourceScanner;

  private AbstractDefaultCustomResourceInitializer<T> initializer;

  private T defaultCustomResource;

  private String resourceNamespace;

  @BeforeEach
  void init() {
    initializer = getInstance();
    initializer.setResourceScheduler(customResourceScheduler);
    when(factoryProvider.getFactories()).thenReturn(Collections.singletonList(resourceFactory));
    initializer.setFactoryProvider(factoryProvider);
    initializer.setResourceScanner(resourceScanner);
    defaultCustomResource = configureDefaultCR();
    resourceNamespace = defaultCustomResource.getMetadata().getNamespace();
  }

  abstract AbstractDefaultCustomResourceInitializer<T> getInstance();

  abstract T getDefaultCR();

  private T configureDefaultCR() {
    T defaultCustomResource = getDefaultCR();
    String name = DefaultCustomResourceFactory.DEFAULT_RESOURCE_NAME_PREFIX
        + System.currentTimeMillis();
    defaultCustomResource.getMetadata().setName(name);
    Map<String, String> annotations = new HashMap<>();
    annotations.put(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString());
    defaultCustomResource.getMetadata().setAnnotations(annotations);
    return defaultCustomResource;
  }

  @Test
  void givenNoResourceCreated_itShouldCreateANewOne() {

    T defaultCustomResource = configureDefaultCR();

    when(resourceScanner.getResources(resourceNamespace))
        .thenReturn(new ArrayList<>());

    when(resourceFactory.buildResource()).thenReturn(defaultCustomResource);


    doNothing().when(customResourceScheduler).create(defaultCustomResource);

    initializer.initialize();

    verify(resourceScanner).getResources(anyString());
    verify(resourceFactory).buildResource();
    verify(customResourceScheduler).create(defaultCustomResource);

  }

  @Test
  void givenAResourceAlreadyCreated_itShouldDoNothing() {

    when(resourceFactory.getDefaultPrefix()).thenCallRealMethod();
    when(resourceScanner.getResources(resourceNamespace))
        .thenReturn(Collections.singletonList(defaultCustomResource));

    when(resourceFactory.buildResource()).thenReturn(defaultCustomResource);

    initializer.initialize();

    verify(resourceScanner).getResources(anyString());
    verify(resourceFactory).buildResource();
    verify(customResourceScheduler, never()).create(any());
  }
}
