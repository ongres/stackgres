/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import static io.stackgres.common.StackGresDistributedLogsUtil.getStackGresClusterForDistributedLogs;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ExtensionsValidator
    extends AbstractExtensionsValidator<StackGresDistributedLogs, StackGresDistributedLogsReview>
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
  protected List<ExtensionTuple> getDefaultExtensions(StackGresDistributedLogs resource) {
    final StackGresVersion operatorVersion = StackGresVersion.getStackGresVersion(
        resource
    );
    return StackGresUtil.getDefaultDistributedLogsExtensions(
        StackGresDistributedLogsUtil.getPostgresVersion(resource),
        operatorVersion
    );
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(StackGresDistributedLogs resource) {
    return List.of();
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresDistributedLogs resource) {
    return Optional.ofNullable(resource.getSpec())
        .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresDistributedLogs resource) {
    return new StackGresClusterBuilder(getStackGresClusterForDistributedLogs(resource))
        .withNewStatus()
        .withOs(Optional.ofNullable(resource.getStatus())
            .map(StackGresDistributedLogsStatus::getOs)
            .orElse(null))
        .withArch(Optional.ofNullable(resource.getStatus())
            .map(StackGresDistributedLogsStatus::getArch)
            .orElse(null))
        .endStatus()
        .build();
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
