/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupEventEmitter extends AbstractEventEmitter<StackGresBackup> {

  private final LabelFactory<StackGresBackup> labelFactory;

  @Inject
  public BackupEventEmitter(LabelFactory<StackGresBackup> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresBackup involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
