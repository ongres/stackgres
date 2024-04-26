/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.user;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.ValidationGroups;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UserDto extends ResourceDto {

  private String k8sUsername;

  private String apiUsername;

  private String password;

  private List<UserRoleRef> roles;

  private List<UserRoleRef> clusterRoles;

  @ReferencedField("k8sUsername")
  interface K8sUsername extends FieldReference { }

  @ReferencedField("apiUsername")
  interface ApiUsername extends FieldReference { }

  @ReferencedField("password")
  interface Password extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "k8sUsername is required and can not be blank",
      payload = { K8sUsername.class },
      groups = { ValidationGroups.Post.class, ValidationGroups.Put.class })
  public boolean isK8sUsernameRequiredAndNotBlank() {
    return k8sUsername != null && !k8sUsername.isBlank();
  }

  @JsonIgnore
  @AssertTrue(message = "apiUsername can not be blank",
      payload = { ApiUsername.class },
      groups = { ValidationGroups.Post.class, ValidationGroups.Put.class })
  public boolean isApiUsernameNotBlank() {
    return apiUsername == null || !apiUsername.isBlank();
  }

  @JsonIgnore
  @AssertTrue(message = "password is required and can not be blank",
      payload = { Password.class },
      groups = { ValidationGroups.Post.class })
  public boolean isPasswordRequiredAndNotBlank() {
    return password != null && !password.isBlank();
  }

  @JsonIgnore
  @AssertTrue(message = "password can not be blank",
      payload = { Password.class },
      groups = { ValidationGroups.Put.class })
  public boolean isPasswordNotBlank() {
    return password == null || !password.isBlank();
  }

  @JsonIgnore
  public Subject getSubject() {
    Subject subject = new Subject();
    subject.setApiGroup("rbac.authorization.k8s.io");
    subject.setKind("User");
    subject.setName(getK8sUsername());
    return subject;
  }

  public String getK8sUsername() {
    return k8sUsername;
  }

  public void setK8sUsername(String k8sUsername) {
    this.k8sUsername = k8sUsername;
  }

  public String getApiUsername() {
    return apiUsername;
  }

  public void setApiUsername(String apiUsername) {
    this.apiUsername = apiUsername;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public List<UserRoleRef> getRoles() {
    return roles;
  }

  public void setRoles(List<UserRoleRef> roles) {
    this.roles = roles;
  }

  public List<UserRoleRef> getClusterRoles() {
    return clusterRoles;
  }

  public void setClusterRoles(List<UserRoleRef> clusterRoles) {
    this.clusterRoles = clusterRoles;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
