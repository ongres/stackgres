/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class TemplatesConfigMap
    implements StackGresClusterResourceStreamFactory {

  private static final String TEMPLATES_SUFFIX = "-templates";

  private LabelFactory<StackGresCluster> labelFactory;

  public static String name(StackGresClusterContext context) {
    return ResourceUtil.resourceName(
        context.getCluster().getMetadata().getName() + TEMPLATES_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
    Map<String, String> data = new HashMap<String, String>();

    for (String resource : new String[] {
        ClusterStatefulSetPath.LOCAL_BIN_SHELL_UTILS_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_START_PATRONI_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_START_PATRONI_WITH_RESTORE_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_POST_INIT_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_EXEC_WITH_ENV_PATH.filename(),
        ClusterStatefulSetPath.ETC_PASSWD_PATH.filename(),
        ClusterStatefulSetPath.ETC_GROUP_PATH.filename(),
        ClusterStatefulSetPath.ETC_SHADOW_PATH.filename(),
        ClusterStatefulSetPath.ETC_GSHADOW_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RUN_DBOPS_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RESULT_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RUN_VACUUM_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RUN_REPACK_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RUN_MAJOR_VERSION_UPGRADE_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RUN_RESTART_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_COPY_BINARIES_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_INITIALIZE_SH_PATH.filename(),
    }) {
      data.put(resource, Unchecked.supplier(() -> Resources
          .asCharSource(ClusterStatefulSet.class.getResource("/templates/" + resource),
              StandardCharsets.UTF_8)
          .read()).get());
    }

    final StackGresCluster cluster = context.getCluster();
    ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.clusterLabels(cluster))
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withData(data)
        .build();
    return Seq.of(configMap);
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
