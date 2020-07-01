/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class ScriptsConfigValidator implements ClusterValidator {

  private static final String forbiddenCrUpdateUri = ErrorType
      .getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();

    List<StackGresClusterScriptEntry> scripts = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getInitData)
        .map(StackGresClusterInitData::getScripts)
        .orElse(ImmutableList.of());

    checkScriptsConfig(review, scripts);
  }

  private void checkScriptsConfig(StackGresClusterReview review,
                                  List<StackGresClusterScriptEntry> scripts)
      throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      List<StackGresClusterScriptEntry> oldScripts = Optional
          .ofNullable(review.getRequest().getOldObject().getSpec().getInitData())
          .map(StackGresClusterInitData::getScripts)
          .orElse(ImmutableList.of());

      if (!Objects.equals(scripts, oldScripts)) {
        fail(forbiddenCrUpdateUri, "Cannot update cluster's scripts configuration");
      }
    }
  }

}
