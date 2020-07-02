/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Charsets;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.common.KubernetesClientFactory;
import okhttp3.Response;

@ApplicationScoped
public class PodExecutor {

  private final KubernetesClientFactory clientFactory;

  @Inject
  public PodExecutor(KubernetesClientFactory clientFactory) {
    super();
    this.clientFactory = clientFactory;
  }

  /**
   * Execute a command inside a container of a pod.
   */
  public List<String> exec(Pod pod, String container,
      String... args) {
    CompletableFuture<Void> completableFuture = new CompletableFuture<Void>();
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorCodeStream = new ByteArrayOutputStream();
        KubernetesClient client = clientFactory.create();
        ExecWatch execWatch = client.pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .inContainer(container)
            .writingOutput(outputStream)
            .writingError(errorStream)
            .writingErrorChannel(errorCodeStream)
            .usingListener(new ExecListener() {
              @Override
              public void onOpen(Response response) {
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
                      new String(errorCodeStream.toByteArray(), Charsets.UTF_8), Status.class);

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
                            byteArrayInputStream, Charsets.UTF_8);
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
                              .findFirst().orElse("Unknown cause") + "\n"
                              + bufferedReader.lines().collect(Collectors.joining("\n"))));
                    }
                  }

                  completableFuture.complete(null);
                } catch (Exception ex) {
                  completableFuture.completeExceptionally(ex);
                }
              }
            })
            .exec(args)) {
      completableFuture.join();

      try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
          outputStream.toByteArray());
          InputStreamReader inputStreamReader = new InputStreamReader(
              byteArrayInputStream, Charsets.UTF_8);
          BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
        return bufferedReader.lines().collect(Collectors.toList());
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
