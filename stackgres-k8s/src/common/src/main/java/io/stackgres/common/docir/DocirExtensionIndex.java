/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.OsDetector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

public class DocirExtensionIndex {
  private final String repository;
  private final String name;
  private final String version;
  private final String flavor;
  private final String flavorVersion;
  private final boolean fromIndex;
  private final String revision;
  private final Optional<String> arch;
  private final Optional<String> os;

  public static DocirExtensionIndex fromClusterInstalledExtension(
      StackGresCluster cluster,
      StackGresClusterInstalledExtension installedExtension,
      boolean detectOs) {
    return new DocirExtensionIndex(cluster, installedExtension,
        Optional.of(OsDetector.OS_DETECTOR).filter(od -> detectOs));
  }

  private DocirExtensionIndex(
      StackGresCluster cluster,
      StackGresClusterInstalledExtension installedExtension,
      Optional<OsDetector> osDetector) {
    this.repository = installedExtension.getRepository();
    this.name = installedExtension.getName();
    this.version = installedExtension.getVersion();
    this.flavor = DocirUtil.getFlavorName(cluster);
    this.flavorVersion = installedExtension.getPostgresVersion();
    this.fromIndex = false;
    this.revision = installedExtension.getBuild();
    this.arch = OsDetector.getClusterArch(cluster, osDetector);
    this.os = OsDetector.getClusterOs(cluster, osDetector);
  }

  public DocirExtensionIndex(
      DocirExtension extension,
      DocirExtensionVersion version) {
    this.repository = extension.getRepository();
    this.name = extension.getName();
    this.version = version.getVersion();
    this.flavor = version.getFlavorOrDefault();
    this.flavorVersion = version.getFlavorVersion();
    this.fromIndex = true;
    this.revision = version.getRevision();
    this.arch = Optional.of(version.getArchOrDefault());
    this.os = Optional.of(version.getOsOrDefault());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, flavor, flavorVersion, repository);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirExtensionIndex)) {
      return false;
    }
    DocirExtensionIndex other = (DocirExtensionIndex) obj;
    if (Objects.equals(repository, other.repository)
        && Objects.equals(name, other.name)
        && Objects.equals(flavor, other.flavor)
        && Objects.equals(flavorVersion, other.flavorVersion)) {
      if ((fromIndex && other.fromIndex)
          || (!fromIndex && !other.fromIndex)) {
        return equalsBothFromIndex(this, other);
      }
      if (fromIndex && !other.fromIndex) {
        return equalsWithFromIndex(other, this);
      }
      if (!fromIndex && other.fromIndex) {
        return equalsWithFromIndex(this, other);
      }
    }
    return false;
  }

  private boolean equalsBothFromIndex(
      DocirExtensionIndex self,
      DocirExtensionIndex other) {
    return Objects.equals(self.version, other.version)
        && Objects.equals(self.arch, other.arch)
        && Objects.equals(self.os, other.os)
        && Objects.equals(self.revision, other.revision);
  }

  private boolean equalsWithFromIndex(
      DocirExtensionIndex other,
      DocirExtensionIndex fromIndex) {
    return fromIndex.version.equals(other.version)
        && (Objects.isNull(fromIndex.revision)
            || ((other.arch.isEmpty() || Objects.equals(fromIndex.arch, other.arch)) // NOPMD
            && (other.os.isEmpty() || Objects.equals(fromIndex.os, other.os))
            && Objects.equals(fromIndex.revision, other.revision)));
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s/%s/%s-%s-%s%s%s",
        repository, arch.orElse("any"), os.orElse("any"), name, version,
        flavor, flavorVersion,
        revision != null ? "-revision-" + revision : "");
  }

}
