/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.stackgres.common.AbstractStackGresComponents;
import org.jooq.lambda.Seq;

public enum StackGresComponents {

  INSTANCE;

  public static final String LATEST = StackGresComponentsComposer.LATEST;
  public static final StackGresComponentsComposer COMPOSER = new StackGresComponentsComposer();

  public static String get(String component) {
    return COMPOSER.get(component);
  }

  public static String[] getAsArray(String component) {
    return COMPOSER.getAsArray(component);
  }

  public static String getPostgresMajorVersion(String pgVersion) {
    return COMPOSER.getPostgresMajorVersion(pgVersion);
  }

  public static String getPostgresMinorVersion(String pgVersion) {
    return COMPOSER.getPostgresMinorVersion(pgVersion);
  }

  public static String calculatePostgresVersion(String pgVersion) {
    return COMPOSER.calculatePostgresVersion(pgVersion);
  }

  public static Seq<String> getOrderedPostgresVersions() {
    return COMPOSER.getOrderedPostgresVersions();
  }

  public static Seq<String> getAllOrderedPostgresVersions() {
    return COMPOSER.getAllOrderedPostgresVersions();
  }

  private static class StackGresComponentsComposer extends AbstractStackGresComponents {
    StackGresComponentsComposer() {
      super();
    }
  }
}
