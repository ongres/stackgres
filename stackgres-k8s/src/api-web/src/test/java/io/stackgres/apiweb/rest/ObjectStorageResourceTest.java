/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.ClusterScanner;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.ObjectStorageScanner;
import io.stackgres.common.resource.ObjectStorageScheduler;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StackGresKubernetesMockServerSetup;
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
        List.of(objectStorageTuple.getSource())
    ).when(scanner).getResources();

    String namespace = objectStorageTuple.getSource().getMetadata().getNamespace();
    String storageName = objectStorageTuple.getSource().getMetadata().getName();

    var clusters = objectStorageTuple.getTarget().getStatus().getClusters()
        .stream().map(clusterName -> {
          var cluster = JsonUtil
              .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
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

    assertEquals(
        List.of(objectStorageTuple.getTarget()),
        Arrays.asList(response)
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
    objectStorageTuple.getTarget().getMetadata().setNamespace(null);
    objectStorageTuple.getTarget().getSpec().getS3().getCredentials()
        .setAccessKey(StringUtils.getRandomString());
    objectStorageTuple.getTarget().getSpec().getS3().getCredentials()
        .setSecretKey(StringUtils.getRandomString());
    objectStorageTuple.getTarget().getSpec().getS3().getCredentials().setSecretKeySelectors(null);
    objectStorageTuple.getTarget().getSpec().getS3().setStorageClass(null);
    objectStorageTuple.getTarget().setStatus(null);

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(objectStorageTuple.getTarget())
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

    String namespace = objectStorageTuple.getSource().getMetadata().getNamespace();
    String name = objectStorageTuple.getSource().getMetadata().getName();

    when(finder.findByNameAndNamespace(name, namespace)).thenReturn(
        Optional.of(objectStorageTuple.getSource())
    );

    when(scheduler.update(any()))
        .then(
            (Answer<StackGresObjectStorage>) invocationOnMock -> invocationOnMock
                .getArgument(0, StackGresObjectStorage.class)
        );

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(objectStorageTuple.getTarget())
        .put("/stackgres/sgobjectstorages")
        .then()
        .statusCode(204);

    verify(scheduler).update(any());
  }

  @Test
  @DisplayName("The object storage dalete should not fail")
  void testObjectStorageDeletion() {

    var objectStorageTuple = ObjectStorageTransformerTest
        .createObjectStorage();

    String namespace = objectStorageTuple.getSource().getMetadata().getNamespace();
    String name = objectStorageTuple.getSource().getMetadata().getName();

    when(finder.findByNameAndNamespace(name, namespace)).thenReturn(
        Optional.of(objectStorageTuple.getSource())
    );

    doNothing().when(scheduler).delete(any());

    given().header(AUTHENTICATION_HEADER)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(objectStorageTuple.getTarget())
        .delete("/stackgres/sgobjectstorages")
        .then()
        .statusCode(204);

    verify(scheduler).delete(any());
  }
}
