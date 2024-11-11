/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum StackGresPort implements StackGresNamedObject {

  CUSTOM("custom-%s");

  private final String name;
  private final String resourceNameFormat;

  StackGresPort(String name) {
    this.name = name;
    this.resourceNameFormat = null;
  }

  StackGresPort(String name,
      String resourceNameFormat) {
    this.name = name;
    this.resourceNameFormat = resourceNameFormat;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getResourceNameFormat() {
    return resourceNameFormat;
  }

  public String getResourceName(String...parameters) {
    return format(getResourceNameFormat(), parameters);
  }

  @Override
  public String toString() {
    return getName();
  }

}
