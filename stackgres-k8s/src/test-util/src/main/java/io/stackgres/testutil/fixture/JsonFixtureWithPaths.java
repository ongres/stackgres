/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil.fixture;

public interface JsonFixtureWithPaths {
  String CONFIGMAP_LIST_JSON = "configmap/list.json";

  String CRONJOB_DEPLOYED_JSON = "cronjob/deployed.json";

  String CRONJOB_REQUIRED_JSON = "cronjob/required.json";

  String ENDPOINTS_DEPLOYED_JSON = "endpoints/deployed.json";

  String ENDPOINTS_PATRONI_CONFIG_JSON = "endpoints/patroni_config.json";

  String ENDPOINTS_PATRONI_CONFIG_WITH_STANDBY_CLUSTER_JSON =
      "endpoints/patroni_config_with_standby_cluster.json";

  String ENDPOINTS_PATRONI_DEPLOYED_JSON = "endpoints/patroni_deployed.json";

  String ENDPOINTS_PATRONI_JSON = "endpoints/patroni.json";

  String ENDPOINTS_PATRONI_REQUIRED_JSON = "endpoints/patroni_required.json";

  String ENDPOINTS_REQUIRED_JSON = "endpoints/required.json";

  String EVENT_EVENT_VALID_JSON = "event/event_valid.json";

  String EXTENSION_METADATA_EXTENSIONS_JSON = "extension_metadata/extensions.json";

  String EXTENSION_METADATA_INDEX_JSON = "extension_metadata/index.json";

  String JOB_DEPLOYED_JSON = "job/deployed.json";

  String JOB_REQUIRED_JSON = "job/required.json";

  String KUBE_STATUS_ALREADY_EXISTS_JSON = "kube_status/already-exists.json";

  String KUBE_STATUS_INVALID_CLUSTER_NAME_JSON = "kube_status/invalid_cluster_name.json";

  String KUBE_STATUS_INVALID_DNS_NAME_JSON = "kube_status/invalid_dns_name.json";

  String KUBE_STATUS_STATUS_1_13_12_JSON = "kube_status/status-1.13.12.json";

  String KUBE_STATUS_STATUS_1_16_4_JSON = "kube_status/status-1.16.4.json";

  String NAMESPACE_LIST_JSON = "namespace/list.json";

  String NAMESPACE_STANDARD_JSON = "namespace/standard.json";

  String OPENAPI_JSON_XZ = "openapi.json.xz";

  String PATRONI_CLUSTERS_JSON = "patroni/clusters.json";

  String PATRONI_PATRONI_PRIMARY_JSON = "patroni/patroni-primary.json";

  String PATRONI_PATRONI_REPLICA_JSON = "patroni/patroni-replica.json";

  String PROMETHEUS_PROMETHEUS_LIST_JSON = "prometheus/prometheus_list.json";

  String SECRET_AUTHENTICATION_JSON = "secret/authentication.json";

  String SECRET_BACKUP_SECRET_JSON = "secret/backup-secret.json";

  String SECRET_BACKUP_SECRET_WITH_MANAGED_FIELDS_JSON =
      "secret/backup-secret-with-managed-fields.json";

  String SECRET_MINIO_JSON = "secret/minio.json";

  String SECRET_PATRONI_JSON = "secret/patroni.json";

  String SECRET_SECRET_JSON = "secret/secret.json";

  String SECRET_USER_JSON = "secret/user.json";

  String SERVICE_DEPLOYED_JSON = "service/deployed.json";

  String SERVICE_PATRONI_REST_JSON = "service/patroni-rest.json";

  String SERVICE_PRIMARY_SERVICE_JSON = "service/primary-service.json";

  String SERVICE_PRIMARY_SERVICE_WITH_MANAGED_FIELDS_JSON =
      "service/primary-service-with-managed-fields.json";

  String SERVICE_REQUIRED_JSON = "service/required.json";

  String SSA_SANITIZATION_ENDPOINTS1_JSON = "ssa-sanitization/endpoints1.json";

