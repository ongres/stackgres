/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.testutil.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PatroniApiHandlerImplTest {

  Vertx vertx = Vertx.vertx();

  HttpServer mockServer;

  @InjectMock
  PatroniApiMetadataFinderImpl patroniApiFinder;

  @Inject
  PatroniApiHandlerImpl patroniApiHandler;

  String username = StringUtils.getRandomString();
  String password = StringUtils.getRandomString();

  String clusterName = StringUtils.getRandomString();
  String namespace = StringUtils.getRandomString();

  @BeforeEach
  void setUp() throws InterruptedException {

    AuthProvider authProvider = (jsonObject, handler) -> {
      String givenUsername = jsonObject.getString("username");
      String givenPassword = jsonObject.getString("password");

      if (username.equals(givenUsername) && password.equals(givenPassword)) {
        handler.handle(Future.succeededFuture());
      } else {
        handler.handle(Future.failedFuture("Invalid credentials"));
      }
    };
    AuthHandler basicAuthHandler = BasicAuthHandler.create(authProvider);

    Router router = Router.router(vertx);

    router.route(HttpMethod.GET, "/cluster")
        .produces("application/json")
        .handler(ctx -> ctx.response().end(getClustersResponse()));
    router.route(HttpMethod.GET, "/patroni")
        .produces("application/json")
        .handler(ctx -> ctx.response().end(getPatroniResponse()));

    router.route(HttpMethod.POST, "/switchover")
        .consumes("application/json")
        .handler(BodyHandler.create())
        .handler(basicAuthHandler)
        .handler(ctx -> {
          if (ctx.getBodyAsString() == null) {
            ctx.response().setStatusCode(400)
                .end("Switchover endpoint should not have an empty body");
          } else {
            String candidateName = ctx.getBodyAsJson().getString("candidate");
            ctx.response()
                .setStatusCode(200)
                .end("Successfully switched over to " + candidateName);
          }

        });

    router.route(HttpMethod.POST, "/restart")
        .consumes("application/json")
        .handler(BodyHandler.create())
        .handler(basicAuthHandler)
        .handler(ctx -> {
          ctx.response().setStatusCode(200)
              .end();
        });

    VertxTestContext testContext = new VertxTestContext();
    mockServer = vertx.createHttpServer().requestHandler(router)
        .listen(0, ar -> {
          testContext.succeeding();
          testContext.completeNow();
        });

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
  }

  private String getClustersResponse() {
    try {
      final String response = Files.readString(Path.of("src/test/resources/patroni/clusters.json"))
          .replaceAll("127\\.0\\.0\\.1:8008", "127.0.0.1:"
              + mockServer.actualPort());
      return response;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getPatroniResponse() {
    try {
      String patroniFile = new Random().nextInt(2) == 0 ? "replica" : "primary";
      return Files.readString(Path.of(String
          .format("src/test/resources/patroni/patroni-%s.json", patroniFile)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void preparePatroniMetadata(String username, String password) {
    when(patroniApiFinder.findPatroniRestApi(clusterName, namespace))
        .thenReturn(ImmutablePatroniApiMetadata.builder()
            .host("127.0.0.1")
            .port(mockServer.actualPort())
            .username(username)
            .password(password)
            .build());
  }

  @Test
  void givenValidCredentials_shouldRetrieveClusterMembers() {

    preparePatroniMetadata(username, password);

    var members = patroniApiHandler.getClusterMembers(clusterName, namespace)
        .await().atMost(Duration.ofSeconds(5));

    final JsonObject expected = (JsonObject) Json.decodeValue(getClustersResponse());

    assertEquals(expected.getJsonArray("members").size(), members.size());

  }

  @Test
  void patroniInformation_shouldNotFail() {

    preparePatroniMetadata(username, password);

    var membersInformation = patroniApiHandler
        .getMembersPatroniInformation(clusterName, namespace)
        .await()
        .atMost(Duration.ofSeconds(5));

    final JsonObject expected = (JsonObject) Json.decodeValue(getClustersResponse());

    assertEquals(expected.getJsonArray("members").size(), membersInformation.size());
  }

  @Test
  void givenValidCredentials_shouldPerformSwitchOver() {

    preparePatroniMetadata(username, password);

    patroniApiHandler.performSwitchover(
        ImmutableClusterMember.builder()
            .clusterName(clusterName)
            .namespace(namespace)
            .name("leader-member")
            .state(MemberState.RUNNING)
            .role(MemberRole.LEADER)
            .build(),
        ImmutableClusterMember.builder()
            .clusterName(clusterName)
            .namespace(namespace)
            .name("replica-member")
            .state(MemberState.RUNNING)
            .role(MemberRole.REPlICA)
            .build())
        .await()
        .atMost(Duration.ofSeconds(5));
  }

  @Test
  void givenBadCredentials_switchoverShouldFail() {
    preparePatroniMetadata(username, StringUtils.getRandomString());

    assertThrows(RuntimeException.class, () -> patroniApiHandler.performSwitchover(
        ImmutableClusterMember.builder()
            .clusterName(clusterName)
            .namespace(namespace)
            .name("leader-member")
            .state(MemberState.RUNNING)
            .role(MemberRole.LEADER)
            .build(),
        ImmutableClusterMember.builder()
            .clusterName(clusterName)
            .namespace(namespace)
            .name("replica-member")
            .state(MemberState.RUNNING)
            .role(MemberRole.REPlICA)
            .build())
        .await()
        .atMost(Duration.ofSeconds(5)));

  }

  @Test
  void givenValidCredentials_shouldRestartPostgres() {
    preparePatroniMetadata(username, password);

    ClusterMember leader = ImmutableClusterMember.builder()
        .clusterName(clusterName)
        .namespace(namespace)
        .name("leader-member")
        .state(MemberState.RUNNING)
        .role(MemberRole.LEADER)
        .apiUrl("http://127.0.0.1:" + mockServer.actualPort() + "/patroni")
        .build();

    patroniApiHandler.restartPostgres(leader)
        .await()
        .atMost(Duration.ofSeconds(5));

  }

  @Test
  void givenBadCrendeltials_restartShouldFail() {

    preparePatroniMetadata(username, StringUtils.getRandomString());

    ClusterMember leader = ImmutableClusterMember.builder()
        .clusterName(clusterName)
        .namespace(namespace)
        .name("leader-member")
        .state(MemberState.RUNNING)
        .role(MemberRole.LEADER)
        .apiUrl("http://127.0.0.1:" + mockServer.actualPort() + "/patroni")
        .build();

    assertThrows(RuntimeException.class, () -> patroniApiHandler.restartPostgres(leader)
        .await()
        .atMost(Duration.ofSeconds(5)));
  }

  @AfterEach
  void tearDown() {
    mockServer.close();
  }
}
