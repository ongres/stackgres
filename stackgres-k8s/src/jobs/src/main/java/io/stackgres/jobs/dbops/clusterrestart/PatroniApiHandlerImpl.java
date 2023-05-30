/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniApiHandlerImpl implements PatroniApiHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniApiHandlerImpl.class);

  @Inject
  PatroniApiMetadataFinder apiFinder;

  private final WebClient client;

  @Inject
  public PatroniApiHandlerImpl(Vertx vertx) {
    this.client = WebClient.create(vertx, new WebClientOptions()
        .setConnectTimeout((int) Duration.ofSeconds(5).toMillis()));
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
          if (res.statusCode() == 200) {
            JsonObject body = res.body();
            JsonArray membersJson = body.getJsonArray("members");
            return IntStream.range(0, membersJson.size())
                .mapToObj(membersJson::getJsonObject)
                .map(member -> ImmutableClusterMember.builder()
                    .clusterName(name)
                    .namespace(namespace)
                    .name(member.getString("name"))
                    .state(getStringOrEmpty(member, "state")
                        .map(this::toMemberState))
                    .role(getStringOrEmpty(member, "role")
                        .map(this::toMemberRole))
                    .apiUrl(getStringOrEmpty(member, "api_url"))
                    .host(getStringOrEmpty(member, "host"))
                    .port(getIntegerOrEmpty(member, "port"))
                    .timeline(getIntegerOrEmpty(member, "timeline"))
                    .lag(getIntegerOrEmpty(member, "lag"))
                    .tags(getMapOrEmpty(member, "tags"))
                    .build())
                .map(ClusterMember.class::cast)
                .collect(Collectors.toUnmodifiableList());
          } else {
            LOGGER.error("Can not retrieve cluster members with message {}",
                res.bodyAsString());
            throw new RuntimeException("status " + res.statusCode() + ": " + res.bodyAsString());
          }
        });
  }

  @Override
  public Uni<List<PatroniInformation>> getClusterMembersPatroniInformation(
      String name, String namespace) {
    final Uni<List<ClusterMember>> clusterMembers = getClusterMembers(name, namespace);
    return clusterMembers.chain(this::getPatroniInformationForClusterMembers);
  }

  private Uni<List<PatroniInformation>> getPatroniInformationForClusterMembers(
      List<ClusterMember> members) {
    return Multi.createFrom().iterable(members)
        .onItem().transformToUniAndConcatenate(this::getClusterMemberPatroniInformation)
        .collect().asList();
  }

  @Override
  public Uni<PatroniInformation> getClusterMemberPatroniInformation(ClusterMember member) {
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
            JsonObject instance = res.body();
            JsonObject patroni = instance.getJsonObject("patroni");
            return ImmutablePatroniInformation.builder()
                .role(getStringOrEmpty(instance, "role")
                    .map(this::toMemberRole))
                .state(getStringOrEmpty(instance, "state")
                    .map(this::toMemberState))
                .serverVersion(getIntegerOrEmpty(instance, "server_version"))
                .patroniScope(getStringOrEmpty(patroni, "scope"))
                .patroniVersion(getStringOrEmpty(patroni, "version"))
                .isPendingRestart(getBooleanOrFalse(instance, "pending_restart"))
                .build();
          } else {
            LOGGER.error("Can not retrieve patroni information of pod {} with message {}",
                member.getName(),
                res.bodyAsString());
            throw new RuntimeException("status " + res.statusCode() + ": " + res.bodyAsString());
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
            throw new RuntimeException("status " + res.statusCode() + ": " + res.bodyAsString());
          }
        });

  }

  @Override
  public Uni<Void> restartPostgres(ClusterMember member) {

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
          if (res.statusCode() != 200) {
            LOGGER.debug("Failed restart, status {}, body {}",
                res.statusCode(),
                res.bodyAsString());
            throw new RuntimeException("Failed restart, status "
                + res.statusCode() + ", body " + res.bodyAsString());
          }
          return null;
        });
  }

  private Optional<String> getStringOrEmpty(JsonObject object, String key) {
    try {
      return Optional.ofNullable(object.getString(key));
    } catch (ClassCastException ex) {
      return Optional.empty();
    }
  }

  private Optional<Integer> getIntegerOrEmpty(JsonObject object, String key) {
    try {
      return Optional.ofNullable(object.getInteger(key));
    } catch (ClassCastException ex) {
      return Optional.empty();
    }
  }

  private boolean getBooleanOrFalse(JsonObject object, String key) {
    try {
      return Optional.ofNullable(object.getBoolean(key)).orElse(false);
    } catch (ClassCastException ex) {
      return false;
    }
  }

  private Optional<Map<String, String>> getMapOrEmpty(JsonObject object, String key) {
    try {
      return Optional.ofNullable(object.getJsonObject(key))
          .map(jsonObject -> Seq.seq(jsonObject.fieldNames())
              .collect(ImmutableMap.toImmutableMap(
                  Function.identity(),
                  name -> jsonObject.getValue(name).toString())));
    } catch (ClassCastException ex) {
      return Optional.empty();
    }
  }

  private MemberRole toMemberRole(String role) {
    if (Objects.equals("leader", role)
        || Objects.equals("master", role)
        || Objects.equals("standby_leader", role)) {
      return MemberRole.LEADER;
    } else if (Objects.equals("replica", role)) {
      return MemberRole.REPLICA;
    } else {
      return null;
    }
  }

  private MemberState toMemberState(String state) {
    if (Objects.equals("running", state)) {
      return MemberState.RUNNING;
    } else if (Objects.equals("stopped", state)) {
      return MemberState.STOPPED;
    } else if (Objects.equals("starting", state)) {
      return MemberState.STARTING;
    } else if (Objects.equals("restarting", state)) {
      return MemberState.RESTARTING;
    } else {
      return null;
    }
  }

}
