/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import org.immutables.value.Value;

@Value.Immutable
public interface ManagedSqlScriptEntry {

  StackGresClusterManagedSqlStatus getManagedSqlStatus();

  StackGresClusterManagedScriptEntry getManagedScript();

  StackGresClusterManagedScriptEntryStatus getManagedScriptStatus();

  StackGresScript getScript();

  StackGresScriptEntry getScriptEntry();

  StackGresScriptEntryStatus getScriptEntryStatus();

  default String getManagedScriptEntryDescription() {
    return getManagedScript().getId() + " (" + getManagedScript().getSgScript() + "),"
        + " entry " + getScriptEntry().getId()
        + Optional.ofNullable(getScriptEntry().getName())
            .map(name -> " (" + name + ")").orElse("");
  }

}