  String SSA_SANITIZATION_ENDPOINTS2_JSON = "ssa-sanitization/endpoints2.json";

  String SSA_SANITIZATION_ENDPOINTS3_JSON = "ssa-sanitization/endpoints3.json";

  String SSA_SANITIZATION_ROLE1_JSON = "ssa-sanitization/role1.json";

  String SSA_SANITIZATION_ROLE2_JSON = "ssa-sanitization/role2.json";

  String SSA_SANITIZATION_ROLE3_JSON = "ssa-sanitization/role3.json";

  String SSA_SANITIZATION_ROLEBINDING1_JSON = "ssa-sanitization/rolebinding1.json";

  String SSA_SANITIZATION_ROLEBINDING2_JSON = "ssa-sanitization/rolebinding2.json";

  String SSA_SANITIZATION_ROLEBINDING3_JSON = "ssa-sanitization/rolebinding3.json";

  String SSA_SANITIZATION_SECRET1_JSON = "ssa-sanitization/secret1.json";

  String SSA_SANITIZATION_SECRET2_JSON = "ssa-sanitization/secret2.json";

  String SSA_SANITIZATION_SECRET3_JSON = "ssa-sanitization/secret3.json";

  String SSA_SANITIZATION_SERVICEACCOUNT_JSON = "ssa-sanitization/serviceaccount.json";

  String SSA_SANITIZATION_SERVICEACCOUNT_SANITIZED_JSON =
      "ssa-sanitization/serviceaccount-sanitized.json";

  String SSA_SANITIZATION_SERVICEMONITOR_JSON = "ssa-sanitization/servicemonitor.json";

  String SSA_SANITIZATION_STS1_JSON = "ssa-sanitization/sts1.json";

  String SSA_SANITIZATION_STS1_SANITIZED_JSON = "ssa-sanitization/sts1-sanitized.json";

  String SSA_SANITIZATION_STS2_JSON = "ssa-sanitization/sts2.json";

  String SSA_SANITIZATION_STS2_SANITIZED_JSON = "ssa-sanitization/sts2-sanitized.json";

  String SSA_SANITIZATION_STS3_JSON = "ssa-sanitization/sts3.json";

  String SSA_SANITIZATION_STS3_SANITIZED_JSON = "ssa-sanitization/sts3-sanitized.json";

  String SSA_SANITIZATION_STS4_JSON = "ssa-sanitization/sts4.json";

  String SSA_SANITIZATION_STS4_SANITIZED_JSON = "ssa-sanitization/sts4-sanitized.json";

  String SSA_SANITIZATION_SVC1_JSON = "ssa-sanitization/svc1.json";

  String SSA_SANITIZATION_SVC1_SANITIZED_JSON = "ssa-sanitization/svc1-sanitized.json";

  String SSA_SANITIZATION_SVC2_JSON = "ssa-sanitization/svc2.json";

  String SSA_SANITIZATION_SVC2_SANITIZED_JSON = "ssa-sanitization/svc2-sanitized.json";

  String SSA_SANITIZATION_SVC3_JSON = "ssa-sanitization/svc3.json";

  String SSA_SANITIZATION_SVC3_SANITIZED_JSON = "ssa-sanitization/svc3-sanitized.json";

  String STACKGRES_BACKUP_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/backup/admission_review/create.json";

  String STACKGRES_BACKUP_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/backup/admission_review/delete.json";

  String STACKGRES_BACKUP_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/backup/admission_review/update.json";

  String STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/backup_config/admission_review/create.json";

  String STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/backup_config/admission_review/delete.json";

  String STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_INVALID_CREATION_GCS_AND_S3_JSON =
      "stackgres/backup_config/admission_review/invalid_creation_gcs_and_s3.json";

  String STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_INVALID_CREATION_NO_S3_JSON =
      "stackgres/backup_config/admission_review/invalid_creation_no_s3.json";

  String STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/backup_config/admission_review/update.json";

