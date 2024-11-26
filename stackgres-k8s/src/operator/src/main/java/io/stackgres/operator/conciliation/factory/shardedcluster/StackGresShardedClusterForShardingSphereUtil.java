/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.external.shardingsphere.ComputeNode;
import io.stackgres.common.crd.external.shardingsphere.ComputeNodeBuilder;
import io.stackgres.common.crd.external.shardingsphere.ComputeNodePortBindingBuilder;
import io.stackgres.common.crd.external.shardingsphere.ComputeNodeUserBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigShardingSphere;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereAuthority;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereEtcd;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereMode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSpherePrivilege;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereRepository;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereZooKeeper;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingSphereRepositoryType;
import io.stackgres.operator.conciliation.factory.cluster.ClusterDefaultScripts;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.impl.DSL;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public interface StackGresShardedClusterForShardingSphereUtil extends StackGresShardedClusterUtil {

  Util UTIL = new Util();

  class Util extends StackGresShardedClusterForUtil {
    @Override
    protected void updateCoordinatorSpec(StackGresShardedCluster cluster, StackGresClusterSpec spec) {
    }

    @Override
    protected void updateShardsClusterSpec(StackGresShardedCluster cluster, StackGresClusterSpec spec, int index) {
      if (spec.getManagedSql() == null) {
        spec.setManagedSql(new StackGresClusterManagedSql());
      }
      spec.getManagedSql().setScripts(
          Seq.seq(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .limit(1))
          .append(Seq.of(new StackGresClusterManagedScriptEntryBuilder()
              .withSgScript(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
              .withId(1)
              .build())
              .filter(ignore -> index == 0))
          .append(new StackGresClusterManagedScriptEntryBuilder()
              .withSgScript(StackGresShardedClusterUtil.shardsScriptName(cluster))
              .withId(2)
              .build())
          .append(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .skip(1))
          .append(Optional.ofNullable(cluster.getSpec().getCoordinator().getManagedSql())
              .filter(ignore -> index == 0)
              .map(StackGresClusterManagedSql::getScripts)
              .stream()
              .flatMap(List::stream)
              .map(script -> new StackGresClusterManagedScriptEntryBuilder(script)
                .withId(script.getId() + 1000000)
                .build()))
          .toList());
    }
  }

  static StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    return UTIL.getCoordinatorCluster(cluster);
  }

  static StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
    return UTIL.getShardsCluster(cluster, index);
  }
  
  static ComputeNode getCoordinatorComputeNode(
      StackGresShardedClusterContext context,
      Map<String, String> coordinatorLabelsWithoutUid) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var superuserCredentials = ShardedClusterSecret.getSuperuserCredentials(context);
    var shardingSphere = cluster.getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getShardingSphere();
    return
        new ComputeNodeBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
            .build())
        .editSpec()
        .withServerVersion("5.4.1")
        .withNewStorageNodeConnector()
        .withType("postgresql")
        .withVersion("42.4.3")
        .endStorageNodeConnector()
        .withSelector(new LabelSelectorBuilder()
            .withMatchLabels(coordinatorLabelsWithoutUid)
            .build())
        .withReplicas(cluster.getSpec().getCoordinator().getInstances())
        .withPortBindings(new ComputeNodePortBindingBuilder()
            .withName(EnvoyUtil.POSTGRES_PORT_NAME)
            .withContainerPort(5432)
            .withServicePort(PatroniUtil.POSTGRES_SERVICE_PORT)
            .withProtocol("TCP")
            .build())
        .withServiceType(cluster.getSpec().getPostgresServices().getCoordinator()
            .getPrimary().getType())
        .withNewBootstrap()
        .withNewServerConfig()
        .withProps(Seq.of(Map.ofEntries(
            Map.entry("proxy-frontend-database-protocol-type", "PostgreSQL"),
            Map.entry("proxy-default-port", "5432")))
            .flatMap(map -> Seq.seq(map)
                .append(Optional.of(shardingSphere)
                    .map(StackGresShardedClusterShardingSphere::getMode)
                    .map(StackGresShardedClusterShardingSphereMode::getProperties)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .filter(e -> !map.containsKey(e.getKey()))
                    .map(e -> Tuple.tuple(e.getKey(), e.getValue()))))
            .toMap(Tuple2::v1, Tuple2::v2))
        .withNewMode()
        .withType(shardingSphere.getMode().getType())
        .withNewRepository()
        .withType(shardingSphere.getMode().getRepository().getType())
        .withProps(getCoordinatorRepositoryProps(shardingSphere.getMode().getRepository()))
        .endRepository()
        .endMode()
        .withNewAuthority()
        .withNewPrivilege()
        .withType(Optional.of(shardingSphere)
            .map(StackGresShardedClusterShardingSphere::getAuthority)
            .map(StackGresShardedClusterShardingSphereAuthority::getPrivilege)
            .map(StackGresShardedClusterShardingSpherePrivilege::getType)
            .orElse(null))
        .endPrivilege()
        .withUsers(Seq.of(new ComputeNodeUserBuilder()
            .withUser(superuserCredentials.v1)
            .withPassword(superuserCredentials.v2)
            .build())
            .append(context.getShardingSphereAuthorityUsers()
                .stream()
                .map(user -> new ComputeNodeUserBuilder()
                    .withUser(user.v1)
                    .withPassword(user.v2)
                    .build()))
            .toList())
        .endAuthority()
        .endServerConfig()
        .endBootstrap()
        .endSpec()
        .build();
  }

  private static Map<String, String> getCoordinatorRepositoryProps(
      StackGresShardedClusterShardingSphereRepository repository) {
    switch (StackGresShardingSphereRepositoryType.fromString(repository.getType())) {
      case ETCD:
        var etcd = Optional.of(repository)
            .map(StackGresShardedClusterShardingSphereRepository::getEtcd);
        return Seq.of(Seq.<Tuple2<String, String>>of()
            .append(etcd
                .map(StackGresShardedClusterShardingSphereEtcd::getServerList)
                .map(value -> Tuple.tuple("server-lists", Seq.seq(value).toString(","))))
            .toMap(Tuple2::v1, Tuple2::v2))
            .flatMap(map -> Seq.seq(map)
                .append(Optional.of(repository)
                    .map(StackGresShardedClusterShardingSphereRepository::getProperties)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .filter(e -> !map.containsKey(e.getKey()))
                    .map(e -> Tuple.tuple(e.getKey(), e.getValue()))))
            .toMap(Tuple2::v1, Tuple2::v2);
      case ZOO_KEEPER:
        var zooKeeper = Optional.of(repository)
            .map(StackGresShardedClusterShardingSphereRepository::getZooKeeper);
        return Seq.of(Seq.<Tuple2<String, String>>of()
            .append(zooKeeper
                .map(StackGresShardedClusterShardingSphereZooKeeper::getServerList)
                .map(value -> Tuple.tuple("server-lists", Seq.seq(value).toString(","))))
            .toMap(Tuple2::v1, Tuple2::v2))
            .flatMap(map -> Seq.seq(map)
                .append(Optional.of(repository)
                    .map(StackGresShardedClusterShardingSphereRepository::getProperties)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .filter(e -> !map.containsKey(e.getKey()))
                    .map(e -> Tuple.tuple(e.getKey(), e.getValue()))))
            .toMap(Tuple2::v1, Tuple2::v2);
      default:
        break;
    }
    return null;
  }

  static StackGresScript getCoordinatorScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return
        new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
            .build())
        .editSpec()
        .withScripts(
            getShardingSphereCreateDatabaseScript(context),
            getShardingSphereInitScript(context),
            getShardingSphereUpdateShardsScript(context))
        .endSpec()
        .build();
  }

  static StackGresScript getShardsScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return
        new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.shardsScriptName(cluster))
            .build())
        .editSpec()
        .withScripts(
            getShardingSphereCreateDatabaseScript(context))
        .endSpec()
        .build();
  }

  private static StackGresScriptEntry getShardingSphereCreateDatabaseScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withName("shardingsphere-create-database")
        .withRetryOnError(true)
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(ClusterDefaultScripts.class.getResource(
                "/shardingsphere/shardingsphere-create-database.sql"),
                StandardCharsets.UTF_8)
            .read()).get()
            .formatted(DSL.inline(cluster.getSpec().getDatabase())))
        .build();
    return script;
  }

  private static StackGresScriptEntry getShardingSphereInitScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withName("shardingsphere-init")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(getInitSecretName(cluster))
        .withKey("shardingsphere-init.sql")
        .endSecretKeyRef()
        .endScriptFrom()
        .build();
    return script;
  }

  static Secret getInitSecret(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var superuserCredentials = ShardedClusterSecret.getSuperuserCredentials(context);
    final Secret secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(getInitSecretName(cluster))
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of("shardingsphere-init.sql",
            Unchecked.supplier(() -> Resources
                .asCharSource(ClusterDefaultScripts.class.getResource(
                    "/shardingsphere/shardingsphere-init.sql"),
                    StandardCharsets.UTF_8)
                .read()).get().formatted(
                    DSL.inline(StackGresShardedClusterUtil.primaryCoordinatorServiceName(cluster)),
                    DSL.inline(String.valueOf(PatroniUtil.POSTGRES_SERVICE_PORT)),
                    DSL.inline(cluster.getSpec().getDatabase()),
                    DSL.inline(superuserCredentials.v1),
                    DSL.inline(superuserCredentials.v2),
                    superuserCredentials.v1))))
        .build();
    return secret;
  }

  private static StackGresScriptEntry getShardingSphereUpdateShardsScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withName("shardingsphere-update-shards")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(getUpdateShardsSecretName(cluster))
        .withKey("shardingsphere-update-shards.sql")
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
        .withData(ResourceUtil.encodeSecret(Map.of("shardingsphere-update-shards.sql",
            Unchecked.supplier(() -> Resources
                .asCharSource(ClusterDefaultScripts.class.getResource(
                    "/shardingsphere/shardingsphere-update-shards.sql"),
                    StandardCharsets.UTF_8)
                .read()).get().formatted(
                    cluster.getSpec().getShards().getClusters(),
                    DSL.inline(primaryShardServiceNamePlaceholder(cluster, "%1s")),
                    PatroniUtil.POSTGRES_SERVICE_PORT,
                    DSL.inline(cluster.getSpec().getDatabase()),
                    DSL.inline(superuserCredentials.v1),
                    DSL.inline(superuserCredentials.v2)))))
        .build();
    return secret;
  }

  static String getInitSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-init";
  }

  static String getUpdateShardsSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-update-shards";
  }

  private static String primaryShardServiceNamePlaceholder(
      StackGresShardedCluster cluster, String shardIndexPlaceholder) {
    return StackGresShardedClusterUtil.getShardClusterName(cluster, shardIndexPlaceholder);
  }

  static Role getShardingSphereOperatorRole(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return
        new RoleBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.getCoordinatorClusterName(cluster))
            .build())
        .withRules(List.of(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(StackGresShardedCluster.class))
            .withResources(HasMetadata.getPlural(StackGresShardedCluster.class) + "/finalizers")
            .withVerbs("update")
            .build()))
        .build();
  }

  static RoleBinding getShardingSphereOperatorRoleBinding(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var shardingSphereServiceAccount = Optional.of(context.getShardedCluster().getSpec().getCoordinator())
        .map(StackGresShardedClusterCoordinator::getConfigurationsForCoordinator)
        .map(StackGresShardedClusterCoordinatorConfigurations::getShardingSphere)
        .map(StackGresShardedClusterShardingSphere::getServiceAccount)
        .or(() -> Optional.of(context.getConfig().getSpec())
            .map(StackGresConfigSpec::getShardingSphere)
            .map(StackGresConfigShardingSphere::getServiceAccount))
        .orElseThrow(() -> new IllegalArgumentException(
            "You must configure any of the SGShardedCluster.spec.shardingSphere or SGConfig.spec.shardingSphere "
                + " section specifying the service account used by the ShardingSphere operator"
                + " in order to use shardingsphere sharding technology"));
    return
        new RoleBindingBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.getCoordinatorClusterName(cluster))
            .build())
        .withSubjects(List.of(new SubjectBuilder()
            .withApiGroup(HasMetadata.getGroup(ServiceAccount.class))
            .withKind(HasMetadata.getKind(ServiceAccount.class))
            .withNamespace(shardingSphereServiceAccount.getNamespace())
            .withName(shardingSphereServiceAccount.getName())
            .build()))
        .withNewRoleRef()
        .withApiGroup(HasMetadata.getGroup(Role.class))
        .withKind(HasMetadata.getKind(Role.class))
        .withName(StackGresShardedClusterUtil.getCoordinatorClusterName(cluster))
        .endRoleRef()
        .build();
  }
}
