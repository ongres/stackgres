/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
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
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions().findFirst().get();

  private static final String BUILD_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedBuildMajorVersions().findFirst().get();

  @Inject
  KubernetesClient client;

  private StackGresClusterReview getConstraintClusterReview() {
    var review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getMetadata().setNamespace("test");
    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setExtensions(
        getExtension("dblink", "pg_stat_statements", "plpgsql", "plpython3u"));
    spec.setToInstallPostgresExtensions(
        getInstalledExtension("dblink", "pg_stat_statements", "plpgsql", "plpython3u"));
    spec.setDistributedLogs(null);
    spec.setInitialData(null);
    spec.getPostgres().setVersion("12.16");
    spec.getConfigurations().setSgBackupConfig(null);
    spec.getConfigurations().setBackupPath(null);

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
    client.resourceList(poolconfList).delete();
    var poolConfig = Fixtures.poolingConfig().loadDefault().get();
    poolConfig.getMetadata().setNamespace("test");
    client.resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
        .inNamespace(poolConfig.getMetadata().getNamespace())
        .withName(poolConfig.getMetadata().getName())
        .createOrReplace(poolConfig);

    StackGresBackupConfigList bkconfList = client
        .resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .list();
    client.resourceList(bkconfList).delete();
    var backupConfig = Fixtures.backupConfig().loadDefault().get();
    backupConfig.getMetadata().setNamespace("test");
    client.resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .inNamespace(backupConfig.getMetadata().getNamespace())
        .withName(backupConfig.getMetadata().getName())
        .createOrReplace(backupConfig);

    StackGresPostgresConfigList pgconfList = client
        .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .list();
    client.resourceList(pgconfList).delete();
    var pgConfig = Fixtures.postgresConfig().loadDefault().get();
    pgConfig.getMetadata().setNamespace("test");
    client.resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .inNamespace(pgConfig.getMetadata().getNamespace())
        .withName(pgConfig.getMetadata().getName())
        .createOrReplace(pgConfig);

    StackGresProfileList instanceList =
        client.resources(StackGresProfile.class, StackGresProfileList.class)
            .list();
    client.resourceList(instanceList).delete();
    var instanceConfig = Fixtures.instanceProfile().loadSizeXs().get();
    instanceConfig.getMetadata().setNamespace("test");
    client.resources(StackGresProfile.class, StackGresProfileList.class)
        .inNamespace(instanceConfig.getMetadata().getNamespace())
        .withName(instanceConfig.getMetadata().getName())
        .createOrReplace(instanceConfig);

    StorageClassList storageList = client.storage().v1().storageClasses().list();
    client.resourceList(storageList).delete();
    var storage = Fixtures.storageClass().loadDefault().get();
    client.resource(storage).create();
  }

  @AfterAll
  void tearDown() {
    StackGresPoolingConfigList poolconfList =
        client.resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)
            .list();
    client.resourceList(poolconfList).delete();

    StackGresBackupConfigList bkconfList = client
        .resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .list();
    client.resourceList(bkconfList).delete();

    StackGresPostgresConfigList pgconfList = client
        .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .list();
    client.resourceList(pgconfList).delete();

    StackGresProfileList instanceList = client
        .resources(StackGresProfile.class, StackGresProfileList.class)
        .list();
    client.resourceList(instanceList).delete();

    StorageClassList storageList = client.storage().v1().storageClasses().list();
    client.resourceList(storageList).delete();
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
    var storage = Fixtures.storageClass().loadDefault().get();
    client.storage().v1().storageClasses().withName(storage.getMetadata().getName()).delete();

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

    client.resource(storage).create();
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
            is("Some extensions were not found: dblink,"
                + " pg_stat_statements, plpgsql,"
                + " plpython3u"))
        .statusCode(200);
  }

}
