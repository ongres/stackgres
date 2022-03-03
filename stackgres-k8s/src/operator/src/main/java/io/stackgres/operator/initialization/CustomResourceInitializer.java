/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.validation.DefaultCustomResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomResourceInitializer<T extends CustomResource<?, ?>>
    implements DefaultCustomResourceInitializer {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CustomResourceInitializer.class);

  private final CustomResourceScheduler<T> customResourceScheduler;
  private final DefaultCustomResourceFactory<T> factory;
  private final CustomResourceScanner<T> resourceScanner;
  private final DefaultCustomResourceHolder<T> holder;
  private T targetResource;

  public CustomResourceInitializer(CustomResourceScheduler<T> customResourceScheduler,
                                   DefaultCustomResourceFactory<T> factory,
                                   CustomResourceScanner<T> resourceScanner,
                                   DefaultCustomResourceHolder<T> holder) {
    this.customResourceScheduler = customResourceScheduler;
    this.factory = factory;
    this.resourceScanner = resourceScanner;
    this.holder = holder;
  }

  public void loadGeneratedResources() {

    T defaultResource = factory.buildResource();
    final String kind = HasMetadata.getKind(
        defaultResource.getClass());
    LOGGER.info("Loading generated default resources of Kind {}", kind);
    String resourceNamespace = defaultResource.getMetadata().getNamespace();
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
      final T customResource = installedDefaultResources.get();
      String resourceName = customResource.getMetadata().getName();
      LOGGER.info("A default resource was found {}, default resource generation would "
          + "be skipped for this kind {}", resourceName, kind);
      holder.holdDefaultCustomResource(customResource);
      targetResource = customResource;
    } else {
      targetResource = defaultResource;
    }
  }

  @Override
  public void initialize() {
    String resourceName = targetResource.getMetadata().getName();

    if (holder.isDefaultCustomResource(targetResource)) {
      LOGGER.info("Skipping default custom resource {} installation", resourceName);
    } else {
      LOGGER.info("Installing default custom resource {}", resourceName);
      holder.holdDefaultCustomResource(targetResource);
      customResourceScheduler.create(targetResource);
    }
  }
}
