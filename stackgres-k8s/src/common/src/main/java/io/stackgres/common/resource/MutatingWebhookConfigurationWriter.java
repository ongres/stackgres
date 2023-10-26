/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfiguration;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class MutatingWebhookConfigurationWriter
    extends AbstractUnamespacedResourceWriter<
        MutatingWebhookConfiguration,
        Resource<MutatingWebhookConfiguration>> {

  @Inject
  public MutatingWebhookConfigurationWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<
          MutatingWebhookConfiguration,
          ?,
          Resource<MutatingWebhookConfiguration>> getResourceEndpoints(KubernetesClient client) {
    return client.admissionRegistration().v1().mutatingWebhookConfigurations();
  }

}
