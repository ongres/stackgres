/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.shardingsphere;

import java.util.List;
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
public class ComputeNodeAuthority {

  private ComputeNodePrivilege privilege;

  private List<ComputeNodeUser> users;

  public ComputeNodePrivilege getPrivilege() {
    return privilege;
  }

  public void setPrivilege(ComputeNodePrivilege privilege) {
    this.privilege = privilege;
  }

  public List<ComputeNodeUser> getUsers() {
    return users;
  }

  public void setUsers(List<ComputeNodeUser> users) {
    this.users = users;
  }

  @Override
  public int hashCode() {
    return Objects.hash(privilege, users);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ComputeNodeAuthority)) {
      return false;
    }
    ComputeNodeAuthority other = (ComputeNodeAuthority) obj;
    return Objects.equals(privilege, other.privilege) && Objects.equals(users, other.users);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
