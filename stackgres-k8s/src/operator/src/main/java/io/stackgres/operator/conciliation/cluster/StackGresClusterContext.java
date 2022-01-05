/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.GenerationContext;
import io.stackgres.operator.conciliation.factory.PatroniScriptsConfigMap;
import org.immutables.value.Value;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple4;

@Value.Immutable
public interface StackGresClusterContext extends GenerationContext<StackGresCluster>,
    ClusterContext {

  @Override
  @Value.Derived
  default StackGresCluster getCluster() {
    return getSource();
  }

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  Optional<StackGresBackupConfig> getBackupConfig();

  StackGresPostgresConfig getPostgresConfig();

  StackGresProfile getStackGresProfile();

  Optional<StackGresPoolingConfig> getPoolingConfig();

  Optional<StackGresBackup> getRestoreBackup();

  List<StackGresClusterScriptEntry> getInternalScripts();

  Optional<Prometheus> getPrometheus();

  Optional<Secret> getDatabaseCredentials();

  @Value.Derived
  default List<Tuple4<StackGresClusterScriptEntry, Long, String, Long>> getIndexedScripts() {
    Seq<StackGresClusterScriptEntry> internalScripts =  Seq.seq(getInternalScripts());
    return internalScripts
        .zipWithIndex()
        .map(t -> t.concat(PatroniScriptsConfigMap.INTERNAL_SCRIPT))
        .append(Seq.of(Optional.ofNullable(
            getSource().getSpec().getInitData())
            .map(StackGresClusterInitData::getScripts))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .zipWithIndex()
            .map(t -> t.concat(PatroniScriptsConfigMap.SCRIPT)))
        .zipWithIndex()
        .map(t -> t.v1.concat(t.v2))
        .toList();
  }

}
