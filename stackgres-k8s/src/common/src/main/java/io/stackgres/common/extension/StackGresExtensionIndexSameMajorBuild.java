/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public class StackGresExtensionIndexSameMajorBuild {
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

  public StackGresExtensionIndexSameMajorBuild(StackGresCluster cluster,
      StackGresClusterExtension extension) {
    this.name = extension.getName();
    this.publisher = extension.getPublisherOrDefault();
    this.version = extension.getVersionOrDefaultChannel();
    this.postgresVersion = StackGresComponent.POSTGRESQL.findMajorVersion(
        cluster.getSpec().getPostgresVersion());
    this.postgresExactVersion = StackGresComponent.POSTGRESQL.findVersion(
        cluster.getSpec().getPostgresVersion());
    this.fromIndex = false;
    this.channels = ImmutableList.of();
    this.build = StackGresComponent.POSTGRESQL.findBuildMajorVersion(
        cluster.getSpec().getPostgresVersion());
    this.arch = ExtensionUtil.ARCH_X86_64;
    this.os = ExtensionUtil.OS_LINUX;
  }

  public StackGresExtensionIndexSameMajorBuild(StackGresExtension extension,
      StackGresExtensionVersion version, StackGresExtensionVersionTarget target) {
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
    this.build = ExtensionUtil.getMajorBuildOrNull(target.getBuild());
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
    if (!(obj instanceof StackGresExtensionIndexSameMajorBuild)) {
      return false;
    }
    StackGresExtensionIndexSameMajorBuild other = (StackGresExtensionIndexSameMajorBuild) obj;
    if (Objects.equals(publisher, other.publisher)
        && Objects.equals(name, other.name)
        && Objects.equals(postgresVersion, other.postgresVersion)) {
      if (fromIndex && other.fromIndex) {
        return Objects.equals(channels, other.channels)
            && Objects.equals(version, other.version)
            && Objects.equals(arch, other.arch)
            && Objects.equals(os, other.os)
            && Objects.equals(build, other.build)
            && Objects.equals(postgresExactVersion, other.postgresExactVersion);
      }
      if (fromIndex && !other.fromIndex) {
        return (version.equals(other.version)
            || channels.stream()
            .anyMatch(channel -> channel.equals(other.version)))
            && (Objects.isNull(postgresExactVersion) // NOPMD
                || Objects.equals(postgresExactVersion, other.postgresExactVersion)) // NOPMD
            && (Objects.isNull(build) // NOPMD
                || (Objects.equals(arch, other.arch) // NOPMD
                && Objects.equals(os, other.os)
                && Objects.equals(build, other.build)));
      }
      if (!fromIndex && other.fromIndex) {
        return (version.equals(other.version)
            || other.channels.stream()
            .anyMatch(channel -> channel.equals(version)))
            && (Objects.isNull(other.postgresExactVersion) // NOPMD
                || Objects.equals(postgresExactVersion, other.postgresExactVersion)) // NOPMD
            && (Objects.isNull(other.build) // NOPMD
                || (Objects.equals(arch, other.arch) // NOPMD
                && Objects.equals(os, other.os)
                && Objects.equals(build, other.build)));
      }
      if (!fromIndex && !other.fromIndex) {
        return Objects.equals(version, other.version)
            && Objects.equals(arch, other.arch)
            && Objects.equals(os, other.os)
            && Objects.equals(build, other.build)
            && Objects.equals(postgresExactVersion, other.postgresExactVersion);
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s/%s/%s-%s-pg%s%s%s",
        publisher, arch, os, name, version, postgresVersion,
        build != null ? "-build-" + build : "",
            channels.isEmpty() ? "" : " (channels: " + channels.stream()
            .collect(Collectors.joining(", ")) + ")");
  }

}
