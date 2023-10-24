/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class ValidatingWebhookConfigurationWriter extends AbstractUnamespacedResourceWriter<
    ValidatingWebhookConfiguration,
    Resource<ValidatingWebhookConfiguration>> {

  @Inject
  public ValidatingWebhookConfigurationWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<
          ValidatingWebhookConfiguration,
          ?,
          Resource<ValidatingWebhookConfiguration>> getResourceEndpoints(KubernetesClient client) {
    return client.admissionRegistration().v1().validatingWebhookConfigurations();
  }

}