  String STACKGRES_BACKUP_CONFIG_DEFAULT_JSON = "stackgres/backup_config/default.json";

  String STACKGRES_BACKUP_CONFIG_DTO_JSON = "stackgres/backup_config/dto.json";

  String STACKGRES_BACKUP_CONFIG_GOOGLE_IDENTITY_CONFIG_JSON =
      "stackgres/backup_config/google_identity_config.json";

  String STACKGRES_BACKUP_CONFIG_LIST_JSON = "stackgres/backup_config/list.json";

  String STACKGRES_BACKUP_DEFAULT_JSON = "stackgres/backup/default.json";

  String STACKGRES_BACKUP_DTO_JSON = "stackgres/backup/dto.json";

  String STACKGRES_BACKUP_LIST_JSON = "stackgres/backup/list.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_BACKUP_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/backup_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_CONNECTION_POOLING_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/connection_pooling_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/cluster/admission_review/create.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_CREATE_WITH_MANAGED_SQL_JSON =
      "stackgres/cluster/admission_review/create_with_managed_sql.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/cluster/admission_review/delete.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_DISTRIBUTED_LOGS_UPDATE_JSON =
      "stackgres/cluster/admission_review/distributed_logs_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_EMPTY_PG_VERSION_JSON =
      "stackgres/cluster/admission_review/invalid_creation_empty_pg_version.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_NO_PG_VERSION_JSON =
      "stackgres/cluster/admission_review/invalid_creation_no_pg_version.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_PG_VERSION_JSON =
      "stackgres/cluster/admission_review/invalid_creation_pg_version.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_ZERO_INSTANCES_JSON =
      "stackgres/cluster/admission_review/invalid_creation_zero_instances.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_MAJOR_POSTGRES_VERSION_UPDATE_JSON =
      "stackgres/cluster/admission_review/major_postgres_version_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_MINOR_POSTGRES_VERSION_UPDATE_JSON =
      "stackgres/cluster/admission_review/minor_postgres_version_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_POSTGRES_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/postgres_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_PROFILE_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/profile_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_RESTORE_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/restore_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_PATRONI_INITIAL_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/patroni_initial_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_SCRIPTS_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/scripts_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_SSL_UPDATE_JSON =
      "stackgres/cluster/admission_review/ssl_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_STORAGE_CLASS_CONFIG_UPDATE_JSON =
      "stackgres/cluster/admission_review/storage_class_config_update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/cluster/admission_review/update.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_UPDATE_WITH_MANAGED_SQL_JSON =
      "stackgres/cluster/admission_review/update_with_managed_sql.json";

  String STACKGRES_CLUSTER_ADMISSION_REVIEW_WRONG_MAJOR_POSTGRES_VERSION_UPDATE_JSON =
      "stackgres/cluster/admission_review/wrong_major_postgres_version_update.json";

  String STACKGRES_CLUSTER_DEFAULT_JSON = "stackgres/cluster/default.json";

  String STACKGRES_CLUSTER_DTO_JSON = "stackgres/cluster/dto.json";

  String STACKGRES_CLUSTER_FROM_VERSION1BETA1_JSON = "stackgres/cluster/from_version1beta1.json";

  String STACKGRES_CLUSTER_FROM_VERSION1_JSON = "stackgres/cluster/from_version1.json";

  String STACKGRES_CLUSTER_INLINE_SCRIPTS_JSON = "stackgres/cluster/inline_scripts.json";

  String STACKGRES_CLUSTER_LIST_JSON = "stackgres/cluster/list.json";

  String STACKGRES_CLUSTER_MANAGED_SQL_JSON = "stackgres/cluster/managed_sql.json";

  String STACKGRES_CLUSTER_PODS_JSON = "stackgres/cluster/pods.json";

  String STACKGRES_CLUSTER_SCHEDULING_BACKUP_JSON = "stackgres/cluster/scheduling_backup.json";

  String STACKGRES_CLUSTER_SCHEDULING_JSON = "stackgres/cluster/scheduling.json";

