/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static io.stackgres.testutil.JsonUtil.readFromJson;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
class ClusterValidationQuarkusTest {

  private static final URI REPOSITORY =
      URI.create("https://extensions.stackgres.io/postgres/repository?skipHostVerification=true");

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions().findFirst().get();

  private static final String BUILD_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedBuildMajorVersions().findFirst().get();

  @Inject
  KubernetesClient client;

  private StackGresClusterReview getConstraintClusterReview() {
    var review = readFromJson("cluster_allow_requests/valid_creation.json",
        StackGresClusterReview.class);
    review.getRequest().getObject().getMetadata().setNamespace("test");
    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setExtensions(
        getExtension("dblink", "pg_stat_statements", "plpgsql", "plpython3u"));
    spec.setToInstallPostgresExtensions(
        getInstalledExtension("dblink", "pg_stat_statements", "plpgsql", "plpython3u"));
    spec.setDistributedLogs(null);
    spec.setInitData(null);
    spec.getPostgres().setVersion("12.8");

    return review;
  }

  private List<StackGresClusterExtension> getExtension(String... names) {
    var extensionsList = new ArrayList<StackGresClusterExtension>();
    for (String name : names) {
      var extension = new StackGresClusterExtension();
      extension.setName(name);
      extensionsList.add(extension);
    }
    return extensionsList;
  }

  private List<StackGresClusterInstalledExtension> getInstalledExtension(String... names) {
    var extensionsList = new ArrayList<StackGresClusterInstalledExtension>();
    for (String name : names) {
      var installedExtension = new StackGresClusterInstalledExtension();
      installedExtension.setName(name);
      installedExtension.setPublisher("com.ongres");
      installedExtension.setRepository(REPOSITORY.toString());
      installedExtension.setVersion(POSTGRES_VERSION);
      installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
      installedExtension.setBuild(BUILD_MAJOR_VERSION);
      extensionsList.add(installedExtension);
    }
    return extensionsList;
  }

  @BeforeAll
  void setUp() {
    StackGresPoolingConfigList poolconfList =
        client.resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
            .list();
    client.resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
        .delete(poolconfList.getItems());
    var poolConfig = readFromJson("pooling_config/default.json", StackGresPoolingConfig.class);
    poolConfig.getMetadata().setNamespace("test");
    client.resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
        .inNamespace(poolConfig.getMetadata().getNamespace())
        .withName(poolConfig.getMetadata().getName())
        .createOrReplace(poolConfig);

    StackGresBackupConfigList bkconfList = client
        .resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .list();
    client.resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .delete(bkconfList.getItems());
    var backupConfig = readFromJson("backup_config/default.json", StackGresBackupConfig.class);
    backupConfig.getMetadata().setNamespace("test");
    client.resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .inNamespace(backupConfig.getMetadata().getNamespace())
        .withName(backupConfig.getMetadata().getName())
        .createOrReplace(backupConfig);

    StackGresPostgresConfigList pgconfList = client
        .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .list();
    client.resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .delete(pgconfList.getItems());
    var pgConfig =
        readFromJson("postgres_config/default_postgres.json", StackGresPostgresConfig.class);
    pgConfig.getMetadata().setNamespace("test");
    client.resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .inNamespace(pgConfig.getMetadata().getNamespace())
        .withName(pgConfig.getMetadata().getName())
        .createOrReplace(pgConfig);

    StackGresProfileList instanceList =
        client.resources(StackGresProfile.class, StackGresProfileList.class)
            .list();
    client.resources(StackGresProfile.class, StackGresProfileList.class)
        .delete(instanceList.getItems());
    var instanceConfig = readFromJson("stackgres_profiles/size-xs.json", StackGresProfile.class);
    instanceConfig.getMetadata().setNamespace("test");
    client.resources(StackGresProfile.class, StackGresProfileList.class)
        .inNamespace(instanceConfig.getMetadata().getNamespace())
        .withName(instanceConfig.getMetadata().getName())
        .createOrReplace(instanceConfig);

    StorageClassList storageList = client.storage().storageClasses().list();
    client.storage().storageClasses().delete(storageList.getItems());
    var storage = readFromJson("storage_class/standard.json", StorageClass.class);
    client.storage().storageClasses().create(storage);
  }

  @AfterAll
  void tearDown() {
    StackGresPoolingConfigList poolconfList =
        client.resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
            .list();
    client.resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
        .delete(poolconfList.getItems());

    StackGresBackupConfigList bkconfList = client
        .resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .list();
    client.resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .delete(bkconfList.getItems());

    StackGresPostgresConfigList pgconfList = client
        .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .list();
    client
        .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .delete(pgconfList.getItems());

    StackGresProfileList instanceList = client
        .resources(StackGresProfile.class, StackGresProfileList.class)
        .list();
    client.resources(StackGresProfile.class, StackGresProfileList.class)
        .delete(instanceList.getItems());

    StorageClassList storageList = client.storage().storageClasses().list();
    client.storage().storageClasses().delete(storageList.getItems());
  }

  @Test
  void given_validStackGresClusterReview_shouldNotFail() {
    StackGresClusterReview clusterReview = getConstraintClusterReview();
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.CLUSTER_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(true),
            "kind", is("AdmissionReview"))
        .statusCode(200);
  }

  @Test
  void given_withoutValidStorageClass_shouldFail() {
    var storage = readFromJson("storage_class/standard.json", StorageClass.class);
    client.storage().storageClasses().withName(storage.getMetadata().getName()).delete();

    StackGresClusterReview clusterReview = getConstraintClusterReview();
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.CLUSTER_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(false),
            "kind", is("AdmissionReview"),
            "response.status.code", is(400),
            "response.status.message", is("Storage class standard not found"))
        .statusCode(200);

    client.storage().storageClasses().create(storage);
  }

  @Test
  void given_withoutInstalledExtensions_shouldFail() {
    StackGresClusterReview clusterReview = getConstraintClusterReview();
    clusterReview.getRequest().getObject().getSpec().setToInstallPostgresExtensions(null);
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.CLUSTER_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(false),
            "kind", is("AdmissionReview"),
            "response.status.code", is(400),
            "response.status.message",
            is("Some extensions were not found: dblink (available 1.2),"
                + " pg_stat_statements (available 1.7), plpgsql (available 1.0),"
                + " plpython3u (available 1.0)"))
        .statusCode(200);
  }

}
