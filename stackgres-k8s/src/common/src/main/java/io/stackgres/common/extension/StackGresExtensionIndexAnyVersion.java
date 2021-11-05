/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Objects;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;

public class StackGresExtensionIndexAnyVersion {
  private final String name;
  private final String publisher;
  private final String flavor;
  private final String postgresVersion;
  private final String postgresExactVersion;
  private final boolean fromIndex;
  private final String build;
  private final String arch;
  private final String os;

  public StackGresExtensionIndexAnyVersion(StackGresCluster cluster,
      StackGresClusterExtension extension) {
    this.name = extension.getName();
    this.publisher = extension.getPublisherOrDefault();
    this.flavor = ExtensionUtil.getFlavorPrefix(cluster);
    this.postgresVersion = getPostgresFlavorComponent(cluster).findMajorVersion(
        cluster.getSpec().getPostgres().getVersion());
    this.postgresExactVersion = getPostgresFlavorComponent(cluster).findVersion(
        cluster.getSpec().getPostgres().getVersion());
    this.fromIndex = false;
    this.build = getPostgresFlavorComponent(cluster).findBuildMajorVersion(
        cluster.getSpec().getPostgres().getVersion());
    this.arch = ExtensionUtil.ARCH_X86_64;
    this.os = ExtensionUtil.OS_LINUX;
  }

  public StackGresExtensionIndexAnyVersion(StackGresExtension extension,
      StackGresExtensionVersionTarget target) {
    this.name = extension.getName();
    this.publisher = extension.getPublisherOrDefault();
    this.flavor = target.getFlavorOrDefault();
    this.postgresVersion = target.getPostgresVersion();
    this.postgresExactVersion = null;
    this.fromIndex = true;
    this.build = ExtensionUtil.getMajorBuildOrNull(target.getBuild());
    this.arch = target.getArchOrDefault();
    this.os = target.getOsOrDefault();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, publisher);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionIndexAnyVersion)) {
      return false;
    }
    StackGresExtensionIndexAnyVersion other = (StackGresExtensionIndexAnyVersion) obj;
    if (Objects.equals(publisher, other.publisher)
        && Objects.equals(name, other.name)) {
      if (fromIndex && other.fromIndex) {
        return equalsBothFromIndex(this, other);
      }
      if (!fromIndex && !other.fromIndex) {
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

  private boolean equalsBothFromIndex(StackGresExtensionIndexAnyVersion self,
      StackGresExtensionIndexAnyVersion other) {
    return Objects.equals(self.arch, other.arch)
        && Objects.equals(self.os, other.os)
        && Objects.equals(self.build, other.build)
        && Objects.equals(self.flavor, other.flavor)
        && Objects.equals(self.postgresVersion, other.postgresVersion);
  }

  private boolean equalsWithFromIndex(StackGresExtensionIndexAnyVersion other,
      StackGresExtensionIndexAnyVersion fromIndex) {
    return Objects.equals(fromIndex.flavor, other.flavor)
        && (Objects.equals(fromIndex.postgresVersion, other.postgresVersion) // NOPMD
        || Objects.equals(fromIndex.postgresVersion, other.postgresExactVersion)) // NOPMD
        && (Objects.isNull(fromIndex.build) // NOPMD
            || (Objects.equals(fromIndex.arch, other.arch) // NOPMD
            && Objects.equals(fromIndex.os, other.os)
            && Objects.equals(fromIndex.build, other.build)));
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s/%s/%s-%s%s%s",
        publisher, arch, os, name,
        flavor, postgresExactVersion != null ? postgresExactVersion : postgresVersion,
        build != null ? "-build-" + build : "");
  }

}
