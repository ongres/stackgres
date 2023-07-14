/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.storage.StorageClassList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.testutil.JsonUtil;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
class ShardedClusterValidationQuarkusTest {

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

  private StackGresShardedClusterReview getConstraintClusterReview() {
    var review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getMetadata().setNamespace("test");
    review.getRequest().getObject().setStatus(new StackGresShardedClusterStatus());
    StackGresShardedClusterStatus status = review.getRequest().getObject().getStatus();
    status.setToInstallPostgresExtensions(
        getInstalledExtension("citus", "citus_columnar"));
    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion("15.2");

    return review;
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
    client.resourceList(poolconfList.getItems()).delete();
    var poolConfig = Fixtures.poolingConfig().loadDefault().get();
    poolConfig.getMetadata().setNamespace("test");
    client.resource(poolConfig).createOrReplace();

    StackGresBackupConfigList bkconfList = client
        .resources(StackGresBackupConfig.class, StackGresBackupConfigList.class)
        .list();
    client.resourceList(bkconfList.getItems()).delete();
    var backupConfig = Fixtures.backupConfig().loadDefault().get();
    backupConfig.getMetadata().setNamespace("test");
    client.resource(backupConfig).createOrReplace();

    StackGresPostgresConfigList pgconfList = client
        .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .list();
    client.resourceList(pgconfList.getItems()).delete();
    var pgConfig = Fixtures.postgresConfig().loadDefault().get();
    pgConfig.getMetadata().setNamespace("test");
    pgConfig.getSpec().setPostgresVersion("15");
    client.resource(pgConfig).createOrReplace();

    StackGresProfileList instanceList =
        client.resources(StackGresProfile.class, StackGresProfileList.class)
            .list();
    client.resourceList(instanceList.getItems()).delete();
    var instanceConfig = Fixtures.instanceProfile().loadSizeXs().get();
    instanceConfig.getMetadata().setNamespace("test");
    client.resource(instanceConfig).createOrReplace();

    StorageClassList storageList = client.storage().v1().storageClasses().list();
    client.resourceList(storageList.getItems()).delete();
    var storage = Fixtures.storageClass().loadDefault().get();
    client.resource(storage).create();
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
    client.resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)
        .delete(pgconfList.getItems());

    StackGresProfileList instanceList = client
        .resources(StackGresProfile.class, StackGresProfileList.class)
        .list();
    client.resources(StackGresProfile.class, StackGresProfileList.class)
        .delete(instanceList.getItems());

    StorageClassList storageList = client.storage().v1().storageClasses().list();
    client.resourceList(storageList.getItems()).delete();
  }

  @Test
  void given_validStackGresClusterReview_shouldNotFail() {
    StackGresShardedClusterReview clusterReview = getConstraintClusterReview();
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.SHARDED_CLUSTER_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(true),
            "kind", is("AdmissionReview"))
        .statusCode(200);
  }

  @Test
  void given_withoutValidStorageClass_shouldFail() {
    var storage = Fixtures.storageClass().loadDefault().get();
    client.resource(storage).delete();

    StackGresShardedClusterReview clusterReview = getConstraintClusterReview();
    RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.SHARDED_CLUSTER_VALIDATION_PATH)
        .then()
        .body("response.allowed", is(false),
            "kind", is("AdmissionReview"),
            "response.status.code", is(400),
            "response.status.message", is("Storage class standard not found for coordinator"))
        .statusCode(200);

    client.resource(storage).create();
  }

  @Test
  void given_withoutInstalledExtensions_shouldFail() throws Exception {
    StackGresShardedClusterReview clusterReview = getConstraintClusterReview();
    clusterReview.getRequest().getObject().getStatus().setToInstallPostgresExtensions(null);
    InputStream is = RestAssured.given()
        .body(clusterReview)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .post(ValidationUtil.SHARDED_CLUSTER_VALIDATION_PATH)
        .then()
        .statusCode(200)
        .extract()
        .asInputStream();
    var body = JsonUtil.jsonMapper().readTree(is);
    try {
      assertThat(body.get("response").get("allowed").asBoolean(), is(false));
      assertThat(body.get("kind").asText(), is("AdmissionReview"));
      assertThat(body.get("response").get("status").get("code").asInt(), is(400));
      assertThat(body.get("response").get("status").get("message").asText(),
          is("Some extensions were not found: citus 11.3-1, citus_columnar 11.3-1"));
    } catch (AssertionError ae) {
      throw new AssertionError("Body " + body + " has unexpected values", ae);
    }
  }

}
