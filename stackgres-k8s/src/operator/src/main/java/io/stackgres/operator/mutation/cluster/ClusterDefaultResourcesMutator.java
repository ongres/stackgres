/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public class ClusterDefaultResourcesMutator implements ClusterMutator {

  private KubernetesClient client;

  private DefaultCustomResourceFactory<StackGresPostgresConfig> postgresFactory;
  private DefaultCustomResourceFactory<StackGresPoolingConfig> pgbouncerFactory;
  private DefaultCustomResourceFactory<StackGresProfile> profileFactory;
  private DefaultCustomResourceFactory<StackGresBackupConfig> backupFactory;

  private transient String installedNamespace;

  private transient StackGresPostgresConfig defaultPostgresConfig;
  private transient StackGresPoolingConfig defaultPgBouncerConfig;
  private transient StackGresProfile defaultProfile;
  private transient StackGresBackupConfig defaultBackup;

  @Inject
  public ClusterDefaultResourcesMutator(
      KubernetesClient client,
      DefaultCustomResourceFactory<StackGresPostgresConfig> postgresFactory,
      DefaultCustomResourceFactory<StackGresPoolingConfig> pgbouncerFactory,
      DefaultCustomResourceFactory<StackGresProfile> profileFactory,
      DefaultCustomResourceFactory<StackGresBackupConfig> backupFactory) {

    this.client = client;
    this.postgresFactory = postgresFactory;
    this.pgbouncerFactory = pgbouncerFactory;
    this.profileFactory = profileFactory;
    this.backupFactory = backupFactory;
  }

  @PostConstruct
  public void init() {
    installedNamespace = client.getNamespace();
    defaultPostgresConfig = postgresFactory.buildResource();
    defaultPgBouncerConfig = pgbouncerFactory.buildResource();
    defaultProfile = profileFactory.buildResource();
    defaultBackup = backupFactory.buildResource();
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {

    if (review.getRequest().getOperation() == Operation.CREATE) {

      StackGresCluster targetCluster = review.getRequest().getObject();

      String targetNamespace = targetCluster.getMetadata().getNamespace();
      StackGresClusterSpec spec = targetCluster.getSpec();

      if (installedNamespace.equals(targetNamespace)) {
        if (isEmpty(spec.getConfiguration().getBackupConfig())) {
          spec.getConfiguration().setBackupConfig(defaultBackup.getMetadata().getName());
        }
        if (isEmpty(spec.getConfiguration().getPostgresConfig())) {
          spec.getConfiguration().setPostgresConfig(defaultPostgresConfig.getMetadata().getName());
        }
        if (isEmpty(spec.getResourceProfile())) {
          spec.setResourceProfile(defaultProfile.getMetadata().getName());
        }
        if (isEmpty(spec.getConfiguration().getConnectionPoolingConfig())) {
          spec.getConfiguration()
              .setConnectionPoolingConfig(defaultPgBouncerConfig.getMetadata().getName());
        }
      }

    }

    return List.of();
  }

}
