/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniApiHandlerImpl implements PatroniApiHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniApiHandlerImpl.class);

  @Inject
  PatroniApiMetadataFinder apiFinder;
  @Inject
  Vertx vertx;
  private WebClient client;

  @Inject
  public PatroniApiHandlerImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  private static @NotNull MemberRole toMemberRole(@NotNull String role) {
    if (Objects.equals("leader", role) || Objects.equals("master", role)) {
      return MemberRole.LEADER;
    } else if (Objects.equals("replica", role)) {
      return MemberRole.REPlICA;
    } else {
      throw new IllegalArgumentException("Unknown role " + role);
    }
  }

  private static MemberState toMemberState(String state) {
    if (Objects.equals("running", state)) {
      return MemberState.RUNNING;
    } else if (Objects.equals("stopped", state)) {
      return MemberState.STOPPED;
    } else {
      return MemberState.INITIALIZING;
    }
  }

  private static Optional<Integer> parsePort(JsonObject memberJson) {
    try {
      return Optional.ofNullable(memberJson.getInteger("port"));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @PostConstruct
  void init() {
    this.client = WebClient.create(vertx);
  }

  @Override
  public Uni<List<ClusterMember>> getClusterMembers(String name, String namespace) {

    PatroniApiMetadata patroniApi = apiFinder.findPatroniRestApi(name, namespace);

    return client.get(patroniApi.getPort(), patroniApi.getHost(), "/cluster")
        .as(BodyCodec.jsonObject())
        .basicAuthentication(patroniApi.getUsername(), patroniApi.getPassword())
        .send()
        .onItem()
        .transform(res -> {
          JsonObject body = res.body();
          JsonArray membersJson = body.getJsonArray("members");
          return IntStream.rangeClosed(0, membersJson.size() - 1)
              .mapToObj(membersJson::getJsonObject)
              .map(member -> ImmutableClusterMember.builder()
                  .clusterName(name)
                  .namespace(namespace)
                  .name(member.getString("name"))
                  .role(toMemberRole(member.getString("role")))
                  .state(toMemberState(member.getString("state")))
                  .apiUrl(member.getString("api_url"))
                  .host(Optional.ofNullable(member.getString("host")))
                  .port(parsePort(member))
                  .timeline(Optional.ofNullable(member.getInteger("timeline")))
                  .lag(Optional.ofNullable(member.getInteger("lag")))
                  .build())
              .collect(Collectors.toUnmodifiableList());
        });
  }

  @Override
  public Uni<List<PatroniInformation>> getMembersPatroniInformation(String name, String namespace) {

    final Uni<List<ClusterMember>> clusterMembers = getClusterMembers(name, namespace);
    return clusterMembers.chain(this::getPatroniInformation);

  }

  private Uni<List<PatroniInformation>> getPatroniInformation(List<ClusterMember> members) {

    return Multi.createFrom().iterable(members)
        .onItem().transformToUniAndConcatenate(this::getPatroniInformation)
        .collect().asList();
  }

  private Uni<PatroniInformation> getPatroniInformation(ClusterMember member) {
    URL apiUrl = member.getApiUrl()
        .map(url -> {
          try {
            return new URL(url);
          } catch (MalformedURLException e) {
            throw new RuntimeException(e);
          }
        })
        .orElseThrow();

    return client.get(apiUrl.getPort(), apiUrl.getHost(), apiUrl.getPath())
        .as(BodyCodec.jsonObject())
        .send()
        .onItem()
        .transform(res -> {
          if (res.statusCode() == 200) {
            JsonObject body = res.body();
            JsonObject patroni = body.getJsonObject("patroni");
            return ImmutablePatroniInformation.builder()
                .role(toMemberRole(body.getString("role")))
                .state(toMemberState(body.getString("state")))
                .serverVersion(body.getInteger("server_version"))
                .patroniScope(patroni.getString("scope"))
                .patroniVersion(patroni.getString("version"))
                .build();
          } else {
            LOGGER.error("Failed restart postgres of pod {} with message {}",
                member.getName(),
                res.bodyAsString());
            throw new RuntimeException(res.bodyAsString());
          }

        });
  }

  @Override
  public Uni<Void> performSwitchover(ClusterMember leader, ClusterMember candidate) {

    PatroniApiMetadata patroniApi = apiFinder.findPatroniRestApi(
        leader.getClusterName(),
        leader.getNamespace());

    return client.post(patroniApi.getPort(), patroniApi.getHost(), "/switchover")
        .basicAuthentication(patroniApi.getUsername(), patroniApi.getPassword())
        .as(BodyCodec.string())
        .sendJsonObject(new JsonObject()
            .put("leader", leader.getName())
            .put("candidate", candidate.getName()))
        .onItem()
        .transform(res -> {
          if (res.statusCode() == 200) {
            return null;
          } else if (res.statusCode() == 412) {
            LOGGER.warn("{} is not the leader anymore, skipping", leader.getName());
            return null;
          } else {
            LOGGER.debug("Failed switchover, status {}, body {}",
                res.statusCode(),
                res.bodyAsString());
            throw new RuntimeException(res.body());
          }
        });

  }

  @Override
  public Uni<Boolean> restartPostgres(ClusterMember member) {

    URL apiUrl = member.getApiUrl()
        .map(url -> {
          try {
            return new URL(url);
          } catch (MalformedURLException e) {
            throw new RuntimeException(e);
          }
        })
        .orElseThrow();

    PatroniApiMetadata patroniApi = apiFinder.findPatroniRestApi(
        member.getClusterName(),
        member.getNamespace());

    return client.post(apiUrl.getPort(), apiUrl.getHost(), "/restart")
        .basicAuthentication(patroniApi.getUsername(), patroniApi.getPassword())
        .as(BodyCodec.string())
        .sendJsonObject(new JsonObject())
        .onItem()
        .transform(res -> {
          if (res.statusCode() == 200) {
            return true;
          } else {
            LOGGER.debug("Failed restart, status {}, body {}",
                res.statusCode(),
                res.bodyAsString());
            return false;
          }
        });
  }
}
