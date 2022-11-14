/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.io.Resources;
import io.stackgres.common.ClusterStatefulSetPath;
import org.jooq.lambda.Unchecked;

public abstract class AbstractPatroniTemplatesConfigMap<T>
    implements VolumeFactory<T> {

  public static final List<ClusterStatefulSetPath> TEMPLATE_PATHS = List.of(
      ClusterStatefulSetPath.LOCAL_BIN_SHELL_UTILS_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_START_PATRONI_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_PATRONICTL_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_POST_INIT_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_EXEC_WITH_ENV_PATH,
      ClusterStatefulSetPath.ETC_PASSWD_PATH,
      ClusterStatefulSetPath.ETC_GROUP_PATH,
      ClusterStatefulSetPath.ETC_SHADOW_PATH,
      ClusterStatefulSetPath.ETC_GSHADOW_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RUN_DBOPS_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RESULT_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RUN_VACUUM_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RUN_REPACK_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RUN_MAJOR_VERSION_UPGRADE_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RUN_RESTART_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_START_FLUENTBIT_SH_PATH,
      ClusterStatefulSetPath.LOCAL_BIN_START_POSTGRES_EXPORTER_SH_PATH);

  protected Map<String, String> getPatroniTemplates() {
    Map<String, String> data = new HashMap<>();

    for (String resource : TEMPLATE_PATHS
        .stream()
        .map(ClusterStatefulSetPath::filename)
        .toList()) {
      data.put(resource, Unchecked.supplier(() -> Resources
          .asCharSource(Objects.requireNonNull(AbstractPatroniTemplatesConfigMap.class
                  .getResource("/templates/" + resource)),
              StandardCharsets.UTF_8)
          .read()).get());
    }

    return data;
  }

}
