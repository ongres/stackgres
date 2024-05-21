/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum StackGresGroupKind {

  CLUSTER,
  DBOPS("dbops"),
  BACKUP("backup"),
  STREAM("stream");

  private final String containerPrefix;

  StackGresGroupKind(String containerPrefix) {
    this.containerPrefix = containerPrefix + ".";
  }

  StackGresGroupKind() {
    this.containerPrefix = "";
  }

  public String getContainerPrefix() {
    return containerPrefix;
  }

  public boolean hasPrefix(String name) {
    if (containerPrefix.isEmpty()) {
      return !name.contains(".");
    }
    return name.startsWith(containerPrefix);
  }

  public String getName(String name) {
    return name.substring(containerPrefix.length());
  }

  @Override
  public String toString() {
    return name();
  }

}
