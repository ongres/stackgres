/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
public class ExtensionsValidator extends AbstractExtensionsValidator<StackGresClusterReview>
    implements ClusterValidator {

  private final ClusterExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ExtensionsValidator(ClusterExtensionMetadataManager extensionMetadataManager) {
    super();
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  protected ClusterExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected ImmutableList<Tuple2<String, Optional<String>>> getDefaultExtensions(
      StackGresCluster cluster) {
    return StackGresUtil.getDefaultClusterExtensions(cluster);
  }

  @Override
  protected StackGresCluster getCluster(StackGresClusterReview review) {
    return review.getRequest().getObject();
  }

  @Override
  protected Optional<List<StackGresClusterExtension>> getPostgresExtensions(
      StackGresClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions);
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresClusterSpec::getToInstallPostgresExtensions);
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