  String STACKGRES_CLUSTER_TO_VERSION1BETA1_JSON = "stackgres/cluster/to_version1beta1.json";

  String STACKGRES_CLUSTER_TO_VERSION1_JSON = "stackgres/cluster/to_version1.json";

  String STACKGRES_CLUSTER_USING_LOAD_BALANCER_IP_JSON =
      "stackgres/cluster/using_load_balancer_ip.json";

  String STACKGRES_CLUSTER_WITHOUT_DISTRIBUTED_LOGS_JSON =
      "stackgres/cluster/without_distributed_logs.json";

  String STACKGRES_CONFIG_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/config/admission_review/create.json";

  String STACKGRES_CONFIG_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/config/admission_review/delete.json";

  String STACKGRES_CONFIG_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/config/admission_review/update.json";

  String STACKGRES_CONFIG_DEFAULT_JSON =
      "stackgres/config/default.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/db_ops/admission_review/delete.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_MAJOR_VERSION_UPGRADE_CREATE_JSON =
      "stackgres/db_ops/admission_review/major_version_upgrade_create.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_MINOR_VERSION_UPGRADE_CREATE_JSON =
      "stackgres/db_ops/admission_review/minor_version_upgrade_create.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_PGBENCH_CREATE_JSON =
      "stackgres/db_ops/admission_review/pgbench_create.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_REPACK_CREATE_JSON =
      "stackgres/db_ops/admission_review/repack_create.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_RESTART_CREATE_JSON =
      "stackgres/db_ops/admission_review/restart_create.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_SECURITY_UPGRADE_CREATE_JSON =
      "stackgres/db_ops/admission_review/security_upgrade_create.json";

  String STACKGRES_DB_OPS_ADMISSION_REVIEW_VACUUM_CREATE_JSON =
      "stackgres/db_ops/admission_review/vacuum_create.json";

  String STACKGRES_DB_OPS_DTO_JSON = "stackgres/db_ops/dto.json";

  String STACKGRES_DB_OPS_LIST_JSON = "stackgres/db_ops/list.json";

  String STACKGRES_DB_OPS_BENCHMARK_JSON = "stackgres/db_ops/benchmark.json";

  String STACKGRES_DB_OPS_DBOPS_VACUUM_JSON = "stackgres/db_ops/dbops_vacuum.json";

  String STACKGRES_DB_OPS_MAJOR_VERSION_UPGRADE_JSON =
      "stackgres/db_ops/major_version_upgrade.json";

  String STACKGRES_DB_OPS_MINOR_VERSION_UPGRADE_JSON =
      "stackgres/db_ops/minor_version_upgrade.json";

  String STACKGRES_DB_OPS_PGBENCH_JSON = "stackgres/db_ops/pgbench.json";

  String STACKGRES_DB_OPS_REPACK_JSON = "stackgres/db_ops/repack.json";

  String STACKGRES_DB_OPS_RESTART_JSON = "stackgres/db_ops/restart.json";

  String STACKGRES_DB_OPS_SCHEDULING_JSON = "stackgres/db_ops/scheduling.json";

  String STACKGRES_DB_OPS_SECURITY_UPGRADE_JSON = "stackgres/db_ops/security_upgrade.json";

  String STACKGRES_DB_OPS_VACUUM_JSON = "stackgres/db_ops/vacuum.json";

  String STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/distributed_logs/admission_review/create.json";

  String STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/distributed_logs/admission_review/delete.json";

  String STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_PROFILE_CONFIG_UPDATE_JSON =
      "stackgres/distributed_logs/admission_review/profile_config_update.json";

  String STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/distributed_logs/admission_review/update.json";

  String STACKGRES_DISTRIBUTED_LOGS_DEFAULT_JSON = "stackgres/distributed_logs/default.json";

  String STACKGRES_DISTRIBUTED_LOGS_DTO_JSON = "stackgres/distributed_logs/dto.json";

