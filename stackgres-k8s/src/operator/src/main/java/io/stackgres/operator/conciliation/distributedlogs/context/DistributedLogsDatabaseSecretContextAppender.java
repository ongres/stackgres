/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext.Builder;
import io.stackgres.operator.conciliation.factory.distributedlogs.v14.DistributedLogsCredentials;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DistributedLogsDatabaseSecretContextAppender
    extends ContextAppender<StackGresDistributedLogs, Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public DistributedLogsDatabaseSecretContextAppender(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  @Override
  public void appendContext(StackGresDistributedLogs distributedLogs, Builder contextBuilder) {
    final Optional<Secret> databaseCredentials =
        secretFinder.findByNameAndNamespace(
            distributedLogs.getMetadata().getName(),
            distributedLogs.getMetadata().getNamespace())
        .or(() -> secretFinder.findByNameAndNamespace(
            DistributedLogsCredentials.secretName(distributedLogs),
            distributedLogs.getMetadata().getNamespace()));

    contextBuilder.databaseSecret(databaseCredentials);
  }

}
