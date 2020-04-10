/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;

public class StackGresBackupContext {

  private final StackGresBackupConfig backupConfig;
  private final Map<String, Map<String, String>> secrets;

  private StackGresBackupContext(Builder builder) {
    this.backupConfig = builder.backupConfig;
    this.secrets = builder.secrets;
  }

  public StackGresBackupConfig getBackupConfig() {
    return backupConfig;
  }

  public Map<String, Map<String, String>> getSecrets() {
    return secrets;
  }

  /**
   * Creates builder to build {@link StackGresBackupContext}.
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link StackGresBackupContext}.
   */
  public static final class Builder {
    private StackGresBackupConfig backupConfig;
    private Map<String, Map<String, String>> secrets;

    private Builder() {}

    public Builder withBackupConfig(StackGresBackupConfig backupConfig) {
      this.backupConfig = backupConfig;
      return this;
    }

    public Builder withSecrets(Map<String, Map<String, String>> secrets) {
      this.secrets = secrets;
      return this;
    }

    public StackGresBackupContext build() {
      return new StackGresBackupContext(this);
    }
  }

}
