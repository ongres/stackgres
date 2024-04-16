/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.application.bbfcompass;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.stackgres.apiweb.application.ApplicationsConfig;
import io.stackgres.apiweb.application.SgApplication;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StringUtil;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BabelfishCompass extends SgApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(BabelfishCompass.class);

  private static final String REPORT = "report";
  private static final String LOGS = "logs";

  @Inject
  ApplicationsConfig config;

  @Inject
  KubernetesClient client;

  @Inject
  PodExecutor podExecutor;

  public BabelfishCompass() {
    super("com.ongres", "babelfish-compass");
  }

  @Override
  public boolean isEnabled() {
    return config.babelfishCompass().enabled();
  }

  public Map<String, String> run(@Nonnull String reportName, @Nonnull List<FileUpload> files) {
    final String namespace = Optional.ofNullable(client.getNamespace()).orElse("default");
    final String name = ResourceUtil.nameIsValidDnsSubdomainForJob("bbf-"
        + StringUtil.generateRandom(16).toLowerCase(Locale.ROOT));

    Job compassJob = client.batch().v1().jobs()
        .inNamespace(namespace)
        .resource(new JobBuilder()
            .withNewMetadata()
            .withName(name)
            .endMetadata()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .withName("babelfish-compass")
            .withSecurityContext(new SecurityContextBuilder()
                .withRunAsUser(1000L)
                .withRunAsGroup(1000L)
                .withRunAsNonRoot(Boolean.TRUE)
                .withAllowPrivilegeEscalation(Boolean.FALSE)
                .build())
            .withResources(new ResourceRequirementsBuilder()
                .withLimits(Map.of("memory", new Quantity("1Gi"),
                    "cpu", new Quantity("2")))
                .withRequests(Map.of("memory", new Quantity("128Mi"),
                    "cpu", new Quantity("500m")))
                .build())
            .withImage(StackGresComponent.BABELFISH_COMPASS.getLatest().getLatestImageName())
            .withCommand("/bin/sleep")
            .withArgs("600")
            .withVolumeMounts(new VolumeMountBuilder()
                .withName("sql-files-volume")
                .withMountPath("/sql")
                .build())
            .endContainer()
            .addToVolumes(new VolumeBuilder()
                .withName("sql-files-volume")
                .withNewEmptyDir()
                .endEmptyDir()
                .build())
            .withRestartPolicy("Never")
            .endSpec()
            .endTemplate()
            .endSpec()
            .build())
        .create();
    try {
      Pod pod = waitForPod(namespace, name);

      writeFiles(pod, files);

      podExecutor.setClientFactory(client);
      List<String> outputExec = podExecutor.exec(pod, "babelfish-compass",
          "/bin/sh", "-c", "/app/compass " + reportName
              + " -reportoption xref=all,status=all,detail "
              + files.stream()
                  .map(s -> "/sql/" + s.filename())
                  .collect(Collectors.joining(" ")));

      Map<String, String> extractFromLogs = extractFromLogs(outputExec);
      Map<String, String> readFiles = readFiles(pod, extractFromLogs);

      return readFiles;
    } finally {
      cleanupKubernetesResources(compassJob);
    }
  }

  private Pod waitForPod(String namespace, String name) {
    CountDownLatch latch = new CountDownLatch(10);
    while (client.pods()
        .inNamespace(namespace)
        .withLabel("job-name", name)
        .list().getItems().isEmpty()) {
      latch.countDown();
      try {
        if (latch.await(1, TimeUnit.SECONDS)) {
          throw new WebApplicationException("Could not get Pod from Job");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    Pod pod = client.pods()
        .inNamespace(namespace)
        .withLabel("job-name", name)
        .list().getItems().get(0);
    try {
      pod = client.resource(pod)
          .waitUntilReady(30, TimeUnit.SECONDS);
    } catch (KubernetesClientTimeoutException e) {
      throw new WebApplicationException(e.getMessage());
    }

    return pod;
  }

  private Map<String, String> readFiles(Pod pod, Map<String, String> filesPath) {
    Path logsFile = null;
    Path reportFile = null;
    try {
      logsFile = Files.createTempFile(LOGS, null);
      reportFile = Files.createTempFile(REPORT, null);

      PodResource withName = client.pods()
          .inNamespace(pod.getMetadata().getNamespace())
          .withName(pod.getMetadata().getName());
      withName
          .file(filesPath.get(LOGS))
          .copy(logsFile);
      withName
          .file(filesPath.get(REPORT))
          .copy(reportFile);

      String logsString = Files.readString(logsFile, StandardCharsets.UTF_8);
      String reportString = Files.readString(reportFile, StandardCharsets.UTF_8);

      return Map.of(LOGS, logsString, REPORT, reportString);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      cleanupFiles(logsFile);
      cleanupFiles(reportFile);
    }
  }

  private void writeFiles(Pod pod, List<FileUpload> files) {
    for (FileUpload entry : files) {
      try (InputStream content = entry.content()) {
        client.pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .file("/sql/" + entry.filename())
            .upload(content);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private void cleanupKubernetesResources(HasMetadata... resources) {
    for (HasMetadata resource : resources) {
      try {
        client.resource(resource).delete();
      } catch (KubernetesClientException e) {
        LOGGER.warn("Could not delete the resource: {}", resource.getMetadata().getName());
      }
    }
  }

  private void cleanupFiles(Path file) {
    try {
      if (file != null) {
        Files.deleteIfExists(file);
      }
    } catch (IOException e) {
      LOGGER.warn("Could not delete the file: {}", file);
    }
  }

  private Map<String, String> extractFromLogs(List<String> logs) {
    Map<String, String> files = new HashMap<>();
    for (String line : logs) {
      if (line.startsWith("Session log          :")) {
        files.put(LOGS, line.split(":")[1].strip());
      }
      if (line.startsWith("Assessment report    :")) {
        files.put(REPORT, line.split(":")[1].strip());
      }
    }
    if (files.isEmpty() || files.size() != 2) {
      throw new WebApplicationException("Failed to extract report files");
    }
    return Map.copyOf(files);
  }

}
