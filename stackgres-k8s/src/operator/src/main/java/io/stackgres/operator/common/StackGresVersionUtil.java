/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.List;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.Component;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import org.jooq.lambda.Seq;

public interface StackGresVersionUtil {

  /**
   * The versions supported by a Postgres component: {@code latest}, the major versions and the
   * full versions.
   */
  static List<String> getSupportedPostgresVersions(Component component, StackGresContext context) {
    return Seq.of(StackGresComponent.LATEST)
        .append(component.streamOrderedMajorVersions(context))
        .append(component.streamOrderedVersions(context))
        .toList();
  }

  /**
   * The Postgres versions supported for the cluster, that is the ones of the Postgres flavor
   * component of the cluster (see {@link StackGresComponent#get(StackGresCluster)}): the ones
   * served by the docir repository of the cluster when {@code spec.configurations.registry.enabled}
   * is {@code true}, the ones bundled with the operator version of the cluster otherwise.
   */
  static List<String> getSupportedPostgresVersions(
      StackGresContext context,
      StackGresCluster cluster) {
    return getSupportedPostgresVersions(
        StackGresUtil.getPostgresFlavorComponent(cluster).get(cluster), context);
  }

  static List<String> getSupportedPostgresVersions(
      StackGresContext context,
      StackGresShardedCluster cluster) {
    return getSupportedPostgresVersions(
        StackGresUtil.getPostgresFlavorComponent(cluster).get(cluster), context);
  }

}
