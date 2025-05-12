/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.restassured.RestAssured.given;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.resource.DbOpsScanner;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ClusterEventResourceTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  @BeforeAll
  public static void setup() {
    StackGresDbOps dbOps = new StackGresDbOps();
    dbOps.setMetadata(new ObjectMeta());
    dbOps.getMetadata().setNamespace("test-namespace");
    dbOps.getMetadata().setName("test-operation");
    dbOps.getMetadata().setUid("1");
    dbOps.setSpec(new StackGresDbOpsSpec());
    dbOps.getSpec().setSgCluster("test");
    DbOpsScanner dbOpsScanner = Mockito.mock(DbOpsScanner.class);
    Mockito.when(dbOpsScanner.getResources(ArgumentMatchers.any()))
        .thenReturn(ImmutableList.of(dbOps));
    QuarkusMock.installMockForType(dbOpsScanner, DbOpsScanner.class);
  }

  @BeforeEach
  void setUp() {
    mockServer.getClient().v1().events().inNamespace("test-namespace").delete();
  }

  @Test
  void ifNoEventsAreCreated_itShouldReturnAnEmptyArray() {
    given()
        .when()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/namespaces/test-namespace/sgclusters/test/events")
        .then().statusCode(200)
        .body("", Matchers.hasSize(0));
  }

  @Test
  void ifEventsAreCreated_itShouldReturnThenInAnArray() {
    mockServer.getClient().v1().events().inNamespace("test-namespace")
        .resource(new EventBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test.1")
            .endMetadata()
            .withType("Normal")
            .withMessage("Test")
            .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(1)))
            .withInvolvedObject(new ObjectReferenceBuilder()
                .withKind(StackGresCluster.KIND)
                .withNamespace("test-namespace")
                .withName("test")
                .withUid("1")
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events().inNamespace("test-namespace")
        .resource(new EventBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test.2")
            .endMetadata()
            .withType("Normal")
            .withMessage("All good!")
            .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(2)))
            .withInvolvedObject(new ObjectReferenceBuilder()
                .withKind("StatefulSet")
                .withNamespace("test-namespace")
                .withName("test")
                .withUid("1")
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events().inNamespace("test-namespace")
        .resource(new EventBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test.3")
            .endMetadata()
            .withType("Warning")
            .withMessage("Something wrong :(")
            .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(3)))
            .withInvolvedObject(new ObjectReferenceBuilder()
                .withKind("Pod")
                .withNamespace("test-namespace")
                .withName("test-0")
                .withUid("1")
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events().inNamespace("test-namespace")
        .resource(new EventBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test.4")
            .endMetadata()
            .withType("Normal")
            .withMessage("I am here too")
            .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(0)))
            .withInvolvedObject(new ObjectReferenceBuilder()
                .withKind(StackGresDbOps.KIND)
                .withNamespace("test-namespace")
                .withName("test-operation")
                .withUid("1")
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events().inNamespace("test-namespace")
        .resource(new EventBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test.5")
            .endMetadata()
            .withType("Normal")
            .withMessage("Test")
            .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(1)))
            .withInvolvedObject(new ObjectReferenceBuilder()
                .withKind("Node")
                .withNamespace(null)
                .withName("test")
                .withUid("1")
                .build())
            .build())
        .create();
    given()
        .when()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/namespaces/test-namespace/sgclusters/test/events")
        .then().statusCode(200)
        .body("", Matchers.hasSize(4))
        .body("[0].metadata.name", Matchers.equalTo("test.4"))
        .body("[0].type", Matchers.equalTo("Normal"))
        .body("[0].message", Matchers.equalTo("I am here too"))
        .body("[1].metadata.name", Matchers.equalTo("test.1"))
        .body("[1].type", Matchers.equalTo("Normal"))
        .body("[1].message", Matchers.equalTo("Test"))
        .body("[2].metadata.name", Matchers.equalTo("test.2"))
        .body("[2].type", Matchers.equalTo("Normal"))
        .body("[2].message", Matchers.equalTo("All good!"))
        .body("[3].metadata.name", Matchers.equalTo("test.3"))
        .body("[3].type", Matchers.equalTo("Warning"))
        .body("[3].message", Matchers.equalTo("Something wrong :("));
  }

}
