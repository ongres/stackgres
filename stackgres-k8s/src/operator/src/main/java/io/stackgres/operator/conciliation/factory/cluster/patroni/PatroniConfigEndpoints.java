/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.PatroniUtil.REPLICATION_SERVICE_PORT;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.ongres.pgconfig.validator.GucValidator;
import com.ongres.pgconfig.validator.PgParameter;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterEnvVar;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.common.patroni.StandbyCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.PostgresSslSecret;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class PatroniConfigEndpoints
    implements ResourceGenerator<StackGresClusterContext> {

  private final ObjectMapper objectMapper;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PatroniConfigEndpoints(ObjectMapper objectMapper,
                                LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.objectMapper = objectMapper;
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    PatroniConfig patroniConf = getPatroniConfig(context);

    final String patroniConfigJson = objectMapper.valueToTree(patroniConf).toString();

    StackGresCluster cluster = context.getSource();
    return Stream.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(PatroniUtil.configName(context.getCluster()))
        .addToLabels(context.servicesCustomLabels())
        .addToLabels(labelFactory.clusterLabels(context.getSource()))
        .withAnnotations(Map.of(PatroniUtil.CONFIG_KEY, patroniConfigJson))
        .endMetadata()
        .build());
  }

  protected PatroniConfig getPatroniConfig(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    PatroniConfig patroniConf = new PatroniConfig();
    patroniConf.setTtl(30);
    patroniConf.setLoopWait(10);
    patroniConf.setRetryTimeout(10);
    if (getPostgresFlavorComponent(cluster) != StackGresComponent.BABELFISH) {
      patroniConf.setCheckTimeline(true);
    }
    patroniConf.setSynchronousMode(
        cluster.getSpec().getReplication().isSynchronousMode()
        || (cluster.getSpec().getReplication().isSynchronousModeAll()
            && cluster.getSpec().getInstances() > 1));
    patroniConf.setSynchronousModeStrict(
        cluster.getSpec().getReplication().isStrictSynchronousMode()
        || (cluster.getSpec().getReplication().isStrictSynchronousModeAll()
            && cluster.getSpec().getInstances() > 1));
    patroniConf.setSynchronousNodeCount(
        cluster.getSpec().getReplication().isSynchronousModeAll()
        && cluster.getSpec().getInstances() > 1
        ? Integer.valueOf(cluster.getSpec().getInstances() - 1)
            : cluster.getSpec().getReplication().getSyncInstances());

    context.getReplicateCluster()
        .ifPresent(replicateCluster -> {
          patroniConf.setStandbyCluster(new StandbyCluster());
          patroniConf.getStandbyCluster().setHost(PatroniUtil.readWriteName(replicateCluster));
          patroniConf.getStandbyCluster().setPort(
              String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT));
          patroniConf.getStandbyCluster().setRestoreCommand("exec-with-env '"
              + ClusterEnvVar.REPLICATE_ENV.value(context.getSource()) + "'"
              + " -- wal-g wal-fetch %f %p");
          patroniConf.getStandbyCluster().setCreateReplicaMethods(
              Seq.<String>of()
              .append(Seq.of("replicate")
                  .filter(createReplicaMethod -> Optional
                      .ofNullable(cluster.getSpec())
                      .map(StackGresClusterSpec::getInitialData)
                      .map(StackGresClusterInitialData::getRestore)
                      .map(StackGresClusterRestore::getFromBackup)
                      .isPresent()))
              .append("basebackup")
              .toList());
        });

    Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getExternal)
        .ifPresent(external -> {
          patroniConf.setStandbyCluster(new StandbyCluster());
          patroniConf.getStandbyCluster().setHost(external.getHost());
          patroniConf.getStandbyCluster().setPort(String.valueOf(external.getPort()));
        });

    Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getStorage)
        .ifPresent(storage -> {
          if (patroniConf.getStandbyCluster() == null) {
            patroniConf.setStandbyCluster(new StandbyCluster());
          }
          if (Optional.ofNullable(cluster.getSpec())
                  .map(StackGresClusterSpec::getReplicateFrom)
                  .map(StackGresClusterReplicateFrom::getInstance)
                  .map(StackGresClusterReplicateFromInstance::getExternal)
                  .isEmpty()
              && Optional.ofNullable(cluster.getStatus())
                  .map(StackGresClusterStatus::getOs)
                  .isEmpty()) {
            patroniConf.getStandbyCluster().setHost(PatroniServices.readWriteName(context));
            patroniConf.getStandbyCluster().setPort(String.valueOf(REPLICATION_SERVICE_PORT));
          }
          patroniConf.getStandbyCluster().setRestoreCommand("exec-with-env '"
              + ClusterEnvVar.REPLICATE_ENV.value(context.getSource()) + "'"
              + " -- wal-g wal-fetch %f %p");
          patroniConf.getStandbyCluster().setCreateReplicaMethods(
              Seq.<String>of()
              .append(Seq.of("replicate")
                  .filter(createReplicaMethod -> Optional
                      .ofNullable(cluster.getSpec())
                      .map(StackGresClusterSpec::getInitialData)
                      .map(StackGresClusterInitialData::getRestore)
                      .map(StackGresClusterRestore::getFromBackup)
                      .isPresent()))
              .append(Seq.of("basebackup")
                  .filter(createReplicaMethod -> Optional
                      .ofNullable(cluster.getSpec())
                      .map(StackGresClusterSpec::getReplicateFrom)
                      .map(StackGresClusterReplicateFrom::getInstance)
                      .map(StackGresClusterReplicateFromInstance::getExternal)
                      .isPresent()))
              .toList());
        });

    patroniConf.setPostgresql(new PatroniConfig.PostgreSql());
    patroniConf.getPostgresql().setUsePgRewind(true);
    patroniConf.getPostgresql().setUseSlots(true);
    patroniConf.getPostgresql().setParameters(getPostgresConfigValues(context));
    patroniConf.getPostgresql().setRecoveryConf(getPostgresRecoveryConfigValues(context));
    return patroniConf;
  }

  protected Map<String, String> getPostgresConfigValues(StackGresClusterContext context) {
    StackGresPostgresConfig pgConfig = context.getPostgresConfig();

    Map<String, String> params = getPostgresParameters(context, pgConfig);

    return normalizeParams(pgConfig.getSpec().getPostgresVersion(), params);
  }

  protected Map<String, String> getPostgresRecoveryConfigValues(StackGresClusterContext context) {
    StackGresPostgresConfig pgConfig = context.getPostgresConfig();

    Map<String, String> params = getPostgresRecoveryParameters(context);

    return normalizeParams(pgConfig.getSpec().getPostgresVersion(), params);
  }

  private Map<String, String> normalizeParams(String postgresVersion,
      Map<String, String> params) {
    final GucValidator val = GucValidator.forVersion(postgresVersion);
    final var builder = ImmutableMap.<String, String>builderWithExpectedSize(params.size());
    params.forEach((name, setting) -> {
      PgParameter parameter = val.parameter(name, setting);
      builder.put(parameter.getName(), parameter.getSetting());
    });
    return builder.build();
  }

  private Map<String, String> getPostgresParameters(StackGresClusterContext context,
      StackGresPostgresConfig pgConfig) {
    final String version = pgConfig.getSpec().getPostgresVersion();
    Map<String, String> params = new HashMap<>(PostgresDefaultValues.getDefaultValues(
        StackGresVersion.getStackGresVersion(context.getCluster()), version));
    Map<String, String> userParams = pgConfig.getSpec().getPostgresqlConf();
    PostgresBlocklist.getBlocklistParameters().forEach(userParams::remove);
    params.putAll(userParams);

    params.put("port", String.valueOf(EnvoyUtil.PG_PORT));

    if (isBackupConfigurationPresent(context)) {
      params.put("archive_command",
          "exec-with-env '" + ClusterEnvVar.BACKUP_ENV.value(context
              .getSource()) + "'"
              + " -- wal-g wal-push %p");
    } else {
      params.put("archive_command", "/bin/true");
    }

    if (Optional.ofNullable(context.getSource())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getDistributedLogs)
        .map(StackGresClusterDistributedLogs::getSgDistributedLogs).isPresent()) {
      params.put("logging_collector", "on");
      params.put("log_destination", "csvlog");
      params.put("log_directory", ClusterPath.PG_LOG_PATH.path());
      params.put("log_filename", "postgres-%M.log");
      params.put("log_rotation_age", "30min");
      params.put("log_rotation_size", "0kB");
      params.put("log_truncate_on_rotation", "on");
    }

    if (getPostgresFlavorComponent(context.getSource()) == StackGresComponent.BABELFISH) {
      params.put("shared_preload_libraries", Optional.ofNullable(
          params.get("shared_preload_libraries"))
          .map(sharedPreloadLibraries -> "babelfishpg_tds, " + sharedPreloadLibraries)
          .orElse("babelfishpg_tds"));
    }

    if (Optional.of(context.getSource())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false)) {
      params.put("ssl", "on");
      params.put("ssl_cert_file",
          ClusterPath.SSL_PATH.path() + "/" + PostgresSslSecret.CERTIFICATE_KEY);
      params.put("ssl_key_file",
          ClusterPath.SSL_PATH.path() + "/" + PostgresSslSecret.PRIVATE_KEY_KEY);
    }

    return params;
  }

  private Map<String, String> getPostgresRecoveryParameters(StackGresClusterContext context) {
    Map<String, String> params = new HashMap<>();

    if (isBackupConfigurationPresent(context)) {
      params.put("restore_command",
          "exec-with-env '" + ClusterEnvVar.BACKUP_ENV.value(context
              .getSource()) + "'"
              + " -- wal-g wal-fetch %f %p");
    }

    return params;
  }

  private boolean isBackupConfigurationPresent(StackGresClusterContext context) {
    return context.getBackupStorage()
        .isPresent();
  }

}
