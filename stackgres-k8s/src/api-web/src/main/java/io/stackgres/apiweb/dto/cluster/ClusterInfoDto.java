/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public final class ClusterInfoDto {

  @JsonProperty("primaryDns")
  private String primaryDns;

  @JsonProperty("replicasDns")
  private String replicasDns;

  @JsonProperty("superuserUsername")
  private String superuserUsername;

  @JsonProperty("superuserSecretName")
  private String superuserSecretName;

  @JsonProperty("superuserPasswordKey")
  private String superuserPasswordKey;

  public String getPrimaryDns() {
    return primaryDns;
  }

  public void setPrimaryDns(String primaryDns) {
    this.primaryDns = primaryDns;
  }

  public String getReplicasDns() {
    return replicasDns;
  }

  public void setReplicasDns(String replicasDns) {
    this.replicasDns = replicasDns;
  }

  public String getSuperuserUsername() {
    return superuserUsername;
  }

  public void setSuperuserUsername(String superuserUsername) {
    this.superuserUsername = superuserUsername;
  }

  public String getSuperuserSecretName() {
    return superuserSecretName;
  }

  public void setSuperuserSecretName(String superuserSecretName) {
    this.superuserSecretName = superuserSecretName;
  }

  public String getSuperuserPasswordKey() {
    return superuserPasswordKey;
  }

  public void setSuperuserPasswordKey(String superuserPasswordKey) {
    this.superuserPasswordKey = superuserPasswordKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(primaryDns, replicasDns, superuserPasswordKey, superuserSecretName,
        superuserUsername);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterInfoDto)) {
      return false;
    }
    ClusterInfoDto other = (ClusterInfoDto) obj;
    return Objects.equals(primaryDns, other.primaryDns)
        && Objects.equals(replicasDns, other.replicasDns)
        && Objects.equals(superuserPasswordKey, other.superuserPasswordKey)
        && Objects.equals(superuserSecretName, other.superuserSecretName)
        && Objects.equals(superuserUsername, other.superuserUsername);
  }

}