  String STACKGRES_DISTRIBUTED_LOGS_LIST_JSON = "stackgres/distributed_logs/list.json";

  String STACKGRES_INSTANCE_PROFILE_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/instance_profile/admission_review/create.json";

  String STACKGRES_INSTANCE_PROFILE_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/instance_profile/admission_review/delete.json";

  String STACKGRES_INSTANCE_PROFILE_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/instance_profile/admission_review/update.json";

  String STACKGRES_INSTANCE_PROFILE_DTO_JSON = "stackgres/instance_profile/dto.json";

  String STACKGRES_INSTANCE_PROFILE_LIST_JSON = "stackgres/instance_profile/list.json";

  String STACKGRES_INSTANCE_PROFILE_SIZE_S_JSON = "stackgres/instance_profile/size-s.json";

  String STACKGRES_INSTANCE_PROFILE_SIZE_M_JSON = "stackgres/instance_profile/size-m.json";

  String STACKGRES_OBJECT_STORAGE_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/object_storage/admission_review/create.json";

  String STACKGRES_OBJECT_STORAGE_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/object_storage/admission_review/delete.json";

  String STACKGRES_OBJECT_STORAGE_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/object_storage/admission_review/update.json";

  String STACKGRES_OBJECT_STORAGE_DEFAULT_JSON = "stackgres/object_storage/default.json";

  String STACKGRES_OBJECT_STORAGE_DTO_JSON = "stackgres/object_storage/dto.json";

  String STACKGRES_OBJECT_STORAGE_GOOGLE_IDENTITY_CONFIG_JSON =
      "stackgres/object_storage/google_identity_config.json";

  String STACKGRES_OBJECT_STORAGE_LIST_JSON = "stackgres/object_storage/list.json";

  String STACKGRES_POOLING_CONFIG_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/pooling_config/admission_review/create.json";

  String STACKGRES_POOLING_CONFIG_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/pooling_config/admission_review/delete.json";

  String STACKGRES_POOLING_CONFIG_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/pooling_config/admission_review/update.json";

  String STACKGRES_POOLING_CONFIG_DEFAULT_JSON = "stackgres/pooling_config/default.json";

  String STACKGRES_POOLING_CONFIG_DTO_JSON = "stackgres/pooling_config/dto.json";

  String STACKGRES_POOLING_CONFIG_FROM_VERSION1BETA1_JSON =
      "stackgres/pooling_config/from_version1beta1.json";

  String STACKGRES_POOLING_CONFIG_FROM_VERSION1_JSON =
      "stackgres/pooling_config/from_version1.json";

  String STACKGRES_POOLING_CONFIG_LIST_JSON = "stackgres/pooling_config/list.json";

  String STACKGRES_POOLING_CONFIG_TO_VERSION1BETA1_JSON =
      "stackgres/pooling_config/to_version1beta1.json";

  String STACKGRES_POOLING_CONFIG_TO_VERSION1_JSON = "stackgres/pooling_config/to_version1.json";

  String STACKGRES_POSTGRES_CONFIG_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/postgres_config/admission_review/create.json";

  String STACKGRES_POSTGRES_CONFIG_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/postgres_config/admission_review/delete.json";

  String STACKGRES_POSTGRES_CONFIG_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/postgres_config/admission_review/update.json";

  String STACKGRES_POSTGRES_CONFIG_DEFAULT_JSON = "stackgres/postgres_config/default.json";

  String STACKGRES_POSTGRES_CONFIG_DTO_JSON = "stackgres/postgres_config/dto.json";

  String STACKGRES_POSTGRES_CONFIG_FROM_VERSION1BETA1_JSON =
      "stackgres/postgres_config/from_version1beta1.json";

  String STACKGRES_POSTGRES_CONFIG_FROM_VERSION1_JSON =
      "stackgres/postgres_config/from_version1.json";

  String STACKGRES_POSTGRES_CONFIG_LIST_JSON = "stackgres/postgres_config/list.json";

