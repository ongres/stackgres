/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;

@ApplicationScoped
public class StatusParserProviderImpl implements StatusParserProvider {

  private Integer kubernetesVersion;

  @PostConstruct
  @Inject
  public void init(KubernetesClientFactory clientFactory) {
    try (KubernetesClient client = clientFactory.create()) {

      kubernetesVersion = Optional.ofNullable(client.getVersion())
          .map(v -> Integer.parseInt(v.getMinor()))
          .orElse(16);
    }
  }

  @Override
  public StatusParser getStatusParser() {
    if (kubernetesVersion >= 16) {
      return new Kubernetes16StatusParser();
    } else {
      return new Kubernetes12StatusParser();
    }
  }

}
