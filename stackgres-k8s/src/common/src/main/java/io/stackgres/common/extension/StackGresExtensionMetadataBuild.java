/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.stackgres.common.StackGresUtil;

public class StackGresExtensionMetadataBuild
    implements Comparable<StackGresExtensionMetadataBuild> {

  private static final Pattern BUILD_PATTERN = Pattern.compile(
      "^(?<major>[0-9]+)\\.(?<minor>[0-9]+)(?:-dev)?$");

  private final Integer majorBuild;
  private final Integer minorBuild;

  public StackGresExtensionMetadataBuild(String build) {
    Optional<Matcher> matcher = Optional.ofNullable(build)
        .map(BUILD_PATTERN::matcher)
        .filter(Matcher::find);
    this.majorBuild = matcher
        .map(m -> m.group("major"))
        .map(Integer::parseInt)
        .orElse(0);
    this.minorBuild = matcher
        .map(m -> m.group("minor"))
        .map(Integer::parseInt)
        .orElse(0);
  }

  public int getMajorBuild() {
    return majorBuild;
  }

  public int getMinorBuild() {
    return minorBuild;
  }

  @Override
  public int hashCode() {
    return Objects.hash(majorBuild, minorBuild);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionMetadataBuild)) {
      return false;
    }
    StackGresExtensionMetadataBuild other = (StackGresExtensionMetadataBuild) obj;
    return Objects.equals(majorBuild, other.majorBuild)
        && Objects.equals(minorBuild, other.minorBuild);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public int compareTo(StackGresExtensionMetadataBuild o) {
    int compare = majorBuild.compareTo(o.majorBuild);
    if (compare == 0) {
      compare = minorBuild.compareTo(o.minorBuild);
    }
    return compare;
  }

}
