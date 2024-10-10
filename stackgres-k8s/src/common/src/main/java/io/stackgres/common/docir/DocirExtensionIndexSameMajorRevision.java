/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.OsDetector;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;

public class DocirExtensionIndexSameMajorRevision {
  private final String name;
  private final String version;
  private final String flavor;
  private final String flavorVersion;
  private final boolean fromIndex;
  private final String revision;
  private final Optional<String> arch;
  private final Optional<String> os;

  public static DocirExtensionIndexSameMajorRevision fromClusterExtension(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      boolean detectOs) {
    return new DocirExtensionIndexSameMajorRevision(context, cluster, extension,
        Optional.of(OsDetector.OS_DETECTOR).filter(od -> detectOs));
  }

  private DocirExtensionIndexSameMajorRevision(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      Optional<OsDetector> osDetector) {
    this.name = extension.getName();
    this.version = extension.getVersion();
    this.flavor = DocirUtil.getFlavorName(cluster);
    this.flavorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getVersion(context, cluster.getSpec().getPostgres().getVersion());
    this.fromIndex = false;
    this.revision = getPostgresFlavorComponent(cluster).get(cluster)
        .getBuildMajorVersion(context, cluster.getSpec().getPostgres().getVersion());
    this.arch = OsDetector.getClusterArch(cluster, osDetector);
    this.os = OsDetector.getClusterOs(cluster, osDetector);
  }

  public DocirExtensionIndexSameMajorRevision(
      DocirExtension extension,
      DocirExtensionVersion version) {
    this.name = extension.getName();
    this.version = version.getVersion();
    this.flavor = version.getFlavorOrDefault();
    this.flavorVersion = version.getFlavorVersion();
    this.fromIndex = true;
    this.revision = version.getBaseIdentity();
    this.arch = Optional.of(version.getArchOrDefault());
    this.os = Optional.of(version.getOsOrDefault());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirExtensionIndexSameMajorRevision)) {
      return false;
    }
    DocirExtensionIndexSameMajorRevision other = (DocirExtensionIndexSameMajorRevision) obj;
    if (Objects.equals(name, other.name)) {
      if ((fromIndex && other.fromIndex)
          || (!fromIndex && !other.fromIndex)) {
        return equalsSameFromIndex(this, other);
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

  private boolean equalsSameFromIndex(
      DocirExtensionIndexSameMajorRevision self,
      DocirExtensionIndexSameMajorRevision other) {
    return Objects.equals(self.version, other.version)
        && Objects.equals(self.arch, other.arch)
        && Objects.equals(self.os, other.os)
        && Objects.equals(self.revision, other.revision)
        && Objects.equals(self.flavor, other.flavor)
        && Objects.equals(self.flavorVersion, other.flavorVersion);
  }

  private boolean equalsWithFromIndex(
      DocirExtensionIndexSameMajorRevision other,
      DocirExtensionIndexSameMajorRevision fromIndex) {
    return fromIndex.version.equals(other.version)
        && Objects.equals(fromIndex.flavor, other.flavor)
        && Objects.equals(fromIndex.flavorVersion, other.flavorVersion) // NOPMD
        && (Objects.isNull(fromIndex.revision) // NOPMD
            || ((other.arch.isEmpty() || Objects.equals(fromIndex.arch, other.arch)) // NOPMD
            && (other.os.isEmpty() || Objects.equals(fromIndex.os, other.os))
            && Objects.equals(fromIndex.revision, other.revision)));
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s/%s-%s-%s%s%s",
        arch.orElse("any"), os.orElse("any"), name, version,
        flavor, flavorVersion,
        revision != null ? "-revision-" + revision : "");
  }

}
