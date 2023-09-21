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
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterServiceBinding {

  private String provider;

  private String database;

  private String username;

  @Valid
  private SecretKeySelector password;

  @ReferencedField("username")
  interface Username extends FieldReference { }

  @ReferencedField("password")
  interface Password extends FieldReference { }

  @JsonIgnore
  @AssertTrue(
      message = "username and password values can only be null or not null both at the same time",
      payload = { Username.class, Password.class })
  public boolean isNotNullUsernameAndPasswordOrIsNullUsernameAndPassword() {
    return ((username == null || username.isEmpty()) && password == null)
        || ((username != null && !username.isEmpty()) && password != null);
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public SecretKeySelector getPassword() {
    return password;
  }

  public void setPassword(SecretKeySelector password) {
    this.password = password;
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, database, username, password);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterServiceBinding)) {
      return false;
    }
    StackGresClusterServiceBinding other =
        (StackGresClusterServiceBinding) obj;
    return Objects.equals(provider, other.provider)
      && Objects.equals(database, other.database) && Objects.equals(username, other.username)
      && Objects.equals(password, other.password);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
