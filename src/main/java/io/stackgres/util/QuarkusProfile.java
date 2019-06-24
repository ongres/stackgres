/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.util;

import java.util.Locale;

import io.quarkus.runtime.configuration.ProfileManager;

/**
 * Type-safe enumeration of Quarkus profile.
 */
public enum QuarkusProfile {

  DEV,
  TEST,
  PROD;

  /**
   * Return the type-safe enumeration of the current Quarkus profile.
   *
   * @return Current QuarkusProfile enumeration
   */
  public static QuarkusProfile getActiveProfile() {
    for (QuarkusProfile p : values()) {
      if (p.toString().equals(ProfileManager.getActiveProfile())) {
        return p;
      }
    }
    return PROD;
  }

  /**
   * The enviroment is development, normally when run with quarkus:dev.
   *
   * @return true if is run with quarkus:dev
   */
  public boolean isDev() {
    return this == DEV;
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }

}
