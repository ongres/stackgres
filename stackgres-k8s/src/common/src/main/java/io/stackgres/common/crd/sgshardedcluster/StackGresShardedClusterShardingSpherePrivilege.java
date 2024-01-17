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
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterShardingSpherePrivilege {

  private String type;

  private String userDatabaseMappings;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUserDatabaseMappings() {
    return userDatabaseMappings;
  }

  public void setUserDatabaseMappings(String userDatabaseMappings) {
    this.userDatabaseMappings = userDatabaseMappings;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, userDatabaseMappings);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterShardingSpherePrivilege)) {
      return false;
    }
    StackGresShardedClusterShardingSpherePrivilege other = (StackGresShardedClusterShardingSpherePrivilege) obj;
    return Objects.equals(type, other.type) && Objects.equals(userDatabaseMappings, other.userDatabaseMappings);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