  String STACKGRES_POSTGRES_CONFIG_TO_VERSION1BETA1_JSON =
      "stackgres/postgres_config/to_version1beta1.json";

  String STACKGRES_POSTGRES_CONFIG_TO_VERSION1_JSON = "stackgres/postgres_config/to_version1.json";

  String STACKGRES_SCRIPT_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/script/admission_review/create.json";

  String STACKGRES_SCRIPT_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/script/admission_review/delete.json";

  String STACKGRES_SCRIPT_ADMISSION_REVIEW_SCRIPTS_CONFIG_UPDATE_JSON =
      "stackgres/script/admission_review/scripts_config_update.json";

  String STACKGRES_SCRIPT_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/script/admission_review/update.json";

  String STACKGRES_SCRIPT_DEFAULT_JSON = "stackgres/script/default.json";

  String STACKGRES_SCRIPT_DTO_JSON = "stackgres/script/dto.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/sharded_cluster/admission_review/create.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/sharded_cluster/admission_review/delete.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_DISTRIBUTED_LOGS_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/distributed_logs_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_CONNECTION_POOLING_CONFIG_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/connection_pooling_config_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_CREATE_WITH_MANAGED_SQL_JSON =
      "stackgres/sharded_cluster/admission_review/create_with_managed_sql.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_EMPTY_PG_VERSION_JSON =
      "stackgres/sharded_cluster/admission_review/invalid_creation_empty_pg_version.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_NO_PG_VERSION_JSON =
      "stackgres/sharded_cluster/admission_review/invalid_creation_no_pg_version.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_PG_VERSION_JSON =
      "stackgres/sharded_cluster/admission_review/invalid_creation_pg_version.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_ZERO_INSTANCES_JSON =
      "stackgres/sharded_cluster/admission_review/invalid_creation_zero_instances.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_MAJOR_POSTGRES_VERSION_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/major_postgres_version_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_MINOR_POSTGRES_VERSION_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/minor_postgres_version_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_POSTGRES_CONFIG_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/postgres_config_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_PROFILE_CONFIG_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/profile_config_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_RESTORE_CONFIG_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/restore_config_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_STORAGE_CLASS_CONFIG_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/storage_class_config_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_UPDATE_WITH_MANAGED_SQL_JSON =
      "stackgres/sharded_cluster/admission_review/update_with_managed_sql.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_SSL_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/ssl_update.json";

  String STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_WRONG_MAJOR_POSTGRES_VERSION_UPDATE_JSON =
      "stackgres/sharded_cluster/admission_review/wrong_major_postgres_version_update.json";

  String STACKGRES_SHARDED_CLUSTER_DEFAULT_JSON = "stackgres/sharded_cluster/default.json";

  String STACKGRES_SHARDED_CLUSTER_DTO_JSON = "stackgres/sharded_cluster/dto.json";

  String STACKGRES_SHARDED_CLUSTER_INLINE_SCRIPTS_JSON =
      "stackgres/sharded_cluster/inline_scripts.json";

  String STACKGRES_SHARDED_CLUSTER_LIST_JSON = "stackgres/sharded_cluster/list.json";

  String STACKGRES_SHARDED_BACKUP_ADMISSION_REVIEW_CREATE_JSON =
      "stackgres/sharded_backup/admission_review/create.json";

  String STACKGRES_SHARDED_BACKUP_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/sharded_backup/admission_review/delete.json";

  String STACKGRES_SHARDED_BACKUP_ADMISSION_REVIEW_UPDATE_JSON =
      "stackgres/sharded_backup/admission_review/update.json";

  String STACKGRES_SHARDED_BACKUP_DEFAULT_JSON = "stackgres/sharded_backup/default.json";

  String STACKGRES_SHARDED_BACKUP_DTO_JSON = "stackgres/sharded_backup/dto.json";

  String STACKGRES_SHARDED_BACKUP_LIST_JSON = "stackgres/sharded_backup/list.json";

  String STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_DELETE_JSON =
      "stackgres/sharded_db_ops/admission_review/delete.json";

