/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;

public abstract class AbstractStackGresComponents {

  public static final String LATEST = "latest";

  private final ImmutableMap<String, String> componentVersions;

  protected AbstractStackGresComponents() {
    this.componentVersions = readComponentVersions();
  }

  @SuppressFBWarnings(value = "UI_INHERITANCE_UNSAFE_GETRESOURCE",
      justification = "It is the wanted behavior")
  protected ImmutableMap<String, String> readComponentVersions() {
    try (InputStream is = getClass().getResourceAsStream("/versions.properties")) {
      Properties properties = new Properties();
      properties.load(is);
      return Seq.seq(properties)
          .collect(ImmutableMap.toImmutableMap(
              t -> t.v1.toString(), t -> t.v2.toString()));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public String get(String component) {
    return componentVersions.get(component);
  }

  public String[] getAsArray(String component) {
    return get(component).split(",");
  }

  public String getPostgresMajorVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  public String getPostgresMinorVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(versionSplit + 1, pgVersion.length());
  }

  public String calculatePostgresVersion(String pgVersion) {
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

  public Seq<String> getOrderedPostgresVersions() {
    return Seq.of(getAsArray("postgresql"))
        .map(version -> Tuple.tuple(
            Integer.parseInt(getPostgresMajorVersion(version)),
            Integer.parseInt(getPostgresMinorVersion(version)),
            version))
        .sorted(Comparator.reverseOrder())
        .map(Tuple3::v3);
  }

  public Seq<String> getAllOrderedPostgresVersions() {
    return Seq.of(LATEST)
        .append(getOrderedPostgresVersions()
            .flatMap(version -> Seq.of(getPostgresMajorVersion(version), version)));
  }

}
