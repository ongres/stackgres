/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.AnyType;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

public class PatroniCtlKubernetesInstance implements PatroniCtlInstance {

  final KubernetesClient client;
  final ObjectMapper objectMapper;
  final LabelFactoryForCluster clusterLabelFactory;
  final PatroniCtlBinaryInstance patroniCtlBinaryInstance;

  final StackGresCluster cluster;
  final String namespace;
  final String name;
  final String scope;
  final Integer group;
  final String primaryName;
  final String configName;
  final int patroniMajorVersion;

  protected PatroniCtlKubernetesInstance(
      KubernetesClient client,
      StackGresCluster cluster,
      ObjectMapper objectMapper,
      LabelFactoryForCluster clusterLabelFactory,
      PatroniCtlBinaryInstance patroniCtlBinaryInstance) {
    this.client = client;
    this.cluster = cluster;
    this.objectMapper = objectMapper;
    this.clusterLabelFactory = clusterLabelFactory;
    this.patroniCtlBinaryInstance = patroniCtlBinaryInstance;
    this.namespace = cluster.getMetadata().getNamespace();
    this.name = cluster.getMetadata().getName();
    this.scope = PatroniUtil.clusterScope(cluster);
    this.group = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .flatMap(StackGresClusterPatroniConfig::getCitusGroup)
        .orElse(null);
    this.primaryName = Optional.ofNullable(group).map(group -> scope + "-" + group).orElse(scope);
    this.configName = Optional.ofNullable(group).map(group -> scope + "-" + group).orElse(scope) + "-config";
    final String patroniVersion = StackGresUtil.getPatroniVersion(cluster);
    int patroniMajorVersion = StackGresUtil.getPatroniMajorVersion(patroniVersion);
    this.patroniMajorVersion = patroniMajorVersion;
  }

