/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum StackGresProperty {

  CRD_GROUP("stackgres.group"),
  CRD_VERSION("stackgres.crd.version"),
  CONTAINER_BUILD("stackgres.containerBuild");

  private final String systemProperty;

  StackGresProperty(String systemProperty) {
    this.systemProperty = systemProperty;
  }

  public String systemProperty() {
    return systemProperty;
  }

}
