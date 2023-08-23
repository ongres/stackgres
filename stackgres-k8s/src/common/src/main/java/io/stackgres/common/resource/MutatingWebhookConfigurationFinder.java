/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfiguration;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class MutatingWebhookConfigurationFinder
    implements ResourceFinder<MutatingWebhookConfiguration> {

  private final KubernetesClient client;

  @Inject
  public MutatingWebhookConfigurationFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<MutatingWebhookConfiguration> findByName(String name) {
    return Optional.ofNullable(client.admissionRegistration().v1().mutatingWebhookConfigurations()
        .withName(name)
        .get());
  }

  @Override
  public Optional<MutatingWebhookConfiguration> findByNameAndNamespace(
      String name, String namespace) {
    throw new UnsupportedOperationException(
        "MutatingWebhookConfigurations are namespaced resources, try using "
        + "findByName(String) to locale mutating webhook configurations");
  }

}
