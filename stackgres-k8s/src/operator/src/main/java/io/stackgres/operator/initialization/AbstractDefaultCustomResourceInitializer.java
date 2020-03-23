/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDefaultCustomResourceInitializer<T extends CustomResource>
    implements DefaultCustomResourceInitializer<T> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractDefaultCustomResourceInitializer.class);

  private CustomResourceScheduler<T> customResourceScheduler;
  private DefaultCustomResourceFactory<T> resourceFactory;
  private CustomResourceScanner<T> resourceScanner;

  @Override
  public void initialize() {
    T defaultResource = resourceFactory.buildResource();
    String resourceNamespace = defaultResource.getMetadata().getNamespace();
    String resourceName = defaultResource.getMetadata().getName();

    LOGGER.info("Initializing " + resourceName);

    List<T> installedResources = resourceScanner.getResources(resourceNamespace);

    Optional<T> installedDefaultResources = installedResources
        .stream()
        .filter(i -> i.getMetadata().getName()
            .startsWith(DefaultCustomResourceFactory.DEFAULT_RESOURCE_NAME_PREFIX))
        .findFirst();

    if (installedDefaultResources.isPresent()) {
      LOGGER.info("Default custom resource " + resourceName + " already installed");
    } else {
      LOGGER.info("Installing default custom resource " + resourceName);
      customResourceScheduler.create(defaultResource);
    }
  }

  @Inject
  public void setResourceScheduler(CustomResourceScheduler<T> resourceScheduler) {
    this.customResourceScheduler = resourceScheduler;
  }

  @Inject
  public void setResourceFactory(DefaultCustomResourceFactory<T> resourceFactory) {
    this.resourceFactory = resourceFactory;
  }

  @Inject
  public void setResourceScanner(CustomResourceScanner<T> resourceScanner) {
    this.resourceScanner = resourceScanner;
  }
}
