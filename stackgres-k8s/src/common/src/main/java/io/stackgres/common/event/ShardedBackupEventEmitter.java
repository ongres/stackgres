/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedBackupEventEmitter extends AbstractEventEmitter<StackGresShardedBackup> {

  private final LabelFactory<StackGresShardedBackup> labelFactory;

  @Inject
  public ShardedBackupEventEmitter(LabelFactory<StackGresShardedBackup> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresShardedBackup involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
