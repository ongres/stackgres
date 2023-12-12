/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.stackgres.common.fixture.backup.BackupFixture;
import io.stackgres.common.fixture.backup.BackupListFixture;
import io.stackgres.common.fixture.cluster.ClusterFixture;
import io.stackgres.common.fixture.cluster.ClusterListFixture;
import io.stackgres.common.fixture.cluster.JsonClusterFixture;
import io.stackgres.common.fixture.config.ConfigFixture;
import io.stackgres.common.fixture.dbops.DbOpsFixture;
import io.stackgres.common.fixture.dbops.DbOpsListFixture;
import io.stackgres.common.fixture.distributedlogs.DistributedLogsFixture;
import io.stackgres.common.fixture.distributedlogs.DistributedLogsListFixture;
import io.stackgres.common.fixture.instanceprofile.InstanceProfileFixture;
import io.stackgres.common.fixture.instanceprofile.InstanceProfileListFixture;
import io.stackgres.common.fixture.objectstorage.ObjectStorageFixture;
import io.stackgres.common.fixture.objectstorage.ObjectStorageListFixture;
import io.stackgres.common.fixture.poolingconfig.JsonPoolingConfigFixture;
import io.stackgres.common.fixture.poolingconfig.PoolingConfigFixture;
import io.stackgres.common.fixture.poolingconfig.PoolingConfigListFixture;
import io.stackgres.common.fixture.postgresconfig.JsonPostgresConfigFixture;
import io.stackgres.common.fixture.postgresconfig.PostgresConfigFixture;
import io.stackgres.common.fixture.postgresconfig.PostgresConfigListFixture;
import io.stackgres.common.fixture.script.ScriptFixture;
import io.stackgres.common.fixture.shardedbackup.ShardedBackupFixture;
import io.stackgres.common.fixture.shardedbackup.ShardedBackupListFixture;
import io.stackgres.common.fixture.shardedcluster.ShardedClusterFixture;
import io.stackgres.common.fixture.shardedcluster.ShardedClusterListFixture;
import io.stackgres.common.fixture.shardeddbops.ShardedDbOpsFixture;
import io.stackgres.common.fixture.shardeddbops.ShardedDbOpsListFixture;
import io.stackgres.common.fixture.upgrade.Upgrade;

public interface Fixtures {

  static BackupFixture backup() {
    return new BackupFixture();
  }

  static BackupListFixture backupList() {
    return new BackupListFixture();
  }

  static ClusterFixture cluster() {
    return new ClusterFixture();
  }

  static ClusterListFixture clusterList() {
    return new ClusterListFixture();
  }

  static DistributedLogsFixture distributedLogs() {
    return new DistributedLogsFixture();
  }

  static DistributedLogsListFixture distributedLogsList() {
    return new DistributedLogsListFixture();
  }

  static DbOpsFixture dbOps() {
    return new DbOpsFixture();
  }

  static DbOpsListFixture dbOpsList() {
    return new DbOpsListFixture();
  }

  static InstanceProfileFixture instanceProfile() {
    return new InstanceProfileFixture();
  }

  static InstanceProfileListFixture instanceProfileList() {
    return new InstanceProfileListFixture();
  }

  static PoolingConfigFixture poolingConfig() {
    return new PoolingConfigFixture();
  }

  static PoolingConfigListFixture poolingConfigList() {
    return new PoolingConfigListFixture();
  }

  static PostgresConfigFixture postgresConfig() {
    return new PostgresConfigFixture();
  }

  static PostgresConfigListFixture postgresConfigList() {
    return new PostgresConfigListFixture();
  }

  static ObjectStorageFixture objectStorage() {
    return new ObjectStorageFixture();
  }

  static ObjectStorageListFixture objectStorageList() {
    return new ObjectStorageListFixture();
  }

  static ScriptFixture script() {
    return new ScriptFixture();
  }

  static ShardedClusterFixture shardedCluster() {
    return new ShardedClusterFixture();
  }

  static ShardedClusterListFixture shardedClusterList() {
    return new ShardedClusterListFixture();
  }

  static ShardedBackupFixture shardedBackup() {
    return new ShardedBackupFixture();
  }

  static ShardedBackupListFixture shardedBackupList() {
    return new ShardedBackupListFixture();
  }

  static ShardedDbOpsFixture shardedDbOps() {
    return new ShardedDbOpsFixture();
  }

  static ShardedDbOpsListFixture shardedDbOpsList() {
    return new ShardedDbOpsListFixture();
  }

  static ConfigFixture config() {
    return new ConfigFixture();
  }

  static ExtensionMetadataFixture extensionMetadata() {
    return new ExtensionMetadataFixture();
  }

  static ExtensionListFixture extensionList() {
    return new ExtensionListFixture();
  }

  static SecretFixture secret() {
    return new SecretFixture();
  }

  static ServiceFixture service() {
    return new ServiceFixture();
  }

  static StatefulSetFixture statefulSet() {
    return new StatefulSetFixture();
  }

  static CronJobFixture cronJob() {
    return new CronJobFixture();
  }

  static JobFixture job() {
    return new JobFixture();
  }

  static EndpointsFixture endpoints() {
    return new EndpointsFixture();
  }

  static StorageClassFixture storageClass() {
    return new StorageClassFixture();
  }

  static StorageClassListFixture storageClassList() {
    return new StorageClassListFixture();
  }

  static NamespaceListFixture namespaceList() {
    return new NamespaceListFixture();
  }

  static EventFixture event() {
    return new EventFixture();
  }

  static KubeStatusFixture kubeStatus() {
    return new KubeStatusFixture();
  }

  static PrometheusListFixture prometheusList() {
    return new PrometheusListFixture();
  }

  static JsonClusterFixture jsonCluster() {
    return new JsonClusterFixture();
  }

  static JsonPoolingConfigFixture jsonPoolingConfig() {
    return new JsonPoolingConfigFixture();
  }

  static JsonPostgresConfigFixture jsonPostgresConfig() {
    return new JsonPostgresConfigFixture();
  }

  static JsonSecretFixture jsonSecret() {
    return new JsonSecretFixture();
  }

  static JsonServiceFixture jsonService() {
    return new JsonServiceFixture();
  }

  static JsonStatefulSetFixture jsonStatefulSet() {
    return new JsonStatefulSetFixture();
  }

  static JsonStatefulSetListFixture jsonStatefulSetList() {
    return new JsonStatefulSetListFixture();
  }

  static JsonOpenApiFixture jsonOpenApi() {
    return new JsonOpenApiFixture();
  }

  static JsonSsaSanitizationFixture jsonSsaSanitization() {
    return new JsonSsaSanitizationFixture();
  }

  static Upgrade upgrade() {
    return new Upgrade() {};
  }

}
