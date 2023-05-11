/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.http.ContentType;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.transformer.ObjectStorageTransformerTest;
import io.stackgres.common.StackGresKubernetesMockServerSetup;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterScanner;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.ObjectStorageScanner;
import io.stackgres.common.resource.ObjectStorageScheduler;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class ObjectStorageResourceTest implements AuthenticatedResourceTest {

  @InjectMock
  ObjectStorageScheduler scheduler;

  @InjectMock
  ObjectStorageFinder finder;

  @InjectMock
  ObjectStorageScanner scanner;

  @InjectMock
  ClusterScanner clusterScanner;

  @Test
  @DisplayName("Given a created list of object resources it should list them")
  void testObjectStorageList() {

    var objectStorageTuple = ObjectStorageTransformerTest.createObjectStorage();
    doReturn(
        List.of(objectStorageTuple.source())
    ).when(scanner).getResources();

    String namespace = objectStorageTuple.source().getMetadata().getNamespace();
    String storageName = objectStorageTuple.source().getMetadata().getName();

    var clusters = objectStorageTuple.target().getStatus().getClusters()
        .stream().map(clusterName -> {
          var cluster = Fixtures.cluster().loadDefault().get();
          cluster.getMetadata().setName(clusterName);
          cluster.getMetadata().setNamespace(namespace);
          var backupConfiguration = new StackGresClusterBackupConfiguration();
          backupConfiguration.setObjectStorage(storageName);
          cluster.getSpec().getConfiguration()
              .setBackups(List.of(backupConfiguration));
          return cluster;
        }).collect(Collectors.toList());

    when(clusterScanner.getResources()).thenReturn(clusters);

    var response = given()
        .header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .get("/stackgres/sgobjectstorages")
        .then()
        .statusCode(200)
        .extract()
        .as(ObjectStorageDto[].class);

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(List.of(objectStorageTuple.target())),
        JsonUtil.toJson(Arrays.asList(response))
    );

  }

  @Test
  @DisplayName("The object storage creation should not fail")
  void testObjectStorageCreation() {

    var objectStorageTuple = ObjectStorageTransformerTest
        .createObjectStorage();

    when(scheduler.create(any()))
        .then(
            (Answer<StackGresObjectStorage>) invocationOnMock -> invocationOnMock
                .getArgument(0, StackGresObjectStorage.class)
        );
    objectStorageTuple.target().getSpec().getS3().getCredentials()
        .setAccessKey(StringUtils.getRandomString());
    objectStorageTuple.target().getSpec().getS3().getCredentials()
        .setSecretKey(StringUtils.getRandomString());
    objectStorageTuple.target().getSpec().getS3().getCredentials().setSecretKeySelectors(null);
    objectStorageTuple.target().getSpec().getS3().setStorageClass(null);
    objectStorageTuple.target().setStatus(null);

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(objectStorageTuple.target())
        .post("/stackgres/sgobjectstorages")
        .then()
        .statusCode(204);

    verify(scheduler).create(any());
  }

  @Test
  @DisplayName("The object storage update should not fail")
  void testObjectStorageUpdate() {

    var objectStorageTuple = ObjectStorageTransformerTest
        .createObjectStorage();

    String namespace = objectStorageTuple.source().getMetadata().getNamespace();
    String name = objectStorageTuple.source().getMetadata().getName();

    when(finder.findByNameAndNamespace(name, namespace)).thenReturn(
        Optional.of(objectStorageTuple.source())
    );

    when(scheduler.update(any(), any()))
        .then(
            (Answer<StackGresObjectStorage>) invocationOnMock -> invocationOnMock
                .getArgument(0, StackGresObjectStorage.class)
        );

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(objectStorageTuple.target())
        .put("/stackgres/sgobjectstorages")
        .then()
        .statusCode(204);

    verify(scheduler).update(any(), any());
  }

  @Test
  @DisplayName("The object storage dalete should not fail")
  void testObjectStorageDeletion() {

    var objectStorageTuple = ObjectStorageTransformerTest
        .createObjectStorage();

    String namespace = objectStorageTuple.source().getMetadata().getNamespace();
    String name = objectStorageTuple.source().getMetadata().getName();

    when(finder.findByNameAndNamespace(name, namespace)).thenReturn(
        Optional.of(objectStorageTuple.source())
    );

    doNothing().when(scheduler).delete(any());

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(objectStorageTuple.target())
        .delete("/stackgres/sgobjectstorages")
        .then()
        .statusCode(204);

    verify(scheduler).delete(any());
  }
}
