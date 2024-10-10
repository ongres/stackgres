/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EphemeralContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.PodOperationsImpl;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPathV2;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.VolumeMountBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.docir.DocirUtil;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.operatorframework.resource.WatcherMonitor;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocirExtensionManager
    implements ExtensionManager {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DocirExtensionManager.class);

  public static final String SHA256_SUFFIX = ".sha256";
  public static final String TGZ_SUFFIX = ".tgz";
  public static final String INSTALLED_SUFFIX = ".installed";
  public static final String LINKS_CREATED_SUFFIX = ".links-created";
  public static final String PENDING_SUFFIX = ".pending";

  private final StackGresContext context;
  private final KubernetesClient client;
  private final String podName;
  private final FileSystemHandler fileSystemHandler;
  private final boolean bootstrap;

  protected DocirExtensionManager(
      StackGresContext context,
      KubernetesClient client,
      String podName,
      FileSystemHandler fileSystemHandler,
      boolean bootstrap) {
    this.context = context;
    this.client = client;
    this.podName = podName;
    this.fileSystemHandler = fileSystemHandler;
    this.bootstrap = bootstrap;
  }

  public DocirExtensionManager() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.context = null;
    this.client = null;
    this.podName = null;
    this.fileSystemHandler = null;
    this.bootstrap = false;
  }

  @Override
  public ExtensionInstaller getExtensionInstaller(
      ClusterContext clusterContext,
      StackGresClusterInstalledExtension installedExtension) throws Exception {
    final String extensionImageName = context.getMetadataManager().getExtensionImage(
        clusterContext.getContext(), clusterContext.getCluster(), installedExtension);
    return new DocirExtensionInstaller(clusterContext, installedExtension, extensionImageName);
  }

  @Override
  public ExtensionUninstaller getExtensionUninstaller(
      ClusterContext clusterContext,
      StackGresClusterInstalledExtension installedExtension) throws Exception {
    return new DocirExtensionUninstaller(clusterContext, installedExtension);
  }

  @Override
  public StackGresClusterInstalledExtension getInstalledExtension(
      StackGresCluster cluster,
      StackGresClusterExtension clusterExtension) {
    var extensionCandidate = context.getMetadataManager().getExtensionCandidateSameMajorBuild(
        context, cluster, clusterExtension, true);
    return DocirUtil.getInstalledExtension(cluster, clusterExtension, extensionCandidate, true);
  }

  public class DocirExtensionInstaller implements ExtensionInstaller, ExtensionPuller {
    private final ClusterContext clusterContext;
    private final StackGresClusterInstalledExtension installedExtension;
    private final String extensionImageName;
    private final String extensionFileName;

    private DocirExtensionInstaller(
        ClusterContext clusterContext,
        StackGresClusterInstalledExtension extension,
        String extensionImageName) {
      this.clusterContext = clusterContext;
      this.installedExtension = extension;
      this.extensionImageName = extensionImageName;
      this.extensionFileName = DocirUtil.getExtensionFileName(clusterContext.getCluster(), extension);
    }

    @Override
    public boolean isExtensionInstalled() throws Exception {
      return fileSystemHandler.exists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + INSTALLED_SUFFIX));
    }

    @Override
    public boolean areLinksCreated() throws Exception {
      return fileSystemHandler.exists(
          Paths.get(ClusterPathV2.PG_RELOCATED_LIB_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + LINKS_CREATED_SUFFIX));
    }

    @Override
    public void downloadAndExtract() throws Exception {
      if (bootstrap) {
        LOGGER.info("Skip downloading {} from image {}",
            DocirUtil.getDescription(clusterContext.getCluster(), installedExtension, true),
            extensionImageName);
        return;
      }
      LOGGER.info("Downloading {} from image {}",
          DocirUtil.getDescription(clusterContext.getCluster(), installedExtension, true),
          extensionImageName);
      // ephemeral containers can not be removed see: https://github.com/kubernetes/kubernetes/issues/84764
      final String ephemeralName = "docir-extension-" + Instant.now().getEpochSecond();
      KubernetesClientUtil.retryOnConflict(() -> {
        Pod pod = client.pods()
            .inNamespace(clusterContext.getCluster().getMetadata().getNamespace())
            .withName(podName)
            .get();
        if (pod == null) {
          throw new IllegalArgumentException("Pod " + podName + " not found");
        }
        Pod podWithExtensionImage = new PodBuilder(pod)
            .editSpec()
            .withEphemeralContainers(Seq.seq(Optional
                .ofNullable(pod.getSpec().getEphemeralContainers()))
                .flatMap(List::stream)
                .append(
                    new EphemeralContainerBuilder()
                    .withName(ephemeralName)
                    .withImage(extensionImageName)
                    .withCommand(
                        ClusterPathV2.PG_RELOCATED_LIB64_PATH.path(clusterContext) + "/ld-linux-x86-64.so.2",
                        ClusterPathV2.PG_RELOCATED_USR_BIN_PATH.path(clusterContext) + "/bash",
                        "-xe",
                        ClusterPathV2.LOCAL_BIN_COPY_EXTENSION_SG_PATH.path())
                    .withEnv(
                        new EnvVarBuilder()
                        .withName("LD_LIBRARY_PATH")
                        .withValue(ClusterPathV2.PG_RELOCATED_LIB64_PATH.path(clusterContext)
                            + ":" + ClusterPathV2.PG_RELOCATED_SYSTEM_LIB_PATH.path(clusterContext))
                        .build(),
                        new EnvVarBuilder()
                        .withName("LD_LINUX_PATH")
                        .withValue(ClusterPathV2.PG_RELOCATED_LIB64_PATH.path(clusterContext) + "/ld-linux-x86-64.so.2")
                        .build(),
                        new EnvVarBuilder()
                        .withName(ClusterPathV2.PG_RELOCATED_LIB64_PATH.name())
                        .withValue(ClusterPathV2.PG_RELOCATED_LIB64_PATH.path(clusterContext))
                        .build(),
                        new EnvVarBuilder()
                        .withName(ClusterPathV2.PG_RELOCATED_USR_BIN_PATH.name())
                        .withValue(ClusterPathV2.PG_RELOCATED_USR_BIN_PATH.path(clusterContext))
                        .build(),
                        new EnvVarBuilder()
                        .withName(ClusterPathV2.PG_EXTENSIONS_PATH.name())
                        .withValue(ClusterPathV2.PG_EXTENSIONS_PATH.path(clusterContext))
                        .build())
                    .withVolumeMounts(
                        new VolumeMountBuilder()
                        .withName(StackGresUtil.statefulSetDataPersistentVolumeClaimName(clusterContext))
                        .withMountPath(ClusterPathV2.PG_BASE_PATH.path(clusterContext))
                        .build(),
                        new VolumeMountBuilder()
                        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
                        .withMountPath(ClusterPathV2.LOCAL_BIN_PATH.path())
                        .build())
                    .build())
                .toList())
            .endSpec()
            .build();
        PodOperationsImpl podOperations = ((PodOperationsImpl) client.pods());
        podOperations.newInstance(
            podOperations.getOperationContext()
            .withSubresource("ephemeralcontainers"))
            .resource(podWithExtensionImage)
            .patch();
      });
      CompletableFuture<Tuple2<Boolean, Optional<String>>> extensionEphemeralContainerCompleted =
          new CompletableFuture<>();
      var watcherMonitor = new WatcherMonitor<Pod>("PodEphemeralContainers",
          watcher -> client
          .pods()
          .inNamespace(clusterContext.getCluster().getMetadata().getNamespace())
          .watch(watcher),
          (action, pod) -> {
            var terminated = Optional.of(pod)
                .map(Pod::getStatus)
                .map(PodStatus::getEphemeralContainerStatuses)
                .stream()
                .flatMap(List::stream)
                .filter(containerStatus -> containerStatus.getName().equals(ephemeralName))
                .flatMap(containerStatus -> Optional.of(containerStatus)
                    .map(ContainerStatus::getState)
                    .map(ContainerState::getTerminated)
                    .filter(value -> value.getExitCode() != null)
                    .stream())
                .findFirst();
            if (terminated.isPresent()) {
              if (terminated.get().getExitCode() == 0) {
                extensionEphemeralContainerCompleted.complete(
                    Tuple.tuple(true, Optional.empty()));
              } else {
                extensionEphemeralContainerCompleted.complete(
                    Tuple.tuple(false, Optional.ofNullable(terminated.get().getMessage())));
              }
            }
          });
      try {
        var result = extensionEphemeralContainerCompleted.get(5, TimeUnit.MINUTES);
        if (!result.v1) {
          throw new RuntimeException("Error while downloading extension "
              + DocirUtil.getDescription(clusterContext.getCluster(), installedExtension, true)
              + ": " + result.v2.orElse("unknown"));
        }
      } finally {
        try {
          watcherMonitor.close();
        } catch (Exception ex) {
          LOGGER.error("Error while closing the watcher monitor", ex);
        }
      }
    }

    @Override
    public void verify() throws Exception {
      // TODO
    }

    @Override
    public boolean doesInstallOverwriteAnySharedFile() throws Exception {
      //TODO
      return false;
    }

    @Override
    public boolean doesInstallOverwriteAnySharedFile(InputStream inputStream) throws Exception {
      //TODO
      return false;
    }

    @Override
    public void installExtension() throws Exception {
      createExtensionLinks();
      fileSystemHandler.createOrReplaceFile(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + INSTALLED_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + PENDING_SUFFIX));
    }

    @Override
    public void createExtensionLinks() throws Exception {
      fileSystemHandler
          .list(Paths.get(ClusterPathV2.PG_EXTENSIONS_LIB_PATH.pathFromEnv(clusterContext)))
          .map(libFile -> Tuple.tuple(libFile,
              Paths.get(ClusterPathV2.PG_RELOCATED_LIB_PATH.pathFromEnv(clusterContext))
                  .resolve(libFile.getFileName())))
          .forEach(
              Unchecked.consumer(t -> {
                Path targetParent = t.v2.getParent();
                if (targetParent != null) {
                  fileSystemHandler.createDirectories(targetParent);
                }
                if (!fileSystemHandler.isDirectory(t.v2)) {
                  fileSystemHandler.createOrReplaceSymbolicLink(t.v2, t.v1);
                }
              }));
      fileSystemHandler.createOrReplaceFile(
          Paths.get(ClusterPathV2.PG_RELOCATED_LIB_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + LINKS_CREATED_SUFFIX));
    }

    @Override
    public boolean isExtensionPendingOverwrite() {
      return fileSystemHandler.exists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + PENDING_SUFFIX));
    }

    @Override
    public void setExtensionAsPending() throws Exception {
      fileSystemHandler.createOrReplaceFile(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + PENDING_SUFFIX));
    }

    @Override
    public ExtensionPuller getPuller() throws Exception {
      return this;
    }

  }

  public class DocirExtensionUninstaller implements ExtensionUninstaller {
    private final ClusterContext clusterContext;
    private final String extensionFileName;

    private DocirExtensionUninstaller(
        ClusterContext clusterContext,
        StackGresClusterInstalledExtension extension) {
      this.clusterContext = clusterContext;
      this.extensionFileName = DocirUtil.getExtensionFileName(clusterContext.getCluster(), extension);
    }

    @Override
    public boolean isExtensionInstalled() throws Exception {
      return fileSystemHandler.exists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + INSTALLED_SUFFIX));
    }

    @Override
    public void uninstallExtension() throws Exception {
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + INSTALLED_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterPathV2.PG_RELOCATED_LIB_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + LINKS_CREATED_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + PENDING_SUFFIX));
      //TODO
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + TGZ_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterPathV2.PG_EXTENSIONS_PATH.pathFromEnv(clusterContext))
          .resolve(extensionFileName + SHA256_SUFFIX));
    }

  }

}
