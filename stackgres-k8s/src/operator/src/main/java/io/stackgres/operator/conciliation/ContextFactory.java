/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;

import io.stackgres.common.CdiUtil;
import jakarta.enterprise.inject.Instance;

public abstract class ContextFactory<C, B> {

  private final List<ContextAppender<C, B>> contextAppenders;

  protected ContextFactory(Instance<ContextAppender<C, B>> contextAppenders) {
    this.contextAppenders = contextAppenders
        .stream()
        .toList();
  }

  protected ContextFactory() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.contextAppenders = null;
  }

  public void appendContext(C context, B contextBuilder) {
    contextAppenders.forEach(contextAppender -> {
      contextAppender.appendContext(context, contextBuilder);
    });
  }

}
