/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import javax.annotation.Nonnull;

public enum StackGresClusterProfile {

  PRODUCTION("production",
      new StackGresClusterSpecBuilder()
      .withNewNonProductionOptions()
      .withDisableClusterPodAntiAffinity(false)
      .withDisablePatroniResourceRequirements(false)
      .withDisableClusterResourceRequirements(false)
      .endNonProductionOptions()
      .build()),
  TESTING("testing",
      new StackGresClusterSpecBuilder()
      .withNewNonProductionOptions()
      .withDisableClusterPodAntiAffinity(true)
      .withDisablePatroniResourceRequirements(false)
      .withDisableClusterResourceRequirements(false)
      .endNonProductionOptions()
      .build()),
  DEVELOPMENT("development",
      new StackGresClusterSpecBuilder()
      .withNewNonProductionOptions()
      .withDisableClusterPodAntiAffinity(true)
      .withDisablePatroniResourceRequirements(true)
      .withDisableClusterResourceRequirements(true)
      .endNonProductionOptions()
      .build());

  private final String profile;
  private final StackGresClusterSpec spec;

  StackGresClusterProfile(String profile,
      StackGresClusterSpec spec) {
    this.profile = profile;
    this.spec = spec;
  }

  public StackGresClusterSpec spec() {
    return spec;
  }

  @Override
  public @Nonnull String toString() {
    return profile;
  }

  public static @Nonnull StackGresClusterProfile fromString(@Nonnull String name) {
    return switch (name) {
      case "production" -> PRODUCTION;
      case "testing" -> TESTING;
      case "development" -> DEVELOPMENT;
      default -> throw new IllegalArgumentException("Unknown profile " + name);
    };
  }

}
