/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook;

import java.util.UUID;

import io.fabric8.kubernetes.api.model.GroupVersionKind;
import io.fabric8.kubernetes.api.model.GroupVersionResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class AdmissionRequest<T extends HasMetadata> {

  private UUID uid;

  private GroupVersionKind kind;

  private GroupVersionResource resource;

  private String subResource;

  private GroupVersionKind requestKind;

  private GroupVersionResource requestResource;

  private String requestSubResource;

  private String name;

  private String namespace;

  private Operation operation;

  private UserInfo userInfo;

  private T object;

  private T oldObject;

  private GroupVersionKind options;

  private Boolean dryRun;

  public UUID getUid() {
    return uid;
  }

  public void setUid(UUID uid) {
    this.uid = uid;
  }

  public GroupVersionKind getKind() {
    return kind;
  }

  public void setKind(GroupVersionKind kind) {
    this.kind = kind;
  }

  public GroupVersionResource getResource() {
    return resource;
  }

  public void setResource(GroupVersionResource resource) {
    this.resource = resource;
  }

  public String getSubResource() {
    return subResource;
  }

  public void setSubResource(String subResource) {
    this.subResource = subResource;
  }

  public GroupVersionKind getRequestKind() {
    return requestKind;
  }

  public void setRequestKind(GroupVersionKind requestKind) {
    this.requestKind = requestKind;
  }

  public GroupVersionResource getRequestResource() {
    return requestResource;
  }

  public void setRequestResource(GroupVersionResource requestResource) {
    this.requestResource = requestResource;
  }

  public String getRequestSubResource() {
    return requestSubResource;
  }

  public void setRequestSubResource(String requestSubResource) {
    this.requestSubResource = requestSubResource;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public Operation getOperation() {
    return operation;
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public UserInfo getUserInfo() {
    return userInfo;
  }

  public void setUserInfo(UserInfo userInfo) {
    this.userInfo = userInfo;
  }

  public T getObject() {
    return object;
  }

  public void setObject(T object) {
    this.object = object;
  }

  public T getOldObject() {
    return oldObject;
  }

  public void setOldObject(T oldObject) {
    this.oldObject = oldObject;
  }

  public GroupVersionKind getOptions() {
    return options;
  }

  public void setOptions(GroupVersionKind options) {
    this.options = options;
  }

  public Boolean getDryRun() {
    return dryRun;
  }

  public void setDryRun(Boolean dryRun) {
    this.dryRun = dryRun;
  }

}
