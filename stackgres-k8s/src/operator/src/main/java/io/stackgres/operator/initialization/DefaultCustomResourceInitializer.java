/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

public interface DefaultCustomResourceInitializer extends Initializer {

  void initialize();

  @Override
  default void run() {
    initialize();
  }

}
