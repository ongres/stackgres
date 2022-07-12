/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import org.immutables.value.Value;

@Value.Immutable
public abstract class StackGresBackupContext {

  public abstract StackGresBackupConfig getBackupConfig();

  public abstract Map<String, Map<String, String>> getSecrets();

}
