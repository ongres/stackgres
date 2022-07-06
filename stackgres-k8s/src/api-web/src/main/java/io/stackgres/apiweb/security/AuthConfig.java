/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "stackgres.auth")
public interface AuthConfig {

  AuthType type();

}
