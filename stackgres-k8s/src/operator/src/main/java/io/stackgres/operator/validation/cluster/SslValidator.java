/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class SslValidator implements ClusterValidator {

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public SslValidator(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
      case UPDATE: {
        StackGresCluster cluster = review.getRequest().getObject();
        Optional<StackGresClusterSsl> ssl = Optional.of(cluster.getSpec())
            .map(StackGresClusterSpec::getPostgres)
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
