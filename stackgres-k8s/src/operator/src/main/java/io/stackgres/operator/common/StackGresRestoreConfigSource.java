/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;

public class StackGresRestoreConfigSource {

  private final StackGresClusterRestore restore;
  private final StackGresBackup backup;
  private final Map<String, Map<String, String>> secrets;

  private StackGresRestoreConfigSource(Builder builder) {
    this.restore = builder.restore;
    this.backup = builder.backup;
    this.secrets = builder.secrets;
  }

  public StackGresClusterRestore getRestore() {
    return restore;
  }

  public StackGresBackup getBackup() {
    return backup;
  }

  public Map<String, Map<String, String>> getSecrets() {
    return secrets;
  }

  /**
   * Creates builder to build {@link StackGresRestoreConfigSource}.
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link StackGresRestoreConfigSource}.
   */
  public static final class Builder {
    private StackGresClusterRestore restore;
    private StackGresBackup backup;
    private Map<String, Map<String, String>> secrets;

    private Builder() {}

    public Builder withRestore(StackGresClusterRestore restore) {
      this.restore = restore;
      return this;
    }

    public Builder withBackup(StackGresBackup backup) {
      this.backup = backup;
      return this;
    }

    public Builder withSecrets(Map<String, Map<String, String>> secrets) {
      this.secrets = secrets;
      return this;
    }

    public StackGresRestoreConfigSource build() {
      return new StackGresRestoreConfigSource(this);
    }
  }

}
