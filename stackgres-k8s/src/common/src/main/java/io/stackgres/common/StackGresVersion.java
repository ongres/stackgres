/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgstream.StackGresStream;
import org.jooq.lambda.Seq;

public enum StackGresVersion {

  UNDEFINED,
  V_1_13("1.13"),
  V_1_14("1.14"),
  V_1_15("1.15");

  public static final StackGresVersion OLDEST =
      Seq.of(values()).skip(1).findFirst().get();

  public static final StackGresVersion LATEST =
      Seq.of(values()).findLast().get();

  final String version;
  final long versionAsNumber;

  StackGresVersion() {
    this.version = null;
    this.versionAsNumber = -1;
  }

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

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positive")
  private static long getVersionAsNumber(String version) {
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
        throw new IllegalArgumentException("Too large numbers");
      }
      return majorVersion << 10
          | minorVersion;
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Version " + version + " is not parseable.", ex);
    }
  }

  private static StackGresVersion ofVersion(String version) {
    return Stream.of(values())
        .filter(minorVersion -> version.startsWith(minorVersion.version + ".")
            || version.equals(minorVersion.version))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "Invalid version " + version));
  }

  public static StackGresVersion getStackGresVersion(StackGresConfig config) {
    return getStackGresVersionFromResource(config);
  }

  public static StackGresVersion getStackGresVersion(StackGresShardedCluster cluster) {
    return getStackGresVersionFromResource(cluster);
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

  public static StackGresVersion getStackGresVersion(StackGresScript script) {
    return getStackGresVersionFromResource(script);
  }

  public static StackGresVersion getStackGresVersion(StackGresShardedBackup backup) {
    return getStackGresVersionFromResource(backup);
  }

  public static StackGresVersion getStackGresVersion(StackGresShardedDbOps dbOps) {
    return getStackGresVersionFromResource(dbOps);
  }

  public static StackGresVersion getStackGresVersion(StackGresStream stream) {
    return getStackGresVersionFromResource(stream);
  }

  private static StackGresVersion getStackGresVersionFromResource(HasMetadata resource) {
    return Optional.of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .map(StackGresVersion::ofVersion)
        .orElse(StackGresVersion.LATEST);
  }

  public static long getStackGresVersionAsNumber(StackGresConfig config) {
    return getStackGresVersionFromResourceAsNumber(config);
  }

  public static long getStackGresVersionAsNumber(StackGresProfile profile) {
    return getStackGresVersionFromResourceAsNumber(profile);
  }

  public static long getStackGresVersionAsNumber(StackGresPostgresConfig postgresConfig) {
    return getStackGresVersionFromResourceAsNumber(postgresConfig);
  }

  public static long getStackGresVersionAsNumber(StackGresPoolingConfig poolingConfig) {
    return getStackGresVersionFromResourceAsNumber(poolingConfig);
  }

  public static long getStackGresVersionAsNumber(StackGresObjectStorage objectStorage) {
    return getStackGresVersionFromResourceAsNumber(objectStorage);
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

  public static long getStackGresVersionAsNumber(StackGresScript script) {
    return getStackGresVersionFromResourceAsNumber(script);
  }

  public static long getStackGresVersionAsNumber(StackGresShardedCluster config) {
    return getStackGresVersionFromResourceAsNumber(config);
  }

  public static long getStackGresVersionAsNumber(StackGresShardedBackup config) {
    return getStackGresVersionFromResourceAsNumber(config);
  }

  public static long getStackGresVersionAsNumber(StackGresShardedDbOps config) {
    return getStackGresVersionFromResourceAsNumber(config);
  }

  public static long getStackGresVersionAsNumber(StackGresStream config) {
    return getStackGresVersionFromResourceAsNumber(config);
  }

  public static long getStackGresVersionAsNumber(
      StackGresDistributedLogs distributedLogs) {
    return getStackGresVersionFromResourceAsNumber(distributedLogs);
  }

  public static long getStackGresVersionFromResourceAsNumber(HasMetadata resource) {
    return Optional.of(resource)
        .map(StackGresVersion::getStackGresRawVersionFromResource)
        .map(StackGresVersion::getVersionAsNumber)
        .orElseGet(StackGresVersion.LATEST::getVersionAsNumber);
  }

  public static String getStackGresRawVersionFromResource(HasMetadata resource) {
    return Optional.of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .orElseGet(StackGresVersion.LATEST::getVersion);
  }

}
