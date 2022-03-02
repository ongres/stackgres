/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.application;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "stackgres.applications")
public interface ApplicationsConfig {

  BabelfishCompass babelfishCompass();

  interface BabelfishCompass {
    boolean enabled();
  }

}
