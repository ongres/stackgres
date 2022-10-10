/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class DbOpsAnnotationMutator
    extends AbstractAnnotationMutator<StackGresDbOps, DbOpsReview>
    implements DbOpsMutator {

  private static final long VERSION_1_1 = StackGresVersion.V_1_1.getVersionAsNumber();

  @Override
  public Optional<Map<String, String>> getAnnotationsToOverwrite(StackGresDbOps resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (VERSION_1_1 > version) {
      return Optional.of(Map.of(StackGresContext.VERSION_KEY, StackGresVersion.V_1_1.getVersion()));
    }
    return Optional.empty();
  }

}