  String STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_MAJOR_VERSION_UPGRADE_CREATE_JSON =
      "stackgres/sharded_db_ops/admission_review/major_version_upgrade_create.json";

  String STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_MINOR_VERSION_UPGRADE_CREATE_JSON =
      "stackgres/sharded_db_ops/admission_review/minor_version_upgrade_create.json";

  String STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_RESHARDING_CREATE_JSON =
      "stackgres/sharded_db_ops/admission_review/resharding_create.json";

  String STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_RESTART_CREATE_JSON =
      "stackgres/sharded_db_ops/admission_review/restart_create.json";

  String STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_SECURITY_UPGRADE_CREATE_JSON =
      "stackgres/sharded_db_ops/admission_review/security_upgrade_create.json";

  String STACKGRES_SHARDED_DB_OPS_DTO_JSON = "stackgres/sharded_db_ops/dto.json";

  String STACKGRES_SHARDED_DB_OPS_LIST_JSON = "stackgres/sharded_db_ops/list.json";

  String STACKGRES_SHARDED_DB_OPS_MAJOR_VERSION_UPGRADE_JSON =
      "stackgres/sharded_db_ops/major_version_upgrade.json";

  String STACKGRES_SHARDED_DB_OPS_MINOR_VERSION_UPGRADE_JSON =
      "stackgres/sharded_db_ops/minor_version_upgrade.json";

  String STACKGRES_SHARDED_DB_OPS_RESHARDING_JSON = "stackgres/sharded_db_ops/resharding.json";

  String STACKGRES_SHARDED_DB_OPS_RESTART_JSON = "stackgres/sharded_db_ops/restart.json";

  String STACKGRES_SHARDED_DB_OPS_SCHEDULING_JSON = "stackgres/sharded_db_ops/scheduling.json";

  String STACKGRES_SHARDED_DB_OPS_SECURITY_UPGRADE_JSON =
      "stackgres/sharded_db_ops/security_upgrade.json";

  String STACKGRES_USER_DTO_JSON = "stackgres/user/dto.json";

  String STATEFULSET_0_9_5_JSON = "statefulset/0.9.5.json";

  String STATEFULSET_DEPLOYED_DISTRIBUTEDLOGS_JSON = "statefulset/deployed_distributedlogs.json";

  String STATEFULSET_DEPLOYED_JSON = "statefulset/deployed.json";

  String STATEFULSET_K8S_STS_LIST_RESPONSE_JSON = "statefulset/k8s-sts-list-response.json";

  String STATEFULSET_REQUIRED_DISTRIBUTEDLOGS_JSON = "statefulset/required_distributedlogs.json";

  String STATEFULSET_REQUIRED_JSON = "statefulset/required.json";

  String STATEFULSET_STATEFULSET_WITHOUT_MANAGED_FIELDS_JSON =
      "statefulset/statefulset-without-managed-fields.json";

  String STATEFULSET_STATEFULSET_WITH_MANAGED_FIELDS_JSON =
      "statefulset/statefulset-with-managed-fields.json";

  String STORAGE_CLASS_LIST_JSON = "storage_class/list.json";

  String STORAGE_CLASS_STANDARD_JSON = "storage_class/standard.json";

  String UPGRADE_SGBACKUPCONFIG_JSON = "upgrade/sgbackupconfig.json";

  String UPGRADE_SGCLUSTER_JSON = "upgrade/sgcluster.json";

  String UPGRADE_SGINSTANCEPROFILE_JSON = "upgrade/sginstanceprofile.json";

  String UPGRADE_SGPGCONFIG_JSON = "upgrade/sgpgconfig.json";

  String UPGRADE_SGPOOLCONFIG_JSON = "upgrade/sgpoolconfig.json";

  String UPGRADE_V1_0_PATRONI_JSON = "upgrade/v1.0/patroni.json";

  String UPGRADE_V1_1_PATRONI_JSON = "upgrade/v1.1/patroni.json";
}
