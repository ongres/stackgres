/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

public abstract class ContextAppender<C, T> {

  protected abstract void appendContext(C inputContext, T contextBuilder);

}
