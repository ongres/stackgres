/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresScriptContext extends GenerationContext<StackGresScript> {

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  public static class Builder extends ImmutableStackGresScriptContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
