/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class BackupSecret extends AbstractBackupSecret
    implements StackGresClusterResourceStreamFactory {

  private static final String BACKUP_SECRET_SUFFIX = "-backup";

  public static String backupName(StackGresClusterContext context) {
    return ResourceUtil.resourceName(
        context.getCluster().getMetadata().getName() + BACKUP_SECRET_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> create(StackGresGeneratorContext context) {
    return Seq.of(context.getClusterContext().getBackupContext()
        .map(backupContext -> new SecretBuilder()
            .withNewMetadata()
            .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
            .withName(backupName(context.getClusterContext()))
            .withLabels(ResourceUtil.clusterLabels(context.getClusterContext().getCluster()))
            .withOwnerReferences(ImmutableList.of(
                ResourceUtil.getOwnerReference(context.getClusterContext().getCluster())))
            .endMetadata()
            .withType("Opaque")
            .withData(ResourceUtil.addMd5Sum(
                getBackupSecrets(backupContext.getBackupConfig().getSpec(),
                    backupContext.getSecrets())))
            .build()))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  protected ImmutableMap<String, String> getBackupSecrets(
      StackGresBackupConfigSpec backupConfigSpec, Map<String, Map<String, String>> secrets) {
    return Seq.of(
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getPgpConfiguration)
        .map(pgpConf -> Seq.of(
            getSecretEntry("WALG_PGP_KEY", pgpConf.getKey(), secrets))),
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getS3)
        .map(awsConf -> Seq.of(
            getSecretEntry("AWS_ACCESS_KEY_ID",
                awsConf.getCredentials().getAccessKey(), secrets),
            getSecretEntry("AWS_SECRET_KEY_ID",
                awsConf.getCredentials().getSecretKey(), secrets))),
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getGcs)
        .map(gcsConfig -> Seq.of(
            getSecretEntry(
                getGcsCredentialsFileName(),
                gcsConfig.getCredentials().getServiceAccountJsonKey(),
                secrets))),
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getAzureblob)
        .map(azureConfig -> Seq.of(
            getSecretEntry("AZURE_STORAGE_ACCOUNT",
                azureConfig.getCredentials().getAccount(), secrets),
            getSecretEntry("AZURE_STORAGE_ACCESS_KEY",
                azureConfig.getCredentials().getAccessKey(), secrets))))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(s -> s)
        .collect(ImmutableMap.toImmutableMap(t -> t.v1, t -> t.v2));
  }

  protected String getGcsCredentialsFileName() {
    return ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  private Tuple2<String, String> getSecretEntry(String envvar,
      SecretKeySelector secretKeySelector, Map<String, Map<String, String>> secrets) {
    return Optional.ofNullable(secrets.get(secretKeySelector.getName()))
        .map(values -> values.get(secretKeySelector.getKey()))
        .map(value -> Tuple.tuple(envvar, value))
        .orElseThrow(() -> new IllegalStateException(
            "Secret " + secretKeySelector.getName()
            + "." + secretKeySelector.getKey() + " not found"));
  }
}
