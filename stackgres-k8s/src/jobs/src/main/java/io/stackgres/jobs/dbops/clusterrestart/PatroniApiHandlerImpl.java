/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.codec.BodyCodec;

@ApplicationScoped
public class PatroniApiHandlerImpl implements PatroniApiHandler {

  @Inject
  PatroniApiMetadataFinder apiFinder;
  @Inject
  Vertx vertx;
  private WebClient client;

  @Inject
  public PatroniApiHandlerImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  private static MemberRole toMemberRole(String role) {
    if (Objects.equals("leader", role)) {
      return MemberRole.LEADER;
    } else {
      return MemberRole.REPlICA;
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
                  .port(Optional.ofNullable(member.getInteger("port")))
                  .timeline(Optional.ofNullable(member.getInteger("timeline")))
                  .lag(Optional.ofNullable(member.getInteger("lag")))
                  .build())
              .collect(Collectors.toUnmodifiableList());
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
          } else {
            throw new RuntimeException(res.body());
          }
        });

  }

}
