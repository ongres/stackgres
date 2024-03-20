/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.crd.external.prometheus.PodMonitor;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgscript.StackGresScript;

public interface ReconciliationOperations {

  List<Class<? extends HasMetadata>> RESOURCES_ORDER = List.of(
      Secret.class,
      ConfigMap.class,
      ServiceAccount.class,
      Role.class,
      RoleBinding.class,
      Endpoints.class,
      Service.class,
      PodMonitor.class,
      Pod.class,
      Job.class,
      CronJob.class,
      StatefulSet.class,
      StackGresScript.class,
      StackGresPostgresConfig.class,
      StackGresCluster.class
      );

  Comparator<HasMetadata> RESOURCES_COMPARATOR = Comparator.comparingInt(
      resource -> Optional.of(RESOURCES_ORDER.indexOf(resource.getClass()))
          .filter(index -> index > -1)
          .orElseGet(RESOURCES_ORDER::size));

}
