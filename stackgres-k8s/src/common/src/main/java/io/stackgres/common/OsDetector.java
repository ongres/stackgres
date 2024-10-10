/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import org.jetbrains.annotations.Nullable;

public class OsDetector {

  public static final String OS_LINUX = "linux";
  public static final String ARCH_X86_64 = "x86_64";
  public static final String ARCH_AARCH64 = "aarch64";
  public static final List<String> SUPPORTED_OSES = List.of(OS_LINUX);
  public static final List<String> SUPPORTED_ARCHS = List.of(ARCH_X86_64, ARCH_AARCH64);

  public static final OsDetector OS_DETECTOR = new OsDetector();

  public static String getClusterArch(@Nullable StackGresCluster cluster) {
    return Optional.ofNullable(cluster).map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getArch)
        .orElseGet(OS_DETECTOR::getArch);
  }

  public static Optional<String> getClusterArch(@Nullable StackGresCluster cluster,
      Optional<OsDetector> osDetector) {
    return Optional.ofNullable(cluster).map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getArch)
        .or(() -> osDetector.map(OsDetector::getArch));
  }

  public static String getClusterOs(@Nullable StackGresCluster cluster) {
    return Optional.ofNullable(cluster).map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getOs)
        .orElseGet(OS_DETECTOR::getOs);
  }

  public static Optional<String> getClusterOs(@Nullable StackGresCluster cluster,
      Optional<OsDetector> osDetector) {
    return Optional.ofNullable(cluster).map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getOs)
        .or(() -> osDetector.map(OsDetector::getOs));
  }

  public static String normalizeArch(final String arch) {
    final String archLowerCase = arch.toLowerCase(Locale.US);
    switch (archLowerCase) {
      case "amd64":
        return ARCH_X86_64;
      case "arm64":
        return ARCH_AARCH64;
      default:
        return archLowerCase;
    }
  }

  public String getArch() {
    final String arch = System.getProperty("os.arch");
    if (arch == null) {
      throw new RuntimeException("Can not detect architecture!");
    }
    return normalizeArch(arch);
  }

  public String getOs() {
    final String os = System.getProperty("os.name");
    if (os == null) {
      throw new RuntimeException("Can not detect operative system!");
    }
    return os.toLowerCase(Locale.US);
  }

}
