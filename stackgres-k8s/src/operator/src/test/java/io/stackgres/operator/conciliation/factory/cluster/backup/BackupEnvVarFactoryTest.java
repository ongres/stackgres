/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.BackupStorageBuilder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Test;

class BackupEnvVarFactoryTest {

  private final BackupEnvVarFactory backupEnvVarFactory = new BackupEnvVarFactory();

  private static Secret createSecret(String name, Map<String, String> data) {
    return new SecretBuilder()
        .withNewMetadata().withName(name).endMetadata()
        .withData(ResourceUtil.encodeSecret(data))
        .build();
  }

  @Test
  void getSecretEnvVar_awsS3_shouldReturnAccessKeyAndSecretKey() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewS3()
            .withNewAwsCredentials()
                .withNewSecretKeySelectors()
                    .withNewAccessKeyId()
                        .withName("my-secret")
                        .withKey("accessKey")
                    .endAccessKeyId()
                    .withNewSecretAccessKey()
                        .withName("my-secret")
                        .withKey("secretKey")
                    .endSecretAccessKey()
                .endSecretKeySelectors()
            .endAwsCredentials()
        .endS3()
        .build();

    Map<String, Secret> secrets = Map.of("my-secret",
        createSecret("my-secret", Map.of("accessKey", "AKIAIOSFODNN7EXAMPLE",
            "secretKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")));

    Map<String, String> result =
        backupEnvVarFactory.getSecretEnvVar("default", storage, secrets);

