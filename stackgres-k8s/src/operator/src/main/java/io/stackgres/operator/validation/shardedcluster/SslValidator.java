/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class SslValidator implements ShardedClusterValidator {

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public SslValidator(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        Optional<StackGresClusterSsl> ssl = Optional.of(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getSsl);
        if (ssl.map(StackGresClusterSsl::getEnabled).orElse(false)) {
          if (ssl.get().getCertificateSecretKeySelector() != null) {
            checkIfSecretOrKeyExists(cluster.getMetadata().getNamespace(),
                ssl.get().getCertificateSecretKeySelector(),
                "Certificate Secret " + ssl.get().getCertificateSecretKeySelector().getName()
                + " or key " + ssl.get().getCertificateSecretKeySelector().getKey() + " not found");
          }
          if (ssl.get().getPrivateKeySecretKeySelector() != null) {
            checkIfSecretOrKeyExists(cluster.getMetadata().getNamespace(),
                ssl.get().getPrivateKeySecretKeySelector(),
                "Private key Secret " + ssl.get().getPrivateKeySecretKeySelector().getName()
                + " or key " + ssl.get().getPrivateKeySecretKeySelector().getKey() + " not found");
          }
        }
        break;
      }
      default:
    }
  }

  private void checkIfSecretOrKeyExists(String namespace, SecretKeySelector secretKeySelector,
      String onError) throws ValidationFailed {
    Optional<Secret> secret = secretFinder
        .findByNameAndNamespace(secretKeySelector.getName(), namespace);

    if (!secret.filter(s -> s.getData()
        .containsKey(secretKeySelector.getKey())).isPresent()) {
      fail(onError);
    }
  }

}
