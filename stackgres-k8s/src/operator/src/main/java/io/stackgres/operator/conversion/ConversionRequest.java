/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ConversionRequest {

  private UUID uid;

  @JsonProperty("desiredAPIVersion")
  private String desiredApiVersion;

  private List<ObjectNode> objects;

  public UUID getUid() {
    return uid;
  }

  public void setUid(UUID uid) {
    this.uid = uid;
  }

  public String getDesiredApiVersion() {
    return desiredApiVersion;
  }

  public void setDesiredApiVersion(String desiredApiVersion) {
    this.desiredApiVersion = desiredApiVersion;
  }

  public List<ObjectNode> getObjects() {
    return objects;
  }

  public void setObjects(List<ObjectNode> objects) {
    this.objects = objects;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversionRequest that = (ConversionRequest) o;
    return Objects.equals(uid, that.uid)
        && Objects.equals(desiredApiVersion, that.desiredApiVersion)
        && Objects.equals(objects, that.objects);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid, desiredApiVersion, objects);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uid", uid)
        .add("desiredApiVersion", desiredApiVersion)
        .add("objects", objects)
        .toString();
  }
}
