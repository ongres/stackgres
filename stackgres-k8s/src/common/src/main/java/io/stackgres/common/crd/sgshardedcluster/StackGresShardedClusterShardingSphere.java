/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ShardingSphereServiceAccount;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterShardingSphere {

  private String version;

  private StackGresShardedClusterShardingSphereMode mode;

  private StackGresShardedClusterShardingSphereAuthority authority;

  private ShardingSphereServiceAccount serviceAccount;

  private StackGresShardedClusterShardingSphereProperties properties;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public StackGresShardedClusterShardingSphereMode getMode() {
    return mode;
  }

  public void setMode(StackGresShardedClusterShardingSphereMode mode) {
    this.mode = mode;
  }

  public StackGresShardedClusterShardingSphereAuthority getAuthority() {
    return authority;
  }

  public void setAuthority(StackGresShardedClusterShardingSphereAuthority authority) {
    this.authority = authority;
  }

  public ShardingSphereServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ShardingSphereServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public StackGresShardedClusterShardingSphereProperties getProperties() {
    return properties;
  }

  public void setProperties(StackGresShardedClusterShardingSphereProperties properties) {
    this.properties = properties;
  }

  @Override
  public int hashCode() {
    return Objects.hash(authority, mode, properties, serviceAccount, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterShardingSphere)) {
      return false;
    }
    StackGresShardedClusterShardingSphere other = (StackGresShardedClusterShardingSphere) obj;
    return Objects.equals(authority, other.authority) && Objects.equals(mode, other.mode)
        && Objects.equals(properties, other.properties)
        && Objects.equals(serviceAccount, other.serviceAccount)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
