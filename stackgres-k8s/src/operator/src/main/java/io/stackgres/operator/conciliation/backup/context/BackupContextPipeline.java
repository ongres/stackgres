/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup.context;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class BackupContextPipeline
    extends ContextPipeline<StackGresBackup, Builder> {

  public BackupContextPipeline(Instance<ContextAppender<StackGresBackup, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}
