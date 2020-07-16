/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Comparator;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;

public enum StackGresComponents {

  INSTANCE;

  public static final String LATEST = "latest";
  public static final ImmutableMap<String, String> COMPONENT_VERSIONS =
      INSTANCE.componentVersions;

  private final ImmutableMap<String, String> componentVersions;

  StackGresComponents() {
    try {
      Properties properties = new Properties();
      properties.load(StackGresComponents.class.getResourceAsStream("/versions.properties"));
      this.componentVersions = Seq.seq(properties)
          .collect(ImmutableMap.toImmutableMap(
              t -> t.v1.toString(), t -> t.v2.toString()));
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String get(String component) {
    return COMPONENT_VERSIONS.get(component);
  }

  public static String[] getAsArray(String component) {
    return get(component).split(",");
  }

  public static String getPostgresMajorVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  public static String getPostgresMinorVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(versionSplit + 1, pgVersion.length());
  }

  public static String calculatePostgresVersion(String pgVersion) {
    if (pgVersion == null || LATEST.equals(pgVersion)) {
      return getOrderedPostgresVersions()
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("postgresql versions not configured"));
    }

    if (!pgVersion.contains(".")) {
      return getOrderedPostgresVersions()
          .filter(version -> version.startsWith(pgVersion))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("postgresql versions not configured"));
    }

    return pgVersion;
  }

  public static Seq<String> getOrderedPostgresVersions() {
    return Seq.of(StackGresComponents.getAsArray("postgresql"))
        .map(version -> Tuple.tuple(
            Integer.parseInt(StackGresComponents.getPostgresMajorVersion(version)),
            Integer.parseInt(StackGresComponents.getPostgresMinorVersion(version)),
            version))
        .sorted(Comparator.reverseOrder())
        .map(Tuple3::v3);
  }

  public static Seq<String> getAllOrderedPostgresVersions() {
    return Seq.of(LATEST)
        .append(getOrderedPostgresVersions()
            .flatMap(version -> Seq.of(getPostgresMajorVersion(version), version)));
  }

}
