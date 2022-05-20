/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import org.jooq.lambda.Seq;

public enum StackGresVersion {

  V_1_0("1.0"),
  V_1_1("1.1"),
  V_1_2("1.2"),
  V_1_3("1.3");

  public static final StackGresVersion OLDEST = Seq.of(StackGresVersion.values())
      .findFirst().orElseThrow();

  public static final StackGresVersion LATEST = Seq.of(StackGresVersion.values())
      .findLast().orElseThrow();

  final String version;
  final long versionAsNumber;

  StackGresVersion(String version) {
    this.version = version;
    this.versionAsNumber = getVersionAsNumber(version);
  }

  public String getVersion() {
    return version;
  }

  public long getVersionAsNumber() {
    return versionAsNumber;
  }

  public static long getVersionAsNumber(String version) {
    int lastMajorVersionIndex = version.indexOf('.') - 1;
    if (lastMajorVersionIndex < 0) {
      throw new IllegalArgumentException(
          "Version " + version + " is not parseable.");
    }
    int lastMinorVersionIndex = version.length();
    for (int index = lastMajorVersionIndex + 2; index < version.length(); index++) {
      char character = version.charAt(index);
      if (character < '0' || character > '9') {
        lastMinorVersionIndex = index;
        break;
      }
    }
    try {
      long majorVersion = Long.parseLong(
          version.substring(0, lastMajorVersionIndex + 1));
      long minorVersion = Long.parseLong(
          version.substring(lastMajorVersionIndex + 2, lastMinorVersionIndex));
      if (majorVersion > 0x3FF
          || minorVersion > 0x3FF) {
        throw new Exception("Too large numbers");
      }
      return majorVersion << 10
          | minorVersion;
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Version " + version + " is not parseable.", ex);
    }
  }

  static StackGresVersion ofVersion(String version) {
    return Stream.of(values())
        .filter(minorVersion -> version.startsWith(minorVersion.version + ".")
            || version.equals(minorVersion.version))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "Invalid version " + version));
  }

  public static StackGresVersion getStackGresVersion(StackGresCluster cluster) {
    return getStackGresVersionFromResource(cluster);
  }

  public static StackGresVersion getStackGresVersion(StackGresBackup backup) {
    return getStackGresVersionFromResource(backup);
  }

  public static StackGresVersion getStackGresVersion(StackGresDbOps dbOps) {
    return getStackGresVersionFromResource(dbOps);
  }

  public static StackGresVersion getStackGresVersion(StackGresDistributedLogs distributedLogs) {
    return getStackGresVersionFromResource(distributedLogs);
  }

  private static StackGresVersion getStackGresVersionFromResource(HasMetadata resource) {
    return Optional.of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .map(StackGresVersion::ofVersion)
        .orElse(StackGresVersion.LATEST);
  }

  public static long getStackGresVersionAsNumber(StackGresCluster cluster) {
    return getStackGresVersionFromResourceAsNumber(cluster);
  }

  public static long getStackGresVersionAsNumber(StackGresBackup backup) {
    return getStackGresVersionFromResourceAsNumber(backup);
  }

  public static long getStackGresVersionAsNumber(StackGresDbOps dbOps) {
    return getStackGresVersionFromResourceAsNumber(dbOps);
  }

  public static long getStackGresVersionAsNumber(
      StackGresDistributedLogs distributedLogs) {
    return getStackGresVersionFromResourceAsNumber(distributedLogs);
  }

  private static long getStackGresVersionFromResourceAsNumber(HasMetadata resource) {
    return Optional.of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .map(StackGresVersion::getVersionAsNumber)
        .orElseGet(() -> StackGresVersion.LATEST.getVersionAsNumber());
  }

}
