/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.factory.cluster.ClusterDefaultScripts;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.impl.DSL;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

public interface StackGresShardedClusterForDdpUtil extends StackGresShardedClusterUtil {

  Util UTIL = new Util();

  class Util extends StackGresShardedClusterForUtil {
    
    @Override
    void updateCoordinatorSpec(
        StackGresShardedCluster cluster,
        StackGresClusterSpec spec) {
      if (spec.getManagedSql() == null) {
        spec.setManagedSql(new StackGresClusterManagedSql());
      }
      spec.getManagedSql().setScripts(
          Seq.seq(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .limit(1))
          .append(new StackGresClusterManagedScriptEntryBuilder()
              .withSgScript(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
              .withId(1)
              .build())
          .append(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .skip(1))
          .toList());
    }

    @Override
    void updateShardsClusterSpec(
        StackGresShardedCluster cluster,
        StackGresClusterSpec spec,
        int index) {
      if (spec.getManagedSql() == null) {
        spec.setManagedSql(new StackGresClusterManagedSql());
      }
      spec.getManagedSql().setScripts(
          Seq.seq(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .limit(1))
          .append(new StackGresClusterManagedScriptEntryBuilder()
              .withSgScript(StackGresShardedClusterUtil.shardsScriptName(cluster))
              .withId(1)
              .build())
          .append(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .skip(1))
          .toList());
    }
  }

  static StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    return UTIL.getCoordinatorCluster(cluster);
  }

  static StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
    return UTIL.getShardsCluster(cluster, index);
  }
  
  static StackGresScript getCoordinatorScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
            .build())
        .editSpec()
        .withScripts(
            getDdpCreateDatabase(context, 0),
            getDdpInitScript(context, 1),
            getDdpUpdateShardsScript(context, 2))
        .endSpec()
        .build();
  }

  static StackGresScript getShardsScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.shardsScriptName(cluster))
            .build())
        .editSpec()
        .withScripts(
            getDdpCreateDatabase(context, 0))
        .endSpec()
        .build();
  }

  private static StackGresScriptEntry getDdpCreateDatabase(
      StackGresShardedClusterContext context, int id) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("ddp-create-database")
        .withRetryOnError(true)
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(ClusterDefaultScripts.class.getResource(
                "/ddp/ddp-create-database.sql"),
                StandardCharsets.UTF_8)
            .read()).get()
            .formatted(DSL.inline(cluster.getSpec().getDatabase())))
        .build();
    return script;
  }

  private static StackGresScriptEntry getDdpInitScript(
      StackGresShardedClusterContext context, int id) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("ddp-init")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(ClusterDefaultScripts.class.getResource(
                "/ddp/ddp--0.1.0.sql"),
                StandardCharsets.UTF_8)
            .read()).get())
        .build();
    return script;
  }

  private static StackGresScriptEntry getDdpUpdateShardsScript(
      StackGresShardedClusterContext context, int id) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("ddp-update-shards")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(getUpdateShardsSecretName(cluster))
        .withKey("ddp-update-shards.sql")
        .endSecretKeyRef()
        .endScriptFrom()
        .build();
    return script;
  }

  static Secret getUpdateShardsSecret(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var superuserCredentials = ShardedClusterSecret.getSuperuserCredentials(context);
    final Secret secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(getUpdateShardsSecretName(cluster))
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of("ddp-update-shards.sql",
            Unchecked.supplier(() -> Resources
                .asCharSource(ClusterDefaultScripts.class.getResource(
                    "/ddp/ddp-update-shards.sql"),
                    StandardCharsets.UTF_8)
                .read()).get().formatted(
                    cluster.getSpec().getShards().getClusters(),
                    DSL.inline("host '" + primaryShardServiceNamePlaceholder(cluster, "%1s") + "', "
                        + "port '" + PatroniUtil.POSTGRES_SERVICE_PORT + "', "
                        + "dbname '" + cluster.getSpec().getDatabase() + "'"),
                    DSL.inline("user '" + superuserCredentials.v1 + "', "
                        + "password '" + superuserCredentials.v2 + "'"),
                    DSL.inline(superuserCredentials.v1),
                    1000))))
        .build();
    return secret;
  }

  static String getUpdateShardsSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-update-shards";
  }

  private static String primaryShardServiceNamePlaceholder(
      StackGresShardedCluster cluster, String shardIndexPlaceholder) {
    return StackGresShardedClusterUtil.getShardClusterName(cluster, shardIndexPlaceholder);
  }
}
