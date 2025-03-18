/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniDynamicConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.common.patroni.PostgreSql;
import io.stackgres.common.patroni.StandbyCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresDefaultValues;
import io.stackgres.operator.initialization.DefaultClusterPostgresConfigFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class PatroniConfigEndpoints
    implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;
  private final ObjectMapper objectMapper;
  private final YAMLMapper yamlMapper;
  private final DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory;

  @Inject
  public PatroniConfigEndpoints(
      LabelFactoryForCluster labelFactory,
      ObjectMapper objectMapper,
      YamlMapperProvider yamlMapperProvider,
      DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory) {
    this.labelFactory = labelFactory;
    this.objectMapper = objectMapper;
    this.yamlMapper = yamlMapperProvider.get();
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
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

  public String getPatroniConfigAsYamlString(StackGresClusterContext context) {
    try {
      return yamlMapper.writeValueAsString(getPatroniConfig(context));
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  PatroniConfig getPatroniConfig(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    final StackGresPostgresConfig pgConfig = context.getPostgresConfig()
        .orElseGet(() -> defaultPostgresConfigFactory.buildResource(cluster));
    final boolean isBackupConfigurationPresent = context.getBackupStorage().isPresent();
    final boolean isReplicateFromPresent = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .isPresent()
        || Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getStorage)
        .isPresent();
    final Optional<StackGresCluster> replicateClusterOptional = context.getReplicateCluster();
    final PatroniConfig patroniConf = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getDynamicConfig)
        .<JsonNode>map(objectMapper::valueToTree)
        .map(Unchecked.function(config -> objectMapper.treeToValue(config, PatroniConfig.class)))
        .orElseGet(PatroniConfig::new);
    if (patroniConf.getTtl() == null) {
      patroniConf.setTtl(30);
    }
    if (patroniConf.getLoopWait() == null) {
      patroniConf.setLoopWait(10);
    }
    if (patroniConf.getRetryTimeout() == null) {
      patroniConf.setRetryTimeout(10);
    }
    patroniConf.setFailsafeMode(null);
    patroniConf.setStandbyCluster(null);
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

    replicateClusterOptional
        .ifPresent(replicateCluster -> {
          patroniConf.setStandbyCluster(new StandbyCluster());
          patroniConf.getStandbyCluster().setHost(PatroniUtil.readWriteName(replicateCluster));
          patroniConf.getStandbyCluster().setPort(
              String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT));
          patroniConf.getStandbyCluster().setRestoreCommand("exec-with-env '"
              + ClusterEnvVar.REPLICATE_ENV.value(cluster) + "'"
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
            patroniConf.getStandbyCluster().setHost(PatroniUtil.readWriteName(cluster));
            patroniConf.getStandbyCluster().setPort(
                String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT));
          }
          patroniConf.getStandbyCluster().setRestoreCommand("exec-with-env '"
              + ClusterEnvVar.REPLICATE_ENV.value(cluster) + "'"
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

    patroniConf.setPostgresql(new PostgreSql());
    patroniConf.getPostgresql().setUsePgRewind(true);
    patroniConf.getPostgresql().setUseSlots(true);
    patroniConf.getPostgresql().setPgHba(Seq
        .seq(
            Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getConfigurations)
            .map(StackGresClusterConfigurations::getPatroni)
            .map(StackGresClusterPatroni::getDynamicConfig)
            .flatMap(StackGresClusterPatroniDynamicConfig::getPgHba)
            .stream()
            .flatMap(List::stream))
        .append(
            "local all all trust",
            "host all all 127.0.0.1/32 md5",
            "host all all ::1/128 md5",
            "local replication all trust",
            "host all all 0.0.0.0/0 md5",
            "host replication " + PatroniSecret.getReplicatorCredentials(context).v1 + " 0.0.0.0/0 md5")
        .toList());
    patroniConf.getPostgresql().setParameters(
        getPostgresConfigValues(cluster, pgConfig, isBackupConfigurationPresent));
    patroniConf.getPostgresql().setRecoveryConf(
        getPostgresRecoveryConfigValues(cluster, pgConfig, isBackupConfigurationPresent, isReplicateFromPresent));
    return patroniConf;
  }

  Map<String, String> getPostgresConfigValues(
      StackGresCluster cluster,
      StackGresPostgresConfig pgConfig,
      boolean isBackupConfigurationPresent) {
    Map<String, String> params = getPostgresParameters(cluster, pgConfig, isBackupConfigurationPresent);

    return normalizeParams(pgConfig.getSpec().getPostgresVersion(), params);
  }

  Map<String, String> getPostgresRecoveryConfigValues(
      StackGresCluster cluster,
      StackGresPostgresConfig pgConfig,
      boolean isBackupConfigurationPresent,
      boolean isReplicateFromPresent) {
    Map<String, String> params = getPostgresRecoveryParameters(
        cluster, isBackupConfigurationPresent, isReplicateFromPresent);

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

  private Map<String, String> getPostgresParameters(
      StackGresCluster cluster,
      StackGresPostgresConfig pgConfig,
      boolean isBackupConfigurationPresent) {
    final String version = pgConfig.getSpec().getPostgresVersion();
    Map<String, String> params = new HashMap<>(PostgresDefaultValues.getDefaultValues(
        StackGresVersion.getStackGresVersion(cluster), version));
    Map<String, String> userParams = pgConfig.getSpec().getPostgresqlConf();

    Optional.ofNullable(cluster.getSpec().getConfigurations().getPostgres())
        .map(StackGresPostgresConfigSpec::getPostgresqlConf)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .forEach(entry -> userParams.put(entry.getKey(), entry.getValue()));

    PostgresBlocklist.getBlocklistParameters().forEach(userParams::remove);
    params.putAll(userParams);

    boolean isEnvoyDisabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableEnvoy)
        .orElse(false);
    params.put("listen_addresses", isEnvoyDisabled ? "0.0.0.0" : "localhost");
    params.put("port", String.valueOf(EnvoyUtil.PG_PORT));

    if (isBackupConfigurationPresent) {
      params.put("archive_command",
          "exec-with-env '" + ClusterEnvVar.BACKUP_ENV.value(cluster) + "'"
              + " -- wal-g wal-push %p");
    } else {
      params.put("archive_command", "/bin/true");
    }

    if (Optional.ofNullable(cluster)
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

    if (getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      params.put("shared_preload_libraries", Optional.ofNullable(
          params.get("shared_preload_libraries"))
          .map(sharedPreloadLibraries -> "babelfishpg_tds, " + sharedPreloadLibraries)
          .orElse("babelfishpg_tds"));
    }

    if (Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false)) {
      params.put("ssl", "on");
      params.put("ssl_cert_file",
          ClusterPath.SSL_PATH.path() + "/" + PatroniUtil.CERTIFICATE_KEY);
      params.put("ssl_key_file",
          ClusterPath.SSL_PATH.path() + "/" + PatroniUtil.PRIVATE_KEY_KEY);
    }

    return params;
  }

  private Map<String, String> getPostgresRecoveryParameters(
      StackGresCluster cluster,
      boolean isBackupConfigurationPresent,
      boolean isReplicateFromPresent) {
    Map<String, String> params = new HashMap<>();

    if (isBackupConfigurationPresent && !isReplicateFromPresent) {
      params.put("restore_command",
          "exec-with-env '" + ClusterEnvVar.BACKUP_ENV.value(cluster) + "'"
              + " -- wal-g wal-fetch %f %p");
    }

    return params;
  }

}
