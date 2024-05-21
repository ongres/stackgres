/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.cloudevent;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.format.CloudEvents;
import io.stackgres.common.RetryUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetCloudEventHttp;
import io.stackgres.stream.jobs.DebeziumEngineHandler;
import io.stackgres.stream.jobs.Metrics;
import io.stackgres.stream.jobs.StateHandler;
import io.stackgres.stream.jobs.StreamEventStateHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StateHandler("CloudEvent")
public class StreamCloudEventStateHandler implements StreamEventStateHandler {

  private static final String CLOUDEVENT_ID_HEADER = "ce-id";
  private static final String CLOUDEVENT_SPECVERSION_HEADER = "ce-specversion";
  private static final String CLOUDEVENT_TYPE_HEADER = "ce-type";
  private static final String CLOUDEVENT_SOURCE_HEADER = "ce-source";
  private static final List<String> CLOUDEVENT_HEADERS =
      List.of(
          CLOUDEVENT_ID_HEADER,
          CLOUDEVENT_SPECVERSION_HEADER,
          CLOUDEVENT_TYPE_HEADER,
          CLOUDEVENT_SOURCE_HEADER);

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamCloudEventStateHandler.class);

  @Inject
  DebeziumEngineHandler debeziumEngineHandler;

  @Inject
  Metrics metrics;

  private final JsonMapper jsonMapper = new JsonMapper();

  @Override
  public CompletableFuture<Void> sendEvents(StackGresStream stream) {
    ClientBuilder brokerClientBuilder = ClientBuilder.newBuilder();
    var http = Optional.of(stream.getSpec().getTarget().getCloudEvent().getHttp());
    http
        .map(StackGresStreamTargetCloudEventHttp::getConnectTimeout)
        .map(Duration::parse)
        .ifPresent(connectTimeout -> brokerClientBuilder.connectTimeout(
            connectTimeout.get(ChronoUnit.SECONDS), TimeUnit.SECONDS));
    http
        .map(StackGresStreamTargetCloudEventHttp::getReadTimeout)
        .map(Duration::parse)
        .ifPresent(readTimeout -> brokerClientBuilder.readTimeout(
            readTimeout.get(ChronoUnit.SECONDS), TimeUnit.SECONDS));
    http
        .map(StackGresStreamTargetCloudEventHttp::getSkipHostnameVerification)
        .filter(skipHostnameVerification -> skipHostnameVerification)
        .ifPresent(skipHostnameVerification -> brokerClientBuilder.hostnameVerifier(
            (hostname, session) -> true));
    URI baseUri = URI.create(stream.getSpec().getTarget().getCloudEvent().getHttp().getUrl());
    int retryBackoffDelay = Optional.of(stream.getSpec().getTarget().getCloudEvent().getHttp())
        .map(StackGresStreamTargetCloudEventHttp::getRetryBackoffDelay)
        .orElse(60)
        * 1000;
    var retryLimit = Optional.of(stream.getSpec().getTarget().getCloudEvent().getHttp())
        .map(StackGresStreamTargetCloudEventHttp::getRetryLimit);
    final RetryHandler handler;
    if (retryLimit.isPresent()) {
      handler = new RetryWithLimitHandler(
          stream, baseUri, brokerClientBuilder.build(),
          retryBackoffDelay, retryLimit.get().intValue());
    } else {
      handler = new RetryHandler(
          stream, baseUri, brokerClientBuilder.build(),
          retryBackoffDelay);
    }
    return debeziumEngineHandler.streamChangeEvents(stream, CloudEvents.class, handler);
  }

  class RetryWithLimitHandler extends RetryHandler {

    private final int retryLimit;

    public RetryWithLimitHandler(
        StackGresStream stream, URI baseUri, Client brokerClient,
        int retryBackoffDelay, int retryLimit) {
      super(stream, baseUri, brokerClient, retryBackoffDelay);
      this.retryLimit = retryLimit;
    }

    @Override
    public void accept(ChangeEvent<String, String> changeEvent) {
      RetryUtil.retryWithLimit(() -> sendCloudEvent(changeEvent), ex -> true,
          retryLimit, retryBackoffDelay, retryBackoffDelay, retryBackoffDelay);
    }
    
  }

  class RetryHandler implements Consumer<ChangeEvent<String, String>> {
    final StackGresStream stream;
    final URI baseUri;
    final Client brokerClient;
    final int retryBackoffDelay;
    final Map<String, String> headers;

    RetryHandler(
        StackGresStream stream, URI baseUri, Client brokerClient,
        int retryBackoffDelay) {
      this.stream = stream;
      this.baseUri = baseUri;
      this.brokerClient = brokerClient;
      this.retryBackoffDelay = retryBackoffDelay;
      this.headers =
          Optional.of(stream.getSpec().getTarget().getCloudEvent().getHttp())
          .map(StackGresStreamTargetCloudEventHttp::getHeaders)
          .stream()
          .map(Map::entrySet)
          .flatMap(Set::stream)
          .filter(Predicate.not(entry -> CLOUDEVENT_HEADERS.contains(entry.getKey())))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void accept(ChangeEvent<String, String> changeEvent) {
      RetryUtil.retry(() -> sendCloudEvent(changeEvent), ex -> true,
          retryBackoffDelay * 10 / 100, retryBackoffDelay, retryBackoffDelay * 10 / 100);
    }

    void sendCloudEvent(ChangeEvent<String, String> changeEvent) {
      try {
        JsonNode recordNode = jsonMapper.readTree(changeEvent.value());
        LOGGER.trace("ChangeEvent: id:{} specversion:{} type:{}, source:{}",
            recordNode.get("id").asText(), recordNode.get("specversion").asText(),
            recordNode.get("type").asText(), recordNode.get("source").asText());
        Invocation.Builder invocationBuilder = brokerClient.target(baseUri).request();
        headers.forEach(invocationBuilder::header);
        invocationBuilder
            .header(CLOUDEVENT_ID_HEADER, recordNode.get("id").asText())
            .header(CLOUDEVENT_SPECVERSION_HEADER, recordNode.get("specversion").asText())
            .header(CLOUDEVENT_TYPE_HEADER, recordNode.get("type").asText())
            .header(CLOUDEVENT_SOURCE_HEADER, recordNode.get("source").asText());
        Response response = invocationBuilder
            .post(Entity.json(changeEvent.value()));
        if (response.getStatus() != 200) {
          metrics.incrementTotalNumberOfErrorsSeen();
          metrics.setLastEventWasSent(false);
          throw new RuntimeException("Error " + response.getStatus()
              + (response.isClosed() || !response.hasEntity() ? "" : ": " + response.readEntity(String.class)));
        }
        metrics.incrementTotalNumberOfEventsSent();
        metrics.setLastEventSent(recordNode.get("id").asText());
        metrics.setLastEventWasSent(true);
      } catch (RuntimeException ex) {
        metrics.incrementTotalNumberOfErrorsSeen();
        metrics.setLastEventWasSent(false);
        throw ex;
      } catch (Exception ex) {
        metrics.incrementTotalNumberOfErrorsSeen();
        metrics.setLastEventWasSent(false);
        throw new RuntimeException(ex);
      }
    }
  }

}

