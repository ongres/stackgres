/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.Status;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ConversionResponse {

  private UUID uid;

  private Status result;

  private List<ObjectNode> convertedObjects;

  public UUID getUid() {
    return uid;
  }

  public void setUid(UUID uid) {
    this.uid = uid;
  }

  public Status getResult() {
    return result;
  }

  public void setResult(Status result) {
    this.result = result;
  }

  public List<ObjectNode> getConvertedObjects() {
    return convertedObjects;
  }

  public void setConvertedObjects(List<ObjectNode> convertedObjects) {
    this.convertedObjects = convertedObjects;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversionResponse response = (ConversionResponse) o;
    return Objects.equals(uid, response.uid)
        && Objects.equals(result, response.result)
        && Objects.equals(convertedObjects, response.convertedObjects);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid, result, convertedObjects);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uid", uid)
        .add("result", result)
        .add("convertedObjects", convertedObjects)
        .toString();
  }
}
