/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PatroniInitialConfigValidator implements ClusterValidator {

  private final String errorCrReferencerUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      StackGresCluster cluster = review.getRequest().getObject();

      Optional<StackGresClusterPatroniConfig> patroniInitialConfig =
          Optional.of(cluster.getSpec())
          .map(StackGresClusterSpec::getConfigurations)
          .map(StackGresClusterConfigurations::getPatroni)
          .map(StackGresClusterPatroni::getInitialConfig);

      StackGresClusterSpec oldSpec = review.getRequest().getOldObject().getSpec();
      Optional<StackGresClusterPatroniConfig> oldPatroniInitialConfig = Optional
          .ofNullable(oldSpec.getConfigurations())
          .map(StackGresClusterConfigurations::getPatroni)
          .map(StackGresClusterPatroni::getInitialConfig);

      var pgCtlTimeout = patroniInitialConfig
          .flatMap(StackGresClusterPatroniConfig::getPgCtlTimeout);
      var oldPgCtlTimeout = oldPatroniInitialConfig
          .flatMap(StackGresClusterPatroniConfig::getPgCtlTimeout);
          
      if (!Objects.equals(pgCtlTimeout, oldPgCtlTimeout)) {
        if (pgCtlTimeout.isPresent() && oldPatroniInitialConfig.isEmpty()) {
          if (oldSpec.getConfigurations() == null) {
            oldSpec.setConfigurations(new StackGresClusterConfigurations());
          }
          if (oldSpec.getConfigurations().getPatroni() == null) {
            oldSpec.getConfigurations().setPatroni(new StackGresClusterPatroni());
          }
          if (oldSpec.getConfigurations().getPatroni().getInitialConfig() == null) {
            oldSpec.getConfigurations().getPatroni().setInitialConfig(new StackGresClusterPatroniConfig());
          }
          oldPatroniInitialConfig = Optional
              .ofNullable(oldSpec.getConfigurations())
              .map(StackGresClusterConfigurations::getPatroni)
              .map(StackGresClusterPatroni::getInitialConfig);
        }
        final Optional<StackGresClusterPatroniConfig> modifiableOldPatroniInitialConfig = oldPatroniInitialConfig;
        pgCtlTimeout
            .ifPresentOrElse(
                value -> modifiableOldPatroniInitialConfig.ifPresent(config -> config.setPgCtlTimeout(value)),
                () -> modifiableOldPatroniInitialConfig.ifPresent(config -> config.removePostgresql()));
      }

      if (!Objects.equals(oldPatroniInitialConfig, patroniInitialConfig)) {
        fail(errorCrReferencerUri, "Cannot update cluster's patroni initial configuration");
      }
    }
  }

}
