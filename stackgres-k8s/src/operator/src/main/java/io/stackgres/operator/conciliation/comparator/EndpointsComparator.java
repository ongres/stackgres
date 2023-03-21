/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

public abstract class EndpointsComparator extends AbstractComparator {

  private final IgnorePatch[] ignorePatchPatterns = {
      new SimpleIgnorePatch("/subsets",
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return ignorePatchPatterns;
  }

}
