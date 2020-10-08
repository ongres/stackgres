/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

public interface EventReason {

  String component();

  String reason();

  public Type type();

  public enum Type {
    NORMAL("Normal"),
    WARNING("Warning");

    private final String type;

    Type(String type) {
      this.type = type;
    }

    public String type() {
      return type;
    }
  }
}
