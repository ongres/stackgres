/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public enum StreamPath implements EnvVarPathSource<StackGresCluster> {

  ETC_PASSWD_PATH("/etc/passwd"),
  ETC_GROUP_PATH("/etc/group"),
  ETC_SHADOW_PATH("/etc/shadow"),
  ETC_GSHADOW_PATH("/etc/gshadow"),
  ETC_POSTGRES_PATH("/etc/postgresql"),
  SHARED_MEMORY_PATH("/dev/shm"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  DEBEZIUM_BASE_PATH("/var/lib/debezium"),
  DEBEZIUM_OFFSET_STORAGE_PATH(DEBEZIUM_BASE_PATH, "offsets.dat"),
  DEBEZIUM_DATABASE_HISTORY_PATH(DEBEZIUM_BASE_PATH, "dbhistory.dat"),
  DEBEZIUM_ANNOTATION_SIGNAL_PATH(DEBEZIUM_BASE_PATH, "annotation-signal.properties");

  private final String path;

  StreamPath(String path) {
    this.path = path;
  }

  StreamPath(String... paths) {
    this(String.join("/", paths));
  }

  StreamPath(StreamPath parent, String... paths) {
    this(parent.path, String.join("/", paths));
  }

  @Override
  public String rawPath() {
    return path;
  }

  public static List<EnvVar> envVars(ClusterContext context) {
    return Arrays
        .stream(values())
        .map(path -> path.envVar(context))
        .toList();
  }

}
