/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.jobs.common.KubernetesClientFactoryImpl;
import io.stackgres.jobs.common.SecretFinder;
import io.stackgres.jobs.crdupgrade.CrdInstaller;
import io.stackgres.jobs.crdupgrade.CrdInstallerImpl;
import io.stackgres.jobs.crdupgrade.CrdLoader;
import io.stackgres.jobs.crdupgrade.CrdLoaderImpl;
import io.stackgres.jobs.crdupgrade.CrdUpgradeProperty;
import io.stackgres.jobs.crdupgrade.CustomResourceDefinitionFinder;
import io.stackgres.jobs.crdupgrade.WebhookConfigurator;
import io.stackgres.jobs.crdupgrade.WebhookConfiguratorImpl;

public class Main {

  private static final Boolean CONVERSION_WEBHOOKS = CrdUpgradeProperty
      .CONVERSION_WEBHOOKS.getBoolean();
  private static final Boolean CRD_UPGRADE = CrdUpgradeProperty
      .CRD_UPGRADE.getBoolean();

  @SuppressWarnings("deprecation")
  public static void main(String[] args) {

    final KubernetesClientFactoryImpl kubernetesClientFactory = new KubernetesClientFactoryImpl();
    /*
     * This is a hack to prevent empty arrays being added to json serializer in native image.
     * For some reason when CRDs are being serialized in json, in the native image the annotation
     * @JsonInclude(Include.NON_EMPTY) is ignored.
     */
    Serialization.jsonMapper().disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    final CrdLoader crdLoader = new CrdLoaderImpl(kubernetesClientFactory);
    final CustomResourceDefinitionFinder crdFinder
        = new CustomResourceDefinitionFinder(kubernetesClientFactory);

    if (CRD_UPGRADE) {
      CrdInstaller crdInstaller = new CrdInstallerImpl(crdFinder, crdFinder, crdLoader);
      crdInstaller.installCustomResourceDefinitions();
    }

    if (CONVERSION_WEBHOOKS) {
      final SecretFinder secretFinder = new SecretFinder(kubernetesClientFactory);
      WebhookConfigurator webhookConfigurator = new WebhookConfiguratorImpl(
          secretFinder,
          crdFinder,
          crdFinder,
          crdLoader);

      webhookConfigurator.configureWebhooks();
    }
  }

}
