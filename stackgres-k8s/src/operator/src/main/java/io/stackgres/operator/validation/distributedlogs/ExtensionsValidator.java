/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operator.validation.ExtensionReview;
import io.stackgres.operator.validation.ImmutableExtensionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ExtensionsValidator extends AbstractExtensionsValidator<StackGresDistributedLogsReview>
    implements DistributedLogsValidator {

  private final ExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ExtensionsValidator(ExtensionMetadataManager extensionMetadataManager) {
    super();
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected ExtensionReview getExtensionReview(StackGresDistributedLogsReview review) {
    return ImmutableExtensionReview.builder()
        .postgresVersion(getPostgresVersion(review))
        .postgresFlavor(getPostgresFlavor())
        .arch(getArch(review))
        .os(getOs(review))
        .defaultExtensions(getDefaultExtensions(review))
        .toInstallExtensions(getToInstallExtensions(review))
        .stackGresVersion(StackGresVersion.getStackGresVersion(review.getRequest().getObject()))
        .build();
  }

  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresDistributedLogsReview review) {
    final StackGresDistributedLogs distributedLogs = review.getRequest().getObject();
    final StackGresVersion stackGresVersion = StackGresVersion.getStackGresVersion(
        distributedLogs
    );
    return StackGresUtil.getDefaultDistributedLogsExtensions(
        StackGresDistributedLogsUtil.getPostgresVersion(distributedLogs),
        stackGresVersion
    );
  }

  protected Optional<String> getArch(StackGresDistributedLogsReview review) {
    return Optional.of(review.getRequest().getObject())
        .map(StackGresDistributedLogs::getStatus)
        .map(StackGresDistributedLogsStatus::getArch);
  }

  protected Optional<String> getOs(StackGresDistributedLogsReview review) {
    return Optional.of(review.getRequest().getObject())
        .map(StackGresDistributedLogs::getStatus)
        .map(StackGresDistributedLogsStatus::getOs);
  }

  protected String getPostgresVersion(StackGresDistributedLogsReview review) {
    final StackGresDistributedLogs distributedLogs = review.getRequest().getObject();
    return StackGresDistributedLogsUtil.getPostgresVersion(distributedLogs);
  }

  protected StackGresComponent getPostgresFlavor() {
    return StackGresComponent.POSTGRESQL;
  }

  protected List<StackGresClusterInstalledExtension> getToInstallExtensions(
      StackGresDistributedLogsReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions)
        .orElse(List.of());
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
