/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceConversionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ServiceReferenceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.WebhookClientConfigBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.WebhookConversionBuilder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.common.resource.SecretFinder;

public class WebhookConfiguratorImpl implements WebhookConfigurator {

  String operatorName = CrdUpgradeProperty.OPERATOR_NAME.getString();

  String operatorNamespace = CrdUpgradeProperty.OPERATOR_NAMESPACE
      .getString();

  String operatorSecretName = operatorName + "-certs";

  private final SecretFinder secretFinder;

  private ResourceFinder<CustomResourceDefinition> crdFinder;

  private ResourceWriter<CustomResourceDefinition> crdWriter;

  private CrdLoader crdLoader;

  public WebhookConfiguratorImpl(SecretFinder secretFinder,
      ResourceFinder<CustomResourceDefinition> crdFinder,
      ResourceWriter<CustomResourceDefinition> crdWriter,
      CrdLoader crdLoader) {
    this.secretFinder = secretFinder;
    this.crdFinder = crdFinder;
    this.crdWriter = crdWriter;
    this.crdLoader = crdLoader;
  }

  @Override
  public void configureWebhooks() {

    String webhookCaCert = getWebhookCaCert()
        .orElseThrow(() -> new RuntimeException("Operator certificates secret not found"));

    crdLoader.scanDefinitions()
        .forEach(crd -> configureWebhook(crd.getMetadata().getName(), webhookCaCert));
  }

  protected void configureWebhook(String name, String webhookCaCert) {
    CustomResourceDefinition customResourceDefinition = crdFinder.findByName(name)
        .orElseThrow(() -> new RuntimeException("Custom Resource Definition "
            + name + " not found"));
    customResourceDefinition.getSpec().setPreserveUnknownFields(false);

    String conversionPath = "/stackgres/conversion/"
        + customResourceDefinition.getSpec().getNames().getSingular();
    customResourceDefinition.getSpec().setConversion(new CustomResourceConversionBuilder()
        .withStrategy("Webhook")
        .withWebhook(new WebhookConversionBuilder()
            .withClientConfig(new WebhookClientConfigBuilder()
                .withCaBundle(webhookCaCert)
                .withService(new ServiceReferenceBuilder()
                    .withNamespace(operatorNamespace)
                    .withName(operatorName)
                    .withPath(conversionPath)
                    .build())
                .build())
            .withConversionReviewVersions("v1", "v1beta1")
            .build())
        .build());
    crdWriter.update(customResourceDefinition);
  }

  protected Optional<String> getWebhookCaCert() {
    return secretFinder.findByNameAndNamespace(operatorSecretName, operatorNamespace)
        .map(Secret::getData)
        .map(data -> data.get("server.crt"));
  }
}
