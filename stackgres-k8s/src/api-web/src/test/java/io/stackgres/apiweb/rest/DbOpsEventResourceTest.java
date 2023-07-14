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
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.StackGresKubernetesMockServerSetup;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class DbOpsEventResourceTest implements AuthenticatedResourceTest {

  @KubernetesTestServer
  KubernetesServer mockServer;

  @BeforeEach
  void setUp() {
    mockServer.getClient().v1().events().inNamespace("test-namespace").delete();
  }

  @Test
  void ifNoEventsAreCreated_itShouldReturnAnEmptyArray() {
    given()
        .when()
        .header(AUTHENTICATION_HEADER)
        .get("/stackgres/namespaces/test-namespace/sgdbops/test/events")
        .then().statusCode(200)
        .body("", Matchers.hasSize(0));
  }

  @Test
  void ifEventsAreCreated_itShouldReturnThenInAnArray() {
    Job testJob = mockServer.getClient().batch().v1().jobs()
        .resource(new JobBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test-job")
            .withUid("1")
            .withOwnerReferences(ImmutableList.of(new OwnerReferenceBuilder()
                .withKind(StackGresDbOps.KIND)
                .withName("test")
                .withUid("1")
                .build()))
            .endMetadata()
            .build())
        .create();
    Pod testPod = mockServer.getClient().pods()
        .resource(new PodBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test-pod")
            .withUid("1")
            .withOwnerReferences(ImmutableList.of(new OwnerReferenceBuilder()
                .withKind("Job")
                .withName("test-job")
                .withUid("1")
                .build()))
            .endMetadata()
            .build())
        .create();
    mockServer.getClient().v1().events()
        .resource(new EventBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test.1")
            .endMetadata()
            .withType("Normal")
            .withMessage("Test")
            .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(1)))
            .withInvolvedObject(new ObjectReferenceBuilder()
                .withKind(StackGresDbOps.KIND)
                .withNamespace("test-namespace")
                .withName("test")
                .withUid("1")
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events()
        .resource(new EventBuilder()
            .withNewMetadata()
            .withNamespace("test-namespace")
            .withName("test.2")
            .endMetadata()
            .withType("Normal")
            .withMessage("All good!")
            .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(2)))
            .withInvolvedObject(new ObjectReferenceBuilder()
                .withKind("Job")
                .withNamespace("test-namespace")
                .withName("test-job")
                .withUid(testJob.getMetadata().getUid())
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events()
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
                .withName("test-pod")
                .withUid(testPod.getMetadata().getUid())
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events()
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
                .withName("test")
                .withUid("1")
                .build())
            .build())
        .create();
    mockServer.getClient().v1().events()
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
        .get("/stackgres/namespaces/test-namespace/sgdbops/test/events")
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
