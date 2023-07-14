/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresBackupContext, PodSecurityContext> {

  @Override
  public PodSecurityContext createResource(StackGresBackupContext source) {
    return createPodSecurityContext();
  }

}
