/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public class StackGresExtensionIndex {
  private final String name;
  private final String publisher;
  private final String version;
  private final String flavor;
  private final String postgresVersion;
  private final boolean fromIndex;
  private final List<String> channels;
  private final String build;
  private final Optional<String> arch;
  private final Optional<String> os;

  public static StackGresExtensionIndex fromClusterInstalledExtension(
      StackGresCluster cluster,
      StackGresClusterInstalledExtension installedExtension,
      boolean detectOs) {
    return new StackGresExtensionIndex(cluster, installedExtension,
        Optional.of(ExtensionUtil.OS_DETECTOR).filter(od -> detectOs));
  }

  private StackGresExtensionIndex(
      StackGresCluster cluster,
      StackGresClusterInstalledExtension installedExtension,
      Optional<ExtensionUtil.OsDetector> osDetector) {
    this.name = installedExtension.getName();
    this.publisher = installedExtension.getPublisher();
    this.version = installedExtension.getVersion();
    this.flavor = ExtensionUtil.getFlavorPrefix(cluster);
    this.postgresVersion = installedExtension.getPostgresVersion();
    this.fromIndex = false;
    this.channels = ImmutableList.of();
    this.build = installedExtension.getBuild();
    this.arch = ExtensionUtil.getClusterArch(cluster, osDetector);
    this.os = ExtensionUtil.getClusterOs(cluster, osDetector);
  }

  public StackGresExtensionIndex(
      StackGresExtension extension,
      StackGresExtensionVersion version,
      StackGresExtensionVersionTarget target) {
    this.name = extension.getName();
    this.publisher = extension.getPublisherOrDefault();
    this.version = version.getVersion();
    this.flavor = target.getFlavorOrDefault();
    this.postgresVersion = target.getPostgresVersion();
    this.fromIndex = true;
    this.channels = Seq.seq(extension.getChannels())
        .filter(channel -> channel.v2().equals(version.getVersion()))
        .map(Tuple2::v1)
        .toList();
    this.build = target.getBuild();
    this.arch = Optional.of(target.getArchOrDefault());
    this.os = Optional.of(target.getOsOrDefault());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, flavor, postgresVersion, publisher);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionIndex)) {
      return false;
    }
    StackGresExtensionIndex other = (StackGresExtensionIndex) obj;
    if (Objects.equals(publisher, other.publisher)
        && Objects.equals(name, other.name)
        && Objects.equals(flavor, other.flavor)
        && Objects.equals(postgresVersion, other.postgresVersion)) {
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
      StackGresExtensionIndex self,
      StackGresExtensionIndex other) {
    return Objects.equals(self.channels, other.channels)
        && Objects.equals(self.version, other.version)
        && Objects.equals(self.arch, other.arch)
        && Objects.equals(self.os, other.os)
        && Objects.equals(self.build, other.build);
  }

  private boolean equalsWithFromIndex(
      StackGresExtensionIndex other,
      StackGresExtensionIndex fromIndex) {
    return (fromIndex.version.equals(other.version)
        || fromIndex.channels.stream().anyMatch(other.version::equals))
        && (Objects.isNull(fromIndex.build)
            || ((other.arch.isEmpty() || Objects.equals(fromIndex.arch, other.arch)) // NOPMD
            && (other.os.isEmpty() || Objects.equals(fromIndex.os, other.os))
            && Objects.equals(fromIndex.build, other.build)));
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s/%s/%s-%s-%s%s%s%s",
        publisher, arch, os, name, version,
        flavor, postgresVersion,
        build != null ? "-build-" + build : "",
            channels.isEmpty() ? "" : " (channels: " + channels.stream()
            .collect(Collectors.joining(", ")) + ")");
  }

}
