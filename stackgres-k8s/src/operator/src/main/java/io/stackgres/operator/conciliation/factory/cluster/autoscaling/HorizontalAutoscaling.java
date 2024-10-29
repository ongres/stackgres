/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.autoscaling;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.external.keda.ScaledObjectBuilder;
import io.stackgres.common.crd.external.keda.TriggerAuthenticationBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscaling;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscalingHorizontal;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class HorizontalAutoscaling implements ResourceGenerator<StackGresClusterContext> {

  public static String name(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName());
  }

  public static String secretName(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + "-autoscaling");
  }

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public HorizontalAutoscaling(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    return Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getAutoscaling)
        .filter(StackGresClusterAutoscaling::isHorizontalPodAutoscalingEnabled)
        .stream()
        .flatMap(autoscaling -> Stream.of(
            new SecretBuilder()
            .withNewMetadata()
            .withLabels(labelFactory.genericLabels(cluster))
            .withName(secretName(cluster))
            .withNamespace(cluster.getMetadata().getNamespace())
            .endMetadata()
            .withData(Optional.of(PatroniSecret.getSuperuserCredentials(context))
                .map(credentials -> "postgresql://"
                    + credentials.v1 + ":" + credentials.v2
                    + "@" + PatroniUtil.readOnlyName(cluster)
                    + "." + cluster.getMetadata().getNamespace()
                    + ":" + String.valueOf(EnvoyUtil.PG_PORT)
                    + "/postgres?sslmode=prefer&default_query_exec_mode=exec")
                .map(connection -> Map.of("connection", connection))
                .map(ResourceUtil::encodeSecret)
                .get())
            .build(),
            new TriggerAuthenticationBuilder()
            .withNewMetadata()
            .withLabels(labelFactory.genericLabels(cluster))
            .withName(name(cluster))
            .withNamespace(cluster.getMetadata().getNamespace())
            .endMetadata()
            .withNewSpec()
            .addNewSecretTargetRef()
            .withName(secretName(cluster))
            .withKey("connection")
            .withParameter("connection")
            .endSecretTargetRef()
            .endSpec()
            .build(),
            new ScaledObjectBuilder()
            .withNewMetadata()
            .withLabels(labelFactory.genericLabels(cluster))
            .withName(name(cluster))
            .withNamespace(cluster.getMetadata().getNamespace())
            .endMetadata()
            .withNewSpec()
            .withNewScaleTargetRef()
            .withApiVersion(HasMetadata.getApiVersion(StackGresCluster.class))
            .withKind(HasMetadata.getKind(StackGresCluster.class))
            .withName(cluster.getMetadata().getName())
            .endScaleTargetRef()
            .withCooldownPeriod(Optional.ofNullable(autoscaling.getHorizontal())
                .map(StackGresClusterAutoscalingHorizontal::getCooldownPeriod)
                .orElse(300))
            .withPollingInterval(Optional.ofNullable(autoscaling.getHorizontal())
                .map(StackGresClusterAutoscalingHorizontal::getPollingInterval)
                .orElse(30))
            .withMinReplicaCount(autoscaling.getMinInstances())
            .withMaxReplicaCount(autoscaling.getMaxInstances())
            .addNewTrigger()
            .withNewAuthenticationRef()
            .withName(name(cluster))
            .endAuthenticationRef()
            .withType("postgresql")
            .withName("connections-used")
            .withMetadata(Map.ofEntries(
                Map.entry(
                    "targetQueryValue",
                    Optional.ofNullable(autoscaling.getHorizontal())
                    .map(StackGresClusterAutoscalingHorizontal::getReplicasConnectionsUsageTarget)
                    .orElse("0.8")),
                Map.entry(
                    "query",
                    Optional.of(cluster.getSpec().getPods())
                    .map(StackGresClusterPods::getDisableConnectionPooling)
                    .orElse(false)
                    ?
                    """
                    WITH
                      max_connections (size) AS (
                        SELECT setting::numeric
                        FROM pg_settings
                        WHERE name = 'max_connections'),
                      active_connections (size) AS (
                        SELECT COUNT(*)::numeric
                        FROM pg_stat_activity
                        WHERE state = 'active'
                        AND backend_type = 'client backend')
                    SELECT active_connections.size / max_connections.size AS connection_usage
                    FROM max_connections, active_connections; 
                    """
                    :
                    """
                    WITH
                      max_connections (size) AS (
                        SELECT SUM(_.pool_size)::numeric
                        FROM dblink(
                          'host=/var/run/postgresql port=6432 dbname=pgbouncer user=pgbouncer',
                          'show databases'::text)
                        AS _(
                          name text, host text, port integer, database text, force_user text,
                          pool_size integer, min_pool_size integer, reserve_pool integer, server_lifetime integer,
                          pool_mode text, max_connections integer, current_connections integer,
                          paused boolean, disabled boolean)),
                      active_connections (size) AS (
                        SELECT SUM(_.current_connections)::numeric
                        FROM dblink(
                          'host=/var/run/postgresql port=6432 dbname=pgbouncer user=pgbouncer',
                          'show databases'::text)
                        AS _(
                          name text, host text, port integer, database text, force_user text,
                          pool_size integer, min_pool_size integer, reserve_pool integer, server_lifetime integer,
                          pool_mode text, max_connections integer, current_connections integer,
                          paused boolean, disabled boolean))
                    SELECT active_connections.size / max_connections.size AS connection_usage
                    FROM max_connections, active_connections; 
                    """)))
            .withMetricType(Optional.ofNullable(autoscaling.getHorizontal())
                .map(StackGresClusterAutoscalingHorizontal::getReplicasConnectionsUsageMetricType)
                .orElse("AverageValue"))
            .endTrigger()
            .endSpec()
            .build()));
  }

}
