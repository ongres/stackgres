/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public class ClusterDefaultResourcesMutator implements ClusterMutator {

  private KubernetesClientFactory factory;

  private DefaultCustomResourceFactory<StackGresPostgresConfig> postgresFactory;
  private DefaultCustomResourceFactory<StackGresPgbouncerConfig> pgbouncerFactory;
  private DefaultCustomResourceFactory<StackGresProfile> profileFactory;
  private DefaultCustomResourceFactory<StackGresBackupConfig> backupFactory;

  private transient String installedNamespace;

  private transient StackGresPostgresConfig defaultPostgresConfig;
  private transient StackGresPgbouncerConfig defaultPgBouncerConfig;
  private transient StackGresProfile defaultProfile;
  private transient StackGresBackupConfig defaultBackup;

  @Inject
  public ClusterDefaultResourcesMutator(
      KubernetesClientFactory factory,
      DefaultCustomResourceFactory<StackGresPostgresConfig> postgresFactory,
      DefaultCustomResourceFactory<StackGresPgbouncerConfig> pgbouncerFactory,
      DefaultCustomResourceFactory<StackGresProfile> profileFactory,
      DefaultCustomResourceFactory<StackGresBackupConfig> backupFactory) {

    this.factory = factory;
    this.postgresFactory = postgresFactory;
    this.pgbouncerFactory = pgbouncerFactory;
    this.profileFactory = profileFactory;
    this.backupFactory = backupFactory;
  }

  @PostConstruct
  public void init() {
    try (KubernetesClient client = factory.create()) {
      installedNamespace = client.getNamespace();
    }
    defaultPostgresConfig = postgresFactory.buildResource();
    defaultPgBouncerConfig = pgbouncerFactory.buildResource();
    defaultProfile = profileFactory.buildResource();
    defaultBackup = backupFactory.buildResource();

  }

  @Override
  public List<JsonPatchOperation> mutate(StackgresClusterReview review) {

    if (review.getRequest().getOperation() == Operation.CREATE) {

      StackGresCluster targetCluster = review.getRequest().getObject();

      String targetNamespace = targetCluster.getMetadata().getNamespace();
      StackGresClusterSpec spec = targetCluster.getSpec();

      if (installedNamespace.equals(targetNamespace)) {
        if (isEmpty(spec.getBackupConfig())) {
          spec.setBackupConfig(defaultBackup.getMetadata().getName());
        }
        if (isEmpty(spec.getPostgresConfig())) {
          spec.setPostgresConfig(defaultPostgresConfig.getMetadata().getName());
        }
        if (isEmpty(spec.getResourceProfile())) {
          spec.setResourceProfile(defaultProfile.getMetadata().getName());
        }
        if (isEmpty(spec.getConnectionPoolingConfig())) {
          spec.setConnectionPoolingConfig(defaultPgBouncerConfig.getMetadata().getName());
        }
      }

    }

    return new ArrayList<>();

  }

}
