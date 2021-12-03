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
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

public class StackGresExtensionMetadata {

  private static final Pattern BUILD_PATTERN = Pattern.compile(
      "^(?<major>[0-9]+)\\.(?<minor>[0-9]+)(?:-dev)?$");

  private final StackGresExtension extension;
  private final StackGresExtensionVersion version;
  private final StackGresExtensionVersionTarget target;
  private final StackGresExtensionPublisher publisher;
  private final Integer majorBuild;
  private final Integer minorBuild;

  public StackGresExtensionMetadata(StackGresExtension extension,
      StackGresExtensionVersion version, StackGresExtensionVersionTarget target,
      StackGresExtensionPublisher publisher) {
    super();
    this.extension = extension;
    this.version = version;
    this.target = target;
    this.publisher = publisher;
    Optional<Matcher> matcher = Optional.ofNullable(target.getBuild())
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

  public StackGresExtensionMetadata(StackGresClusterInstalledExtension installedExtension) {
    super();
    this.extension = new StackGresExtension();
    this.extension.setName(installedExtension.getName());
    this.extension.setRepository(installedExtension.getRepository());
    this.version = new StackGresExtensionVersion();
    this.version.setVersion(installedExtension.getVersion());
    this.version.setExtraMounts(installedExtension.getExtraMounts());
    this.target = new StackGresExtensionVersionTarget();
    this.target.setPostgresVersion(installedExtension.getPostgresVersion());
    this.target.setBuild(installedExtension.getBuild());
    this.target.setOs(ExtensionUtil.DEFAULT_OS);
    this.target.setArch(ExtensionUtil.DEFAULT_ARCH);
    this.publisher = new StackGresExtensionPublisher();
    Optional<Matcher> matcher = Optional.ofNullable(installedExtension.getBuild())
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

  public StackGresExtension getExtension() {
    return extension;
  }

  public StackGresExtensionVersion getVersion() {
    return version;
  }

  public int getMajorBuild() {
    return majorBuild;
  }

  public int getMinorBuild() {
    return minorBuild;
  }

  public StackGresExtensionVersionTarget getTarget() {
    return target;
  }

  public StackGresExtensionPublisher getPublisher() {
    return publisher;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extension, publisher, target, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionMetadata)) {
      return false;
    }
    StackGresExtensionMetadata other = (StackGresExtensionMetadata) obj;
    return Objects.equals(extension, other.extension)
        && Objects.equals(publisher, other.publisher) && Objects.equals(target, other.target)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  public static int compareBuild(StackGresExtensionMetadata left,
      StackGresExtensionMetadata right) {
    int compare = left.majorBuild.compareTo(right.majorBuild);
    if (compare == 0) {
      compare = left.minorBuild.compareTo(right.minorBuild);
    }
    return compare;
  }

}
