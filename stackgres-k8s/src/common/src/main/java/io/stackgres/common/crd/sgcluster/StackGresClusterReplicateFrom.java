/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterReplicateFrom {

  @JsonProperty("instance")
  @Valid
  private StackGresClusterReplicateFromInstance instance;

  @JsonProperty("storage")
  @Valid
  private StackGresClusterReplicateFromStorage storage;

  @JsonProperty("users")
  @Valid
  private StackGresClusterReplicateFromUsers users;

  @ReferencedField("instance")
  interface Instance extends FieldReference { }

  @ReferencedField("storage")
  interface Storage extends FieldReference { }

  @ReferencedField("users")
  interface Users extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "One of internal or storage is required",
      payload = { Instance.class, Storage.class })
  public boolean isInstanceOrStoragePresent() {
    return instance != null || storage != null;
  }

  @JsonIgnore
  @AssertTrue(message = "storage is forbidden when replicating from an SGCluster",
      payload = { Storage.class })
  public boolean isStorageNullWithSgCluster() {
    return instance == null || instance.getSgCluster() == null || storage == null;
  }

  @JsonIgnore
  @AssertTrue(message = "users is required when replicating from external instance or storage",
      payload = { Users.class })
  public boolean isUsersNotNullWithExternalOrStorage() {
    return ((instance == null || instance.getExternal() == null) && storage == null)
        || users != null;
  }

  @JsonIgnore
  @AssertTrue(message = "users is forbidden when replicating from an SGCluster",
      payload = { Users.class })
  public boolean isUsersNullWithSgCluster() {
    return instance == null || instance.getSgCluster() == null || users == null;
  }

  public StackGresClusterReplicateFromInstance getInstance() {
    return instance;
  }

  public void setInstance(StackGresClusterReplicateFromInstance instance) {
    this.instance = instance;
  }

  public StackGresClusterReplicateFromStorage getStorage() {
    return storage;
  }

  public void setStorage(StackGresClusterReplicateFromStorage storage) {
    this.storage = storage;
  }

  public StackGresClusterReplicateFromUsers getUsers() {
    return users;
  }

  public void setUsers(StackGresClusterReplicateFromUsers users) {
    this.users = users;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance, storage, users);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicateFrom)) {
      return false;
    }
    StackGresClusterReplicateFrom other = (StackGresClusterReplicateFrom) obj;
    return Objects.equals(instance, other.instance) && Objects.equals(storage, other.storage)
        && Objects.equals(users, other.users);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
