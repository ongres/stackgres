/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.transformer.EventMapper;
import io.stackgres.common.resource.ResourceScanner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventDtoScanner implements ResourceScanner<EventDto> {

  private final KubernetesClient client;

  @Inject
  public EventDtoScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public List<EventDto> findResources() {
    return client.v1().events().list().getItems().stream()
        .map(EventMapper::map)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<EventDto> findResourcesInNamespace(String namespace) {
    return client.v1().events().inNamespace(namespace).list().getItems().stream()
        .map(EventMapper::map)
        .collect(ImmutableList.toImmutableList());
  }
}
