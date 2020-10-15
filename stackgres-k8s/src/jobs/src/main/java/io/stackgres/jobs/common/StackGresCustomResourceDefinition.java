/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.common;

import java.util.Objects;

import com.google.common.base.MoreObjects;

public class StackGresCustomResourceDefinition {

  private String kind;

  private String name;

  private String singular;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSingular() {
    return singular;
  }

  public void setSingular(String singular) {
    this.singular = singular;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresCustomResourceDefinition that = (StackGresCustomResourceDefinition) o;
    return Objects.equals(kind, that.kind)
        && Objects.equals(name, that.name)
        && Objects.equals(singular, that.singular);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, name, singular);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("kind", kind)
        .add("name", name)
        .add("singular", singular)
        .toString();
  }
}
