/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.StatefulSetComparator;

@ReconciliationScope(value = StackGresCluster.class, kind = "StatefulSet")
@ApplicationScoped
public class ClusterStatefulSetComparator extends StatefulSetComparator {

  private final ResourceScanner<Pod> podScanner;

  @Inject
  public ClusterStatefulSetComparator(
      ResourceScanner<Pod> podScanner) {
    this.podScanner = podScanner;
  }

  public ClusterStatefulSetComparator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.podScanner = null;
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {
    final String namespace = required.getMetadata().getNamespace();
    Map<String, String> primaryLabels = new ImmutableMap.Builder<String, String>()
        .putAll(ClusterLabelFactory.patroniClusterLabels(required))
        .put(StackGresContext.ROLE_KEY, StackGresContext.PRIMARY_ROLE)
        .build();
    var pods = podScanner.findByLabelsAndNamespace(namespace, primaryLabels);
    if (pods.isEmpty()) {
      return false;
    }
    return super.isResourceContentEqual(required, deployed);
  }

}
