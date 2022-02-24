/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.Serialization;

@ApplicationScoped
public class PodExecutor {

  private KubernetesClient client;

  /**
   * Execute a command inside a container of a pod.
   */
  public List<String> exec(Pod pod, String container,
      String... args) {
    CompletableFuture<Void> completableFuture = new CompletableFuture<>();
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorCodeStream = new ByteArrayOutputStream();
        ExecWatch execWatch = client.pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .inContainer(container)
            .writingOutput(outputStream)
            .writingError(errorStream)
            .writingErrorChannel(errorCodeStream)
            .usingListener(new PodExecListener(outputStream, pod, args, completableFuture,
                errorStream, container, errorCodeStream))
            .exec(args)) {
      completableFuture.join();

      try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
          outputStream.toByteArray());
          InputStreamReader inputStreamReader = new InputStreamReader(
              byteArrayInputStream, StandardCharsets.UTF_8);
          BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
        return bufferedReader.lines().collect(Collectors.toList());
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static final class PodExecListener implements ExecListener {
    private final ByteArrayOutputStream outputStream;
    private final Pod pod;
    private final String[] args;
    private final CompletableFuture<Void> completableFuture;
    private final ByteArrayOutputStream errorStream;
    private final String container;
    private final ByteArrayOutputStream errorCodeStream;

    private PodExecListener(ByteArrayOutputStream outputStream, Pod pod, String[] args,
        CompletableFuture<Void> completableFuture, ByteArrayOutputStream errorStream,
        String container, ByteArrayOutputStream errorCodeStream) {
      this.outputStream = outputStream;
      this.pod = pod;
      this.args = args;
      this.completableFuture = completableFuture;
      this.errorStream = errorStream;
      this.container = container;
      this.errorCodeStream = errorCodeStream;
    }

    @Override
    public void onFailure(Throwable t, Response response) {
      completableFuture.completeExceptionally(t);
    }

    @Override
    public void onClose(int code, String reason) {
      try {
        outputStream.write(errorStream.toByteArray());
        Status status = Serialization.unmarshal(
            new String(errorCodeStream.toByteArray(), StandardCharsets.UTF_8),
            Status.class);

        int exitCode = status.getStatus().equals("Success") ? 0
            : Integer.parseInt(status.getDetails().getCauses().stream()
                .filter(cause -> cause.getReason() != null)
                .filter(cause -> cause.getReason().equals("ExitCode"))
                .map(StatusCause::getMessage)
                .findFirst().orElse("-1"));
        if (exitCode != 0) {
          try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
              outputStream.toByteArray());
              InputStreamReader inputStreamReader = new InputStreamReader(
                  byteArrayInputStream, StandardCharsets.UTF_8);
              BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            completableFuture.completeExceptionally(new RuntimeException(
                "Command exited with code " + exitCode + " on container "
                    + container
                    + " of pod " + pod.getMetadata().getName()
                    + " in namespace " + pod.getMetadata().getNamespace()
                    + " with arguments " + Arrays.asList(args) + ": "
                    + status.getDetails().getCauses().stream()
                        .filter(cause -> cause.getMessage() != null)
                        .map(StatusCause::getMessage)
                        .findFirst().orElse("Unknown cause")
                    + "\n"
                    + bufferedReader.lines().collect(Collectors.joining("\n"))));
          }
        }

        completableFuture.complete(null);
      } catch (IOException ex) {
        completableFuture.completeExceptionally(ex);
      }
    }
  }

  @Inject
  public void setClientFactory(KubernetesClient client) {
    this.client = client;
  }
}
