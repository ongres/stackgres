/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public class StackGresExtensionIndex {
  private final String name;
  private final String publisher;
  private final String version;
  private final String postgresVersion;
  private final String postgresExactVersion;
  private final boolean fromIndex;
  private final List<String> channels;
  private final String build;
  private final String arch;
  private final String os;

  public StackGresExtensionIndex(StackGresClusterInstalledExtension installedExtension) {
    this.name = installedExtension.getName();
    this.publisher = installedExtension.getPublisher();
    this.version = installedExtension.getVersion();
    this.postgresVersion = installedExtension.getPostgresVersion();
    this.postgresExactVersion = installedExtension.getPostgresExactVersion();
    this.fromIndex = false;
    this.channels = ImmutableList.of();
    this.build = installedExtension.getBuild();
    this.arch = ExtensionUtil.ARCH_X86_64;
    this.os = ExtensionUtil.OS_LINUX;
  }

  public StackGresExtensionIndex(StackGresExtension extension, StackGresExtensionVersion version,
      StackGresExtensionVersionTarget target) {
    this.name = extension.getName();
    this.publisher = extension.getPublisherOrDefault();
    this.version = version.getVersion();
    this.postgresVersion = target.getPostgresVersion();
    this.postgresExactVersion = target.getPostgresExactVersion();
    this.fromIndex = true;
    this.channels = Seq.seq(extension.getChannels())
        .filter(channel -> channel.v2().equals(version.getVersion()))
        .map(Tuple2::v1)
        .collect(ImmutableList.toImmutableList());
    this.build = target.getBuild();
    this.arch = target.getArchOrDefault();
    this.os = target.getOsOrDefault();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, postgresVersion, publisher);
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
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(postgresExactVersion, other.postgresExactVersion)) {
      if (fromIndex && other.fromIndex) {
        return Objects.equals(channels, other.channels)
            && Objects.equals(version, other.version)
            && Objects.equals(arch, other.arch)
            && Objects.equals(os, other.os)
            && Objects.equals(build, other.build);
      }
      if (fromIndex && !other.fromIndex) {
        return (version.equals(other.version)
            || channels.stream()
                .anyMatch(channel -> channel.equals(other.version)))
            && (Objects.isNull(build)
                || (Objects.equals(arch, other.arch) // NOPMD
                && Objects.equals(os, other.os)
                && Objects.equals(build, other.build)));
      }
      if (!fromIndex && other.fromIndex) {
        return (version.equals(other.version)
            || other.channels.stream()
                .anyMatch(channel -> channel.equals(version)))
            && (Objects.isNull(other.build)
                || (Objects.equals(arch, other.arch) // NOPMD
                && Objects.equals(os, other.os)
                && Objects.equals(build, other.build)));
      }
      if (!fromIndex && !other.fromIndex) {
        return Objects.equals(version, other.version)
            && Objects.equals(arch, other.arch)
            && Objects.equals(os, other.os)
            && Objects.equals(build, other.build);
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s/%s/%s-%s-pg%s%s%s",
        publisher, arch, os, name, version,
        postgresExactVersion == null ? postgresVersion : postgresExactVersion,
        build != null ? "-build-" + build : "",
            channels.isEmpty() ? "" : " (channels: " + channels.stream()
            .collect(Collectors.joining(", ")) + ")");
  }

}
