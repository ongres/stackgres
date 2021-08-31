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
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
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
public class ClusterValidationQuarkusTest {

  private static final URI REPOSITORY =
      URI.create("https://extensions.stackgres.io/postgres/repository?skipHostVerification=true");

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions().findFirst().get();

  private static final String BUILD_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedBuildMajorVersions().findFirst().get();

  @Inject
  KubernetesClientFactory factory;

  private StackGresClusterReview getConstraintClusterReview() {
    var review = readFromJson("cluster_allow_requests/valid_creation.json",
        StackGresClusterReview.class);
    review.getRequest().getObject().getMetadata().setNamespace("test");
    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.setToInstallPostgresExtensions(
        getInstalledExtension("dblink", "pg_stat_statements", "plpgsql", "plpython3u"));
    spec.setDistributedLogs(null);
    spec.setInitData(null);
    spec.getPostgres().setVersion("12.8");

    return review;
  }

  private List<StackGresClusterInstalledExtension> getInstalledExtension(String... names) {
    var extensionsList = new ArrayList<StackGresClusterInstalledExtension>();
    for (String name : names) {
      var installedExtension = new StackGresClusterInstalledExtension();
      installedExtension.setName(name);
      installedExtension.setPublisher("com.ongres");
      installedExtension.setRepository(REPOSITORY.toASCIIString());
      installedExtension.setVersion(POSTGRES_VERSION);
      installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
      installedExtension.setBuild(BUILD_MAJOR_VERSION);
      extensionsList.add(installedExtension);
    }
    return extensionsList;
  }

  @BeforeAll
  void setUp() {
    try (KubernetesClient client = factory.create()) {
      StackGresPoolingConfigList poolconfList = client
          .customResources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
          .list();
      client
          .customResources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
          .delete(poolconfList.getItems());
      var poolConfig = readFromJson("pooling_config/default.json", StackGresPoolingConfig.class);
      poolConfig.getMetadata().setNamespace("test");
      client
          .customResources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
          .inNamespace(poolConfig.getMetadata().getNamespace())
          .withName(poolConfig.getMetadata().getName())
          .createOrReplace(poolConfig);

      StackGresBackupConfigList bkconfList = client
          .customResources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
          .list();
      client.customResources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
          .delete(bkconfList.getItems());
      var backupConfig = readFromJson("backup_config/default.json", StackGresBackupConfig.class);
      backupConfig.getMetadata().setNamespace("test");
      client.customResources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
          .inNamespace(backupConfig.getMetadata().getNamespace())
          .withName(backupConfig.getMetadata().getName())
          .createOrReplace(backupConfig);

      StackGresPostgresConfigList pgconfList = client
          .customResources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
          .list();
      client
          .customResources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
          .delete(pgconfList.getItems());
      var pgConfig =
          readFromJson("postgres_config/default_postgres.json", StackGresPostgresConfig.class);
      pgConfig.getMetadata().setNamespace("test");
      client
          .customResources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
          .inNamespace(pgConfig.getMetadata().getNamespace())
          .withName(pgConfig.getMetadata().getName())
          .createOrReplace(pgConfig);

      StackGresProfileList instanceList = client
          .customResources(StackGresProfile.class, StackGresProfileList.class)
          .list();
      client.customResources(StackGresProfile.class, StackGresProfileList.class)
          .delete(instanceList.getItems());
      var instanceConfig = readFromJson("stackgres_profiles/size-xs.json", StackGresProfile.class);
      instanceConfig.getMetadata().setNamespace("test");
      client.customResources(StackGresProfile.class, StackGresProfileList.class)
          .inNamespace(instanceConfig.getMetadata().getNamespace())
          .withName(instanceConfig.getMetadata().getName())
          .createOrReplace(instanceConfig);

      StorageClassList storageList = client.storage().storageClasses().list();
      client.storage().storageClasses().delete(storageList.getItems());
      var storage = readFromJson("storage_class/standard.json", StorageClass.class);
      client.storage().storageClasses().create(storage);
    }
  }

  @AfterAll
  void tearDown() {
    try (KubernetesClient client = factory.create()) {
      StackGresPoolingConfigList poolconfList =
          client.customResources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
              .list();
      client.customResources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
          .delete(poolconfList.getItems());

      StackGresBackupConfigList bkconfList = client
          .customResources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
          .list();
      client.customResources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
          .delete(bkconfList.getItems());

      StackGresPostgresConfigList pgconfList = client
          .customResources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
          .list();
      client
          .customResources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
          .delete(pgconfList.getItems());

      StackGresProfileList instanceList = client
          .customResources(StackGresProfile.class, StackGresProfileList.class)
          .list();
      client.customResources(StackGresProfile.class, StackGresProfileList.class)
          .delete(instanceList.getItems());

      StorageClassList storageList = client.storage().storageClasses().list();
      client.storage().storageClasses().delete(storageList.getItems());
    }
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
  void given_invalidStackGresClusterName_shouldFail() {
    StackGresClusterReview clusterReview = getConstraintClusterReview();
    StackGresCluster cluster = clusterReview.getRequest().getObject();
    cluster.getMetadata().setName("postgres-13.0-to-13.1");
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.CLUSTER_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(false),
            "kind", is("AdmissionReview"),
            "response.status.code", is(422),
            "response.status.message", is("Name must consist of lower case alphanumeric "
                + "characters or '-', start with an alphabetic character, "
                + "and end with an alphanumeric character"))
        .statusCode(200);
  }

  @Test
  void given_invalidStackGresClusterLongName_shouldFail() {
    StackGresClusterReview clusterReview = getConstraintClusterReview();
    StackGresCluster cluster = clusterReview.getRequest().getObject();
    cluster.getMetadata().setName("husked-condition-calculus-ridden-pancreas-heave-extented");
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.CLUSTER_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(false),
            "kind", is("AdmissionReview"),
            "response.status.code", is(422),
            "response.status.message", is("Valid name must be 53 characters or less"))
        .statusCode(200);
  }

  @Test
  void given_withoutValidStorageClass_shouldFail() {
    var storage = readFromJson("storage_class/standard.json", StorageClass.class);
    factory.create().storage().storageClasses().withName(storage.getMetadata().getName()).delete();

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

    factory.create().storage().storageClasses().create(storage);
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
            is("Extensions dblink, pg_stat_statements, plpgsql, plpython3u are missing."))
        .statusCode(200);
  }

}
