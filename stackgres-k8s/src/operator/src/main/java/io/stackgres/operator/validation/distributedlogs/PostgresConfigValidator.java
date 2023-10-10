/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import static io.stackgres.common.StackGresDistributedLogsUtil.getPostgresFlavorComponent;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfigurations;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PostgresConfigValidator
    extends AbstractReferenceValidator<
      StackGresDistributedLogs, StackGresDistributedLogsReview, StackGresPostgresConfig>
    implements DistributedLogsValidator {

  private final CustomResourceFinder<StackGresPostgresConfig> configFinder;

  private final String errorPostgresMismatchUri;

  @Inject
  public PostgresConfigValidator(
      CustomResourceFinder<StackGresPostgresConfig> configFinder) {
    super(configFinder);
    this.configFinder = configFinder;
    this.errorPostgresMismatchUri = ErrorType.getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresDistributedLogsReview review) throws ValidationFailed {
    StackGresDistributedLogs distributedLogs = review.getRequest().getObject();

    if (distributedLogs == null) {
      return;
    }

    String givenPgVersion = StackGresDistributedLogsUtil.getPostgresVersion(distributedLogs);
    String pgConfig = distributedLogs.getSpec().getConfigurations().getSgPostgresConfig();

    String givenMajorVersion = getPostgresFlavorComponent(distributedLogs).get(distributedLogs)
        .getMajorVersion(givenPgVersion);
    String namespace = distributedLogs.getMetadata().getNamespace();

    super.validate(review);

    switch (review.getRequest().getOperation()) {
      case CREATE:
        validateAgainstConfiguration(givenMajorVersion, pgConfig, namespace);
        break;
      case UPDATE:
        StackGresDistributedLogs oldDistributedLogs = review.getRequest().getOldObject();

        String oldPgConfig = Optional.ofNullable(oldDistributedLogs.getSpec().getConfigurations())
            .map(StackGresDistributedLogsConfigurations::getSgPostgresConfig).orElse(null);
        if (!Objects.equals(oldPgConfig, pgConfig)) {
          validateAgainstConfiguration(givenMajorVersion, pgConfig, namespace);
        }
        break;
      default:
    }
  }

  private void validateAgainstConfiguration(String givenMajorVersion,
      String pgConfig, String namespace) throws ValidationFailed {
    Optional<StackGresPostgresConfig> postgresConfigOpt = configFinder
        .findByNameAndNamespace(pgConfig, namespace);

    if (postgresConfigOpt.isPresent()) {
      StackGresPostgresConfig postgresConfig = postgresConfigOpt.get();
      String pgVersion = postgresConfig.getSpec().getPostgresVersion();

      if (!pgVersion.equals(givenMajorVersion)) {
        final String message = "Invalid postgres version for SGPostgresConfig " + pgConfig
            + ", it must be " + givenMajorVersion;
        fail(errorPostgresMismatchUri, message);
      }
    }
  }

  @Override
  protected Class<StackGresPostgresConfig> getReferenceClass() {
    return StackGresPostgresConfig.class;
  }

  @Override
  protected String getReference(StackGresDistributedLogs resource) {
    return Optional.ofNullable(resource.getSpec().getConfigurations())
        .map(StackGresDistributedLogsConfigurations::getSgPostgresConfig)
        .orElse(null);
  }

  @Override
  protected boolean checkReferenceFilter(StackGresDistributedLogsReview review) {
    return !Optional.ofNullable(review.getRequest().getDryRun()).orElse(false);
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}
