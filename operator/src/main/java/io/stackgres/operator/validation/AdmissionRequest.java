/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.GroupVersionKind;
import io.fabric8.kubernetes.api.model.GroupVersionResource;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;

public class AdmissionRequest {

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

  private JsonNode object;

  private JsonNode oldObject;

  private JsonNode options;

  private boolean dryRun;

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

  public JsonNode getObject() {
    return object;
  }

  public void setObject(JsonNode object) {
    this.object = object;
  }

  public JsonNode getOldObject() {
    return oldObject;
  }

  public void setOldObject(JsonNode oldObject) {
    this.oldObject = oldObject;
  }

  public JsonNode getOptions() {
    return options;
  }

  public void setOptions(JsonNode options) {
    this.options = options;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }
}
