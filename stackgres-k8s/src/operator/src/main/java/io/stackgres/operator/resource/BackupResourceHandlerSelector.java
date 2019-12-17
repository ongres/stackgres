/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operatorframework.resource.AbstractResourceHandlerSelector;
import io.stackgres.operatorframework.resource.KindLiteral;
import io.stackgres.operatorframework.resource.ResourceHandler;

@ApplicationScoped
public class BackupResourceHandlerSelector
    extends AbstractResourceHandlerSelector<StackGresBackup> {

  private final Instance<ResourceHandler<StackGresBackup>> handlers;

  @Inject
  public BackupResourceHandlerSelector(
      @Any Instance<ResourceHandler<StackGresBackup>> handlers) {
    this.handlers = handlers;
  }

  public BackupResourceHandlerSelector() {
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.handlers = null;
  }

  @Override
  protected Stream<ResourceHandler<StackGresBackup>> getResourceHandlers() {
    return handlers.stream();
  }

  @Override
  protected Optional<ResourceHandler<StackGresBackup>> selectResourceHandler(
      KindLiteral kindLiteral) {
    Instance<ResourceHandler<StackGresBackup>> instance = handlers.select(kindLiteral);
    return instance.isResolvable() ? Optional.of(instance.get()) : Optional.empty();
  }

  @Override
  protected Optional<ResourceHandler<StackGresBackup>> getDefaultResourceHandler() {
    Instance<DefaultBackupResourceHandler> instance = handlers.select(
        DefaultBackupResourceHandler.class);
    return instance.isResolvable() ? Optional.of(instance.get()) : Optional.empty();
  }

}