  @Override
  public List<PatroniMember> list() {
    return client.pods()
        .inNamespace(namespace)
        .withLabels(clusterLabelFactory.clusterLabels(cluster))
        .list()
        .getItems()
        .stream()
        .map(pod -> Optional.of(pod)
            .map(Pod::getMetadata)
            .map(this::createMemberFromMetadata))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.groupingBy(member -> true))
        .values()
        .stream()
        .flatMap(members -> {
          var currentLocation = getPrimaryMemberLagInMb(members);
          return members.stream()
              .map(member -> setMemberLagInMb(currentLocation, member));
        })
        .toList();
  }

  @Override
  public List<PatroniHistoryEntry> history() {
    var configEndpointsHistory = Optional.ofNullable(client.endpoints()
        .inNamespace(namespace)
        .withName(configName)
        .get())
        .map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get("history"))
        .map(Unchecked.function(objectMapper::readTree))
        .filter(ArrayNode.class::isInstance)
        .map(ArrayNode.class::cast);
    if (configEndpointsHistory.isEmpty()) {
      return List.of();
    }
    return configEndpointsHistory
        .stream()
        .map(ArrayNode::elements)
        .flatMap(Seq::seq)
        .filter(ArrayNode.class::isInstance)
        .map(ArrayNode.class::cast)
        .filter(entry -> entry.size() >= 5)
        .map(entry -> {
          var historyEntry = new PatroniHistoryEntry();
          historyEntry.setTimeline(entry.get(0).asText());
          historyEntry.setLsn(entry.get(1).asText());
          historyEntry.setReason(entry.get(2).asText());
          historyEntry.setTimestamp(entry.get(3).asText());
          historyEntry.setNewLeader(entry.get(4).asText());
          return historyEntry;
        })
        .toList();
  }

  @Override
  public PatroniConfig showConfig() {
    return Optional
        .of(
            Optional.ofNullable(client.endpoints()
            .inNamespace(namespace)
            .withName(configName)
            .get())
            .orElseThrow(() -> new RuntimeException("Endpoints " + configName + " not found")))
        .map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get("config"))
        .map(Unchecked.function(config -> objectMapper.readValue(config, PatroniConfig.class)))
        .orElseThrow(() -> new RuntimeException("Annotaion config not found in Endpoints " + configName));
  }

  @Override
  public ObjectNode showConfigJson() {
    return Optional
        .of(
            Optional.ofNullable(client.endpoints()
            .inNamespace(namespace)
            .withName(configName)
            .get())
            .orElseThrow(() -> new RuntimeException("Endpoints " + configName + " not found")))
        .map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get("config"))
        .map(Unchecked.function(config -> objectMapper.readTree(config)))
        .filter(ObjectNode.class::isInstance)
        .map(ObjectNode.class::cast)
        .orElseThrow(() -> new RuntimeException("Annotaion config not found in Endpoints " + configName));
  }

  @Override
  public void editConfig(PatroniConfig patroniConfig) {
    patroniCtlBinaryInstance.editConfig(patroniConfig);
  }

  @Override
  public void editConfigJson(ObjectNode patroniConfig) {
    patroniCtlBinaryInstance.editConfigJson(patroniConfig);
  }

  @Override
  public void restart(String username, String password, String member) {
    patroniCtlBinaryInstance.restart(username, password, member);
  }

  @Override
  public void switchover(String username, String password, String leader, String candidate) {
    patroniCtlBinaryInstance.switchover(username, password, leader, candidate);
  }

  @Override
  public void remove(String username, String password) {
    Optional
        .ofNullable(client.endpoints()
            .inNamespace(namespace)
            .withName(configName)
            .edit(endpoints -> endpoints
                .edit()
                .editMetadata()
                .withAnnotations(Optional.of(endpoints)
                    .map(Endpoints::getMetadata)
                    .map(ObjectMeta::getAnnotations)
                    .map(annotations -> annotations.entrySet()
                        .stream()
                        .reduce(
                            new HashMap<String, String>(),
                            (map, entry) -> {
                              if (entry.getKey().equals("config")) {
                                map.put(entry.getKey(), entry.getValue());
                              } else if (entry.getKey().equals("history")) {
                                map.put(entry.getKey(), "[]");
                              } else {
                                map.put(entry.getKey(), "");
                              }
                              return map;
                            },
                            (u, v) -> u))
                    .orElse(null))
                .endMetadata()
                .build()));
    Optional
        .ofNullable(client.endpoints()
            .inNamespace(namespace)
            .withName(primaryName)
            .edit(endpoints -> endpoints
                .edit()
                .editMetadata()
                .withAnnotations(Optional.of(endpoints)
                    .map(Endpoints::getMetadata)
                    .map(ObjectMeta::getAnnotations)
                    .map(annotations -> annotations.entrySet()
                        .stream()
                        .map(Map.Entry::getKey)
                        .reduce(
                            new HashMap<String, String>(),
                            (map, key) -> {
                              map.put(key, "");
                              return map;
                            },
                            (u, v) -> u))
                    .orElse(null))
                .endMetadata()
                .build()));
  }

  @Override
  public JsonNode queryPrimary(String query, String username, String password) {
    return patroniCtlBinaryInstance.queryPrimary(query, username, password);
  }

  private PatroniMember createMemberFromMetadata(ObjectMeta metadata) {
    var member = new PatroniMember();
    member.setCluster(scope);
    member.setMember(metadata.getName());
    member.setGroup(new IntOrString(group));
    Optional.of(metadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get("status"))
        .ifPresent(statusAnnotation -> setMemberFieldsFromStatusAnnotation(member, statusAnnotation));
    member.setTags(getTagsFromMetadata(metadata));
    return member;
  }

  private void setMemberFieldsFromStatusAnnotation(PatroniMember member, String statusAnnotation) {
    var status = Unchecked.supplier(() -> objectMapper.readTree(statusAnnotation)).get();
    member.setHost(
        Optional.ofNullable(status.get("conn_url"))
        .map(JsonNode::asText)
        .map(connUrl -> connUrl.split("/"))
        .filter(connUrlParts -> connUrlParts.length >= 3)
        .map(connUrlParts -> connUrlParts[2])
        .orElse(null));
    member.setRoleFromLabel(
        Optional.ofNullable(status.get("role"))
        .map(JsonNode::asText)
        .orElse(null));
    member.setStateFromLabel(
        Optional.ofNullable(status.get("state"))
        .map(JsonNode::asText)
        .orElse(null));
    member.setTimeline(
        Optional.ofNullable(status.get("timeline"))
        .map(JsonNode::asText)
        .orElse(null));
    member.setLagInMb(new IntOrString(
        Optional.ofNullable(status.get("xlog_location"))
        .map(JsonNode::asText)
        .orElse(null)));
    member.setPendingRestart(
        Optional.ofNullable(status.get("pending_restart"))
        .map(JsonNode::asText)
        .orElse(null));
    member.setScheduledRestart(
        Optional.ofNullable(status.get("scheduled_restart"))
        .map(JsonNode::asText)
        .orElse(null));
  }

  private Map<String, AnyType> getTagsFromMetadata(ObjectMeta metadata) {
    return Optional.of(metadata)
    .map(ObjectMeta::getLabels)
    .map(labels -> Stream
        .of(
            "clonefrom",
            "nofailover",
            "noloadbalance",
            "nosync",
            "nostream",
            "failover_priority")
        .map(tag -> Map.entry(tag, new AnyType(labels.get(tag))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
    .orElse(null);
  }

  private Optional<IntOrString> getPrimaryMemberLagInMb(List<PatroniMember> members) {
    return members
        .stream()
        .filter(member -> member.isPrimary())
        .findAny()
        .map(PatroniMember::getLagInMb)
        .filter(current -> current.getStrVal() != null);
  }

  private PatroniMember setMemberLagInMb(Optional<IntOrString> currentLocation,
      PatroniMember member) {
    member.setLagInMb(new IntOrString(currentLocation
        .map(current -> {
          if (member.getLagInMb() == null
              || member.getLagInMb().getStrVal() == null) {
            return null;
          }
          try {
            return String.valueOf(
                Integer.parseInt(current.getStrVal())
                - Integer.parseInt(member.getLagInMb().getStrVal()));
          } catch (NumberFormatException ex) {
            return null;
          }
        })
        .orElse(null)));
    return member;
  }

}
