/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomResourceInitializer<T extends CustomResource>
    implements DefaultCustomResourceInitializer<T> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CustomResourceInitializer.class);

  private final CustomResourceScheduler<T> customResourceScheduler;
  private final DefaultCustomResourceFactory<T> factory;
  private final CustomResourceScanner<T> resourceScanner;

  public CustomResourceInitializer(CustomResourceScheduler<T> customResourceScheduler,
                                   DefaultCustomResourceFactory<T> factory,
                                   CustomResourceScanner<T> resourceScanner) {
    this.customResourceScheduler = customResourceScheduler;
    this.factory = factory;
    this.resourceScanner = resourceScanner;
  }

  @Override
  public void initialize() {
    T defaultResource = factory.buildResource();
    String resourceNamespace = defaultResource.getMetadata().getNamespace();
    String resourceName = defaultResource.getMetadata().getName();

    LOGGER.info("Initializing " + resourceName);

    List<T> installedResources = resourceScanner.getResources(resourceNamespace);

    Optional<T> installedDefaultResources = installedResources
        .stream()
        .filter(i -> i.getMetadata().getName()
            .startsWith(factory.getDefaultPrefix()))
        .filter(i -> Optional
            .ofNullable(i.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
            .map(StackGresProperty.OPERATOR_VERSION.getString()::equals)
            .orElse(false))
        .findFirst();

    if (installedDefaultResources.isPresent()) {
      LOGGER.info("Default custom resource " + resourceName + " already installed");
    } else {
      LOGGER.info("Installing default custom resource " + resourceName);
      customResourceScheduler.create(defaultResource);
    }
  }
}
