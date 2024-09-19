/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.crd.sgconfig.StackGresConfig;

public enum ConfigPath implements EnvVarPathSource<StackGresConfig> {

  ETC_PATH("/etc"),
  ETC_COLLECTOR_PATH(ETC_PATH, "collector"),
  COLLECTOR_CONFIG_PATH(ETC_COLLECTOR_PATH, "config.yaml"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_START_OTEL_COLLECTOR_SH_PATH(LOCAL_BIN_PATH, "start-otel-collector.sh");

  private final String path;

  ConfigPath(String path) {
    this.path = path;
  }

  ConfigPath(String... paths) {
    this(String.join("/", paths));
  }

  ConfigPath(ConfigPath parent, String... paths) {
    this(parent.path, String.join("/", paths));
  }

  @Override
  public String rawPath() {
    return path;
  }

}
