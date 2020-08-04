/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.distributedlogs.DistributedLogsFetcher;
import io.stackgres.apiweb.distributedlogs.FullTextSearchQuery;
import io.stackgres.apiweb.distributedlogs.ImmutableDistributedLogsQueryParameters;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterInitData;
import io.stackgres.apiweb.dto.cluster.ClusterLogEntryDto;
import io.stackgres.apiweb.dto.cluster.ClusterScriptFrom;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.dto.cluster.ConfigMapKeySelectorDto;
import io.stackgres.apiweb.dto.cluster.SecretKeySelectorDto;
import io.stackgres.apiweb.resource.ResourceTransactionHandler;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Path("/stackgres/sgcluster")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterResource
    extends AbstractRestService<ClusterDto, StackGresCluster> {

  private static final String DEFAULT_SCRIPT_KEY = "script";
  private final CustomResourceScanner<ClusterDto> clusterScanner;
  private final CustomResourceFinder<ClusterDto> clusterFinder;
  private final CustomResourceFinder<ClusterStatsDto> clusterResourceStatsFinder;
  private final DistributedLogsFetcher distributedLogsFetcher;
  private final ResourceTransactionHandler<Secret> secretTransactionHandler;
  private final ResourceTransactionHandler<ConfigMap> configMapTransactionHandler;

  @Inject
  public ClusterResource(
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      ResourceTransformer<ClusterDto, StackGresCluster> transformer,
      CustomResourceScanner<ClusterDto> clusterScanner,
      CustomResourceFinder<ClusterDto> clusterFinder,
      CustomResourceFinder<ClusterStatsDto> clusterResourceStatsFinder,
      DistributedLogsFetcher distributedLogsFetcher,
      ResourceTransactionHandler<Secret> secretTransactionHandler,
      ResourceTransactionHandler<ConfigMap> configMapTransactionHandler) {
    super(null, finder, scheduler, transformer);
    this.clusterScanner = clusterScanner;
    this.clusterFinder = clusterFinder;
    this.clusterResourceStatsFinder = clusterResourceStatsFinder;
    this.distributedLogsFetcher = distributedLogsFetcher;
    this.secretTransactionHandler = secretTransactionHandler;
    this.configMapTransactionHandler = configMapTransactionHandler;
  }

  public ClusterResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.clusterScanner = null;
    this.clusterFinder = null;
    this.clusterResourceStatsFinder = null;
    this.distributedLogsFetcher = null;
    this.secretTransactionHandler = null;
    this.configMapTransactionHandler = null;
  }

  @Authenticated
  @Override
  public List<ClusterDto> list() {
    return clusterScanner.getResources();
  }

  @Authenticated
  @Override
  public ClusterDto get(String namespace, String name) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

  @Authenticated
  @Override
  public void create(ClusterDto resource) {
    Deque<Secret> secretsToCreate = getSecretsToCreate(resource);
    Deque<ConfigMap> configMapsToCreate = getConfigMapsToCreate(resource);

    createSecrets(secretsToCreate,
        () -> createConfigMaps(configMapsToCreate,
            () -> super.create(resource)));
  }

  public void createSecrets(Deque<Secret> secrets, Runnable transaction) {
    Secret secret = secrets.poll();
    if (secret != null) {
      secretTransactionHandler.create(secret, () -> createSecrets(secrets, transaction));
    } else {
      transaction.run();
    }
  }

  public void createConfigMaps(Deque<ConfigMap> configMaps, Runnable transaction) {
    ConfigMap configMap = configMaps.poll();
    if (configMap != null) {
      configMapTransactionHandler.create(configMap,
          () -> createConfigMaps(configMaps, transaction));
    } else {
      transaction.run();
    }
  }

  private Deque<ConfigMap> getConfigMapsToCreate(ClusterDto resource) {
    return Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getInitData)
        .map(ClusterInitData::getScripts)
        .map(clusterScriptEntries -> Seq.zipWithIndex(clusterScriptEntries)
            .filter(entry -> entry.v1.getScriptFrom() != null)
            .filter(entry -> entry.v1.getScriptFrom().getConfigMapScript() != null)
            .map(tuple -> {
              ClusterScriptFrom clusterScriptFrom = tuple.v1.getScriptFrom();
              ConfigMapKeySelectorDto configMapKeyRef = clusterScriptFrom.getConfigMapKeyRef();
              final String configMapScript = clusterScriptFrom.getConfigMapScript();
              if (configMapKeyRef != null) {
                return new ConfigMapBuilder()
                    .withNewMetadata()
                    .withName(configMapKeyRef.getName())
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(configMapKeyRef.getKey(),
                        configMapScript))
                    .build();
              } else {
                final String configMapName = tuple.v1.getName() != null ? tuple.v1.getName() :
                    resource.getMetadata().getName() + "-init-script-" + tuple.v2;
                configMapKeyRef = new ConfigMapKeySelectorDto();
                configMapKeyRef.setName(configMapName);
                configMapKeyRef.setKey(DEFAULT_SCRIPT_KEY);
                clusterScriptFrom.setConfigMapKeyRef(configMapKeyRef);
                return new ConfigMapBuilder()
                    .withNewMetadata()
                    .withName(configMapName)
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(DEFAULT_SCRIPT_KEY,
                        configMapScript))
                    .build();
              }
            }).collect(Collectors.toCollection(ArrayDeque::new))
        ).orElse(new ArrayDeque<>());
  }

  private Deque<Secret> getSecretsToCreate(ClusterDto resource) {
    return Optional
        .ofNullable(resource.getSpec())
        .map(ClusterSpec::getInitData)
        .map(ClusterInitData::getScripts)
        .map(clusterScriptEntries -> Seq.zipWithIndex(clusterScriptEntries)
            .filter(entry -> entry.v1.getScriptFrom() != null)
            .filter(entry -> entry.v1.getScriptFrom().getSecretScript() != null)
            .map(tuple -> {
              ClusterScriptFrom clusterScriptFrom = tuple.v1.getScriptFrom();
              SecretKeySelectorDto secretKeyRef = clusterScriptFrom.getSecretKeyRef();

              final String secretScript = encodeScript(clusterScriptFrom.getSecretScript());

              if (secretKeyRef != null) {
                return new SecretBuilder()
                    .withNewMetadata()
                    .withName(clusterScriptFrom.getSecretKeyRef().getName())
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(clusterScriptFrom.getSecretKeyRef().getKey(),
                        secretScript))
                    .build();
              } else {
                final String secretName = tuple.v1.getName() != null ? tuple.v1.getName() :
                    resource.getMetadata().getName() + "-init-script-" + tuple.v2;
                secretKeyRef = new SecretKeySelectorDto();
                secretKeyRef.setName(secretName);
                secretKeyRef.setKey(DEFAULT_SCRIPT_KEY);
                clusterScriptFrom.setSecretKeyRef(secretKeyRef);
                return new SecretBuilder()
                    .withNewMetadata()
                    .withName(secretName)
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(DEFAULT_SCRIPT_KEY,
                        secretScript))
                    .build();
              }
            }).collect(Collectors.toCollection(ArrayDeque::new))).orElse(new ArrayDeque<>());
  }

  private static String encodeScript(String script) {
    final byte[] encodedScript = Base64.getEncoder()
        .encode(script.getBytes(StandardCharsets.UTF_8));
    return new String(encodedScript, StandardCharsets.UTF_8);
  }


  /**
   * Return a {@code ClusterStatus}.
   */
  @GET
  @Path("/stats/{namespace}/{name}")
  @Authenticated
  public ClusterStatsDto stats(@PathParam("namespace") String namespace,
                               @PathParam("name") String name) {
    return clusterResourceStatsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

  /**
   * Query distributed logs and return a list of {@code ClusterLogEntry}.
   */
  @GET
  @Path("/logs/{namespace}/{name}")
  @Authenticated
  public List<ClusterLogEntryDto> logs(
      @PathParam("namespace") String namespace,
      @PathParam("name") String name,
      @QueryParam("records") Integer records,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @QueryParam("sort") String sort,
      @QueryParam("text") String text,
      @QueryParam("logType") String logType,
      @QueryParam("podName") String podName,
      @QueryParam("role") String role,
      @QueryParam("errorLevel") String errorLevel,
      @QueryParam("userName") String userName,
      @QueryParam("databaseName") String databaseName,
      @QueryParam("fromInclusive") Boolean fromInclusive) {
    final ClusterDto cluster = clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);

    final int calculatedRecords = Optional.ofNullable(records).orElse(50);

    if (calculatedRecords <= 0) {
      throw new BadRequestException("records should be a positive number");
    }

    final Optional<Tuple2<Instant, Integer>> fromTuple;
    final Optional<Tuple2<Instant, Integer>> toTuple;

    if (!Optional.ofNullable(cluster.getSpec())
        .map(ClusterSpec::getDistributedLogs)
        .map(ClusterDistributedLogs::getDistributedLogs)
        .isPresent()) {
      throw new BadRequestException(
          "Distributed logs are not configured for specified cluster");
    }

    final ImmutableMap.Builder<String, Optional<String>> filters =
        ImmutableMap.<String, Optional<String>>builder();
    if (logType != null) {
      filters.put("logType", Optional.of(logType)
          .filter(value -> !value.isEmpty()));
    }
    if (podName != null) {
      filters.put("podName", Optional.of(podName)
          .filter(value -> !value.isEmpty()));
    }
    if (role != null) {
      filters.put("role", Optional.of(role)
          .filter(value -> !value.isEmpty()));
    }
    if (errorLevel != null) {
      filters.put("errorLevel", Optional.of(errorLevel)
          .filter(value -> !value.isEmpty()));
    }
    if (userName != null) {
      filters.put("userName", Optional.of(userName)
          .filter(value -> !value.isEmpty()));
    }
    if (databaseName != null) {
      filters.put("databaseName", Optional.of(databaseName)
          .filter(value -> !value.isEmpty()));
    }

    try {
      fromTuple = Optional.ofNullable(from)
          .map(s -> s.split(","))
          .map(ss -> Tuple.tuple(ss[0],
              ss.length > 1 ? ss[1] : String.valueOf(Integer.valueOf(0))))
          .map(t -> t.map1(Instant::parse))
          .map(t -> t.map2(Integer::valueOf));
    } catch (Exception ex) {
      throw new BadRequestException("from should be a timestamp"
          + " or a timestamp and an index separated by character ','", ex);
    }

    try {
      toTuple = Optional.ofNullable(to)
          .map(s -> s.split(","))
          .map(ss -> Tuple.tuple(ss[0],
              ss.length > 1 ? ss[1] : String.valueOf(Integer.MAX_VALUE)))
          .map(t -> t.map1(Instant::parse))
          .map(t -> t.map2(Integer::valueOf));
    } catch (Exception ex) {
      throw new BadRequestException("to should be a timestamp"
          + " or a timestamp and an index separated by character ','", ex);
    }

    if (sort != null && !sort.equals("asc") && !sort.equals("desc")) {
      throw new BadRequestException("sort only accept asc or desc values");
    }

    return distributedLogsFetcher.logs(
        ImmutableDistributedLogsQueryParameters.builder()
            .cluster(cluster)
            .records(calculatedRecords)
            .fromTimeAndIndex(fromTuple)
            .toTimeAndIndex(toTuple)
            .filters(filters.build())
            .isSortAsc(Objects.equals("asc", sort))
            .fullTextSearchQuery(Optional.ofNullable(text)
                .map(FullTextSearchQuery::new))
            .isFromInclusive(Optional.ofNullable(fromInclusive).orElse(false))
            .build());
  }
}
