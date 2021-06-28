/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.transformer.EventMapper;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.resource.ResourceScanner;

@ApplicationScoped
public class EventDtoScanner implements ResourceScanner<EventDto> {

  private final KubernetesClientFactory factory;

  @Inject
  public EventDtoScanner(KubernetesClientFactory factory) {
    this.factory = factory;
  }

  @Override
  public List<EventDto> findResources() {
    try (KubernetesClient client = factory.create()) {
      return client.v1().events().list().getItems().stream()
          .map(EventMapper::map)
          .collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public List<EventDto> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = factory.create()) {
      return client.v1().events().inNamespace(namespace).list().getItems().stream()
          .map(EventMapper::map)
          .collect(ImmutableList.toImmutableList());
    }
  }
}
