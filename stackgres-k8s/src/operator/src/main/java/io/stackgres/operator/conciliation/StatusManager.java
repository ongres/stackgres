/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

public interface StatusManager<T, C> {

  T refreshCondition(T source);

  void updateCondition(C condition, T context);
}
