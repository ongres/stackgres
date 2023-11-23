/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ValidatingWebhookConfigurationFinder
    implements ResourceFinder<ValidatingWebhookConfiguration> {

  private final KubernetesClient client;

  @Inject
  public ValidatingWebhookConfigurationFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<ValidatingWebhookConfiguration> findByName(String name) {
    return Optional.ofNullable(client.admissionRegistration().v1().validatingWebhookConfigurations()
        .withName(name)
        .get());
  }

  @Override
  public Optional<ValidatingWebhookConfiguration> findByNameAndNamespace(
      String name, String namespace) {
    throw new UnsupportedOperationException(
        "ValidatingWebhookConfigurations are namespaced resources, try using "
        + "findByName(String) to locale validating webhook configurations");
  }

}
