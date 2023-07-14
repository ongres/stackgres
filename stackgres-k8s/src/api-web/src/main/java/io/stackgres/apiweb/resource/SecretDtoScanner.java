/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.dto.secret.SecretDto;
import io.stackgres.apiweb.transformer.SecretMapper;
import io.stackgres.common.resource.ResourceScanner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SecretDtoScanner implements ResourceScanner<SecretDto> {

  private final KubernetesClient client;

  @Inject
  public SecretDtoScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public List<SecretDto> findResources() {
    return client.secrets().list().getItems().stream()
        .map(SecretMapper::map)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<SecretDto> findResourcesInNamespace(String namespace) {
    return client.secrets().inNamespace(namespace).list().getItems().stream()
        .map(SecretMapper::map)
        .collect(ImmutableList.toImmutableList());
  }
}
