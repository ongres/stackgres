/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterConfigurations {

  @Valid
  private List<StackGresShardedClusterBackupConfiguration> backups;

  @Valid
  private StackGresClusterCredentials credentials;

  @Valid
  private StackGresClusterServiceBinding binding;

  public List<StackGresShardedClusterBackupConfiguration> getBackups() {
    return backups;
  }

  public void setBackups(List<StackGresShardedClusterBackupConfiguration> backups) {
    this.backups = backups;
  }

  public StackGresClusterCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(StackGresClusterCredentials credentials) {
    this.credentials = credentials;
  }

  public StackGresClusterServiceBinding getBinding() {
    return binding;
  }

  public void setBinding(StackGresClusterServiceBinding binding) {
    this.binding = binding;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backups, binding, credentials);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterConfigurations)) {
      return false;
    }
    StackGresShardedClusterConfigurations other = (StackGresShardedClusterConfigurations) obj;
    return Objects.equals(backups, other.backups)
        && Objects.equals(binding, other.binding)
        && Objects.equals(credentials, other.credentials);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
