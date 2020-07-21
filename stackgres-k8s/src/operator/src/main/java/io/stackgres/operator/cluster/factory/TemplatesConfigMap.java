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
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
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
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    Map<String, String> data = new HashMap<String, String>();

    for (String resource : new String[] {
        ClusterStatefulSetPath.LOCAL_BIN_START_PATRONI_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_START_PATRONI_WITH_RESTORE_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_POST_INIT_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH.filename(),
        ClusterStatefulSetPath.LOCAL_BIN_EXEC_WITH_ENV_PATH.filename(),
        ClusterStatefulSetPath.ETC_PASSWD_PATH.filename(),
        ClusterStatefulSetPath.ETC_GROUP_PATH.filename(),
        ClusterStatefulSetPath.ETC_SHADOW_PATH.filename(),
        ClusterStatefulSetPath.ETC_GSHADOW_PATH.filename()
    }) {
      data.put(resource, Unchecked.supplier(() -> Resources
          .asCharSource(ClusterStatefulSet.class.getResource("/templates/" + resource),
              StandardCharsets.UTF_8)
          .read()).get());
    }

    final StackGresCluster cluster = clusterContext.getCluster();
    ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(clusterContext))
        .withLabels(labelFactory.clusterLabels(cluster))
        .withOwnerReferences(clusterContext.getOwnerReferences())
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