    assertTrue(result.containsKey("AWS_ACCESS_KEY_ID"));
    assertEquals("AKIAIOSFODNN7EXAMPLE", result.get("AWS_ACCESS_KEY_ID"));
    assertTrue(result.containsKey("AWS_SECRET_ACCESS_KEY"));
    assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
        result.get("AWS_SECRET_ACCESS_KEY"));
  }

  @Test
  void getSecretEnvVar_awsS3Compatible_shouldReturnCredentials() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewS3Compatible()
            .withNewAwsCredentials()
                .withNewSecretKeySelectors()
                    .withNewAccessKeyId()
                        .withName("compat-secret")
                        .withKey("accessKey")
                    .endAccessKeyId()
                    .withNewSecretAccessKey()
                        .withName("compat-secret")
                        .withKey("secretKey")
                    .endSecretAccessKey()
                .endSecretKeySelectors()
            .endAwsCredentials()
        .endS3Compatible()
        .build();

    Map<String, Secret> secrets = Map.of("compat-secret",
        createSecret("compat-secret", Map.of("accessKey", "compatAccessKey",
            "secretKey", "compatSecretKey")));

    Map<String, String> result =
        backupEnvVarFactory.getSecretEnvVar("default", storage, secrets);

    assertTrue(result.containsKey("AWS_ACCESS_KEY_ID"));
    assertEquals("compatAccessKey", result.get("AWS_ACCESS_KEY_ID"));
    assertTrue(result.containsKey("AWS_SECRET_ACCESS_KEY"));
    assertEquals("compatSecretKey", result.get("AWS_SECRET_ACCESS_KEY"));
  }

  @Test
  void getSecretEnvVar_awsS3CompatibleWithCaCert_shouldReturnCredentialsAndCaCert() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewS3Compatible()
            .withNewAwsCredentials()
                .withNewSecretKeySelectors()
                    .withNewAccessKeyId()
                        .withName("compat-secret")
                        .withKey("accessKey")
                    .endAccessKeyId()
                    .withNewSecretAccessKey()
                        .withName("compat-secret")
                        .withKey("secretKey")
                    .endSecretAccessKey()
                    .withNewCaCertificate()
                        .withName("compat-secret")
                        .withKey("caCert")
                    .endCaCertificate()
                .endSecretKeySelectors()
            .endAwsCredentials()
        .endS3Compatible()
        .build();

    Map<String, Secret> secrets = Map.of("compat-secret",
        createSecret("compat-secret", Map.of(
            "accessKey", "compatAccessKey",
            "secretKey", "compatSecretKey",
            "caCert", "-----BEGIN CERTIFICATE-----\nfakecert\n-----END CERTIFICATE-----")));

    Map<String, String> result =
        backupEnvVarFactory.getSecretEnvVar("default", storage, secrets);

    assertTrue(result.containsKey("AWS_ACCESS_KEY_ID"));
    assertTrue(result.containsKey("AWS_SECRET_ACCESS_KEY"));
    assertTrue(result.containsKey(BackupEnvVarFactory.AWS_S3_COMPATIBLE_CA_CERTIFICATE_FILE_NAME));
  }

  @Test
  void getSecretEnvVar_gcs_shouldReturnGcsCredentials() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewGcs()
            .withNewGcpCredentials()
                .withNewSecretKeySelectors()
                    .withNewServiceAccountJsonKey()
                        .withName("gcs-secret")
                        .withKey("sa-key")
                    .endServiceAccountJsonKey()
                .endSecretKeySelectors()
            .endGcpCredentials()
        .endGcs()
        .build();

    Map<String, Secret> secrets = Map.of("gcs-secret",
        createSecret("gcs-secret", Map.of("sa-key", "{\"type\":\"service_account\"}")));

    Map<String, String> result =
        backupEnvVarFactory.getSecretEnvVar("default", storage, secrets);

    assertTrue(result.containsKey(BackupEnvVarFactory.GCS_CREDENTIALS_FILE_NAME));
    assertEquals("{\"type\":\"service_account\"}",
        result.get(BackupEnvVarFactory.GCS_CREDENTIALS_FILE_NAME));
  }

  @Test
  void getSecretEnvVar_azureBlob_shouldReturnAzureCredentials() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewAzureBlob()
            .withNewAzureCredentials()
                .withNewSecretKeySelectors()
                    .withNewStorageAccount()
                        .withName("azure-secret")
                        .withKey("account")
                    .endStorageAccount()
                    .withNewAccessKey()
                        .withName("azure-secret")
                        .withKey("key")
                    .endAccessKey()
                .endSecretKeySelectors()
            .endAzureCredentials()
        .endAzureBlob()
        .build();

    Map<String, Secret> secrets = Map.of("azure-secret",
        createSecret("azure-secret", Map.of("account", "myaccount", "key", "mykey")));

    Map<String, String> result =
        backupEnvVarFactory.getSecretEnvVar("default", storage, secrets);

    assertTrue(result.containsKey("AZURE_STORAGE_ACCOUNT"));
    assertEquals("myaccount", result.get("AZURE_STORAGE_ACCOUNT"));
    assertTrue(result.containsKey("AZURE_STORAGE_ACCESS_KEY"));
    assertEquals("mykey", result.get("AZURE_STORAGE_ACCESS_KEY"));
  }

  @Test
  void getSecretEnvVar_sodiumEncryption_shouldReturnSodiumKey() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewEncryption()
            .withNewSodium()
                .withNewKey()
                    .withName("sodium-secret")
                    .withKey("sodium-key")
                .endKey()
            .endSodium()
        .endEncryption()
        .build();

    Map<String, Secret> secrets = Map.of("sodium-secret",
        createSecret("sodium-secret", Map.of("sodium-key", "sodiumKeyValue")));

    Map<String, String> result =
        backupEnvVarFactory.getSecretEnvVar("default", storage, secrets);

    assertTrue(result.containsKey("WALG_LIBSODIUM_KEY"));
    assertEquals("sodiumKeyValue", result.get("WALG_LIBSODIUM_KEY"));
  }

  @Test
  void getSecretEnvVar_openPgpEncryption_shouldReturnPgpKeys() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewEncryption()
            .withNewOpenpgp()
                .withNewKey()
                    .withName("pgp-secret")
                    .withKey("pgp-key")
                .endKey()
                .withNewKeyPassphrase()
                    .withName("pgp-secret")
                    .withKey("pgp-passphrase")
                .endKeyPassphrase()
            .endOpenpgp()
        .endEncryption()
        .build();

    Map<String, Secret> secrets = Map.of("pgp-secret",
        createSecret("pgp-secret", Map.of(
            "pgp-key", "pgpKeyValue", "pgp-passphrase", "pgpPassphraseValue")));

    Map<String, String> result =
        backupEnvVarFactory.getSecretEnvVar("default", storage, secrets);

    assertTrue(result.containsKey("WALG_PGP_KEY"));
    assertEquals("pgpKeyValue", result.get("WALG_PGP_KEY"));
    assertTrue(result.containsKey("WALG_PGP_KEY_PASSPHRASE"));
    assertEquals("pgpPassphraseValue", result.get("WALG_PGP_KEY_PASSPHRASE"));
  }

  @Test
  void getSecretEnvVar_missingSecret_shouldThrowIllegalArgumentException() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewS3()
            .withNewAwsCredentials()
                .withNewSecretKeySelectors()
                    .withNewAccessKeyId()
                        .withName("missing-secret")
                        .withKey("accessKey")
                    .endAccessKeyId()
                    .withNewSecretAccessKey()
                        .withName("missing-secret")
                        .withKey("secretKey")
                    .endSecretAccessKey()
                .endSecretKeySelectors()
            .endAwsCredentials()
        .endS3()
        .build();

    Map<String, Secret> secrets = Map.of();

    assertThrows(IllegalArgumentException.class,
        () -> backupEnvVarFactory.getSecretEnvVar("default", storage, secrets));
  }

  @Test
  void getSecretEnvVar_missingKey_shouldThrowIllegalArgumentException() {
    BackupStorage storage = new BackupStorageBuilder()
        .withNewS3()
            .withNewAwsCredentials()
                .withNewSecretKeySelectors()
                    .withNewAccessKeyId()
                        .withName("my-secret")
                        .withKey("accessKey")
                    .endAccessKeyId()
                    .withNewSecretAccessKey()
                        .withName("my-secret")
                        .withKey("secretKey")
                    .endSecretAccessKey()
                .endSecretKeySelectors()
            .endAwsCredentials()
        .endS3()
        .build();

    Map<String, Secret> secrets = Map.of("my-secret",
        createSecret("my-secret", Map.of("accessKey", "someValue")));

    assertThrows(IllegalArgumentException.class,
        () -> backupEnvVarFactory.getSecretEnvVar("default", storage, secrets));
  }
}
