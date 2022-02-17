/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import org.jooq.lambda.Seq;

public enum StackGresVersion {

  V_1_0("1.0"),
  V_1_1("1.1"),
  V_1_2("1.2");

  public static final StackGresVersion OLDEST = Seq.of(StackGresVersion.values())
      .findFirst().orElseThrow();

  public static final StackGresVersion LATEST = Seq.of(StackGresVersion.values())
      .findLast().orElseThrow();

  final String version;

  StackGresVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  static StackGresVersion ofVersion(String version) {
    return Stream.of(values())
        .filter(minorVersion -> version.startsWith(minorVersion.version + ".")
            || version.equals(minorVersion.version))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "Invalid version " + version));
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresCluster cluster) {
    return getStackGresVersionFromResource(cluster);
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresBackup backup) {
    return getStackGresVersionFromResource(backup);
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresDbOps dbOps) {
    return getStackGresVersionFromResource(dbOps);
  }

  public static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersion(
      StackGresDistributedLogs distributedLogs) {
    return getStackGresVersionFromResource(distributedLogs);
  }

  private static <T extends CustomResource<?, ?>> StackGresVersion getStackGresVersionFromResource(
      HasMetadata resource) {
    return Optional.of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .map(StackGresVersion::ofVersion)
        .orElse(StackGresVersion.LATEST);
  }

}
