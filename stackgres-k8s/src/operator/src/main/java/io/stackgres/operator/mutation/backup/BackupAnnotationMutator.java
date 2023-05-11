/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class BackupAnnotationMutator
    extends AbstractAnnotationMutator<StackGresBackup, BackupReview>
    implements BackupMutator {

  // On version removed change this code to use the oldest one
  private static final long VERSION_1_3 = StackGresVersion.V_1_3.getVersionAsNumber();

  @Override
  public Map<String, String> getAnnotationsToOverwrite(StackGresBackup resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (VERSION_1_3 > version) {
      return Map.of(StackGresContext.VERSION_KEY, StackGresVersion.V_1_3.getVersion());
    }
    return Map.of();
  }

}
