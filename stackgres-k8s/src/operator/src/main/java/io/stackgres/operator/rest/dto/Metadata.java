/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto;

import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Metadata {

  @NotNull
  private String namespace;

  @NotNull
  private String name;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("namespace", namespace)
        .add("name", name)
        .toString();
  }

}
