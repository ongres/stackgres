/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigSource;
import io.stackgres.operator.patroni.PatroniRestoreSource;
import io.stackgres.operatorframework.Operation;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class RestoreSecretSourceValidator implements RestoreConfigValidator {

  private PatroniRestoreSource restoreSource;

  private KubernetesClientFactory clientFactory;

  @Inject
  public RestoreSecretSourceValidator(PatroniRestoreSource restoreSource,
                                      KubernetesClientFactory clientFactory) {
    this.restoreSource = restoreSource;
    this.clientFactory = clientFactory;
  }

  @Override
  public void validate(RestoreConfigReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE
        || operation == Operation.UPDATE) {

      StackgresRestoreConfig restoreConfig = review.getRequest().getObject();

      StackgresRestoreConfigSource source = restoreSource.getStorageConfig(restoreConfig);

      if (!source.isAutoCopySecretsEnabled()) {

        try (KubernetesClient client = clientFactory.create()) {

          for (PatroniRestoreSource.SourceSecret sourceSecret : restoreSource
              .getSourceCredentials(restoreConfig)) {

            Secret restoreSecret = client.secrets()
                .inNamespace(restoreConfig.getMetadata().getNamespace())
                .withName(sourceSecret.getSecretName())
                .get();

            if (restoreSecret == null) {
              throw new ValidationFailed("Secret " + sourceSecret.getSecretName()
                  + " is required to install the restore configuration ");
            }
          }
        }
      }
    }

  }
}
