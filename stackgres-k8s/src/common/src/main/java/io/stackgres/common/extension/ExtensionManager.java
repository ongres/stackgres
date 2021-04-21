/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SignatureException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.SignatureUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.WebClientFactory.WebClient;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExtensionManager {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExtensionManager.class);

  private static final String SKIP_HOSTNAME_VERIFICATION_PARAMETER = "skipHostnameVerification";

  public static final String SHA256_SUFFIX = ".sha256";
  public static final String TGZ_SUFFIX = ".tgz";
  public static final String INSTALLED_SUFFIX = ".installed";
  public static final String LINKS_CREATED_SUFFIX = ".links-created";
  public static final String PENDING_SUFFIX = ".pending";

  private final ExtensionMetadataManager extensionMetadataManager;
  private final WebClientFactory webClientFactory;
  private final FileSystemHandler fileSystemHandler;

  public ExtensionManager(ExtensionMetadataManager extensionMetadataManager,
      WebClientFactory webClientFactory,
      FileSystemHandler fileSystemHandler) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.webClientFactory = webClientFactory;
    this.fileSystemHandler = fileSystemHandler;
  }

  public boolean areCompatibles(ClusterContext context, StackGresClusterExtension extension,
      StackGresClusterInstalledExtension installedExtension) {
    return Objects.equals(extension.getName(), installedExtension.getName())
        && Objects.equals(extension.getPublisherOrDefault(), installedExtension.getPublisher())
        && Objects.equals(extension.getRepository(), installedExtension.getRepository())
        && Objects.equals(Optional.ofNullable(extension.getVersion())
            .orElseGet(Unchecked.supplier(() -> extensionMetadataManager
                .getExtensionCandidateSameMajorBuild(context.getCluster(), extension)
                .getVersion().getVersion())), installedExtension.getVersion())
        && Objects.equals(StackGresComponent.POSTGRESQL.findMajorVersion(
            context.getCluster().getSpec().getPostgresVersion()),
            installedExtension.getPostgresVersion())
        && Objects.equals(StackGresComponent.POSTGRESQL.findBuildMajorVersion(
            context.getCluster().getSpec().getPostgresVersion()),
            ExtensionUtil.getMajorBuildOrNull(installedExtension.getBuild()));
  }

  public ExtensionInstaller getExtensionInstaller(ClusterContext context,
      StackGresClusterExtension extension) throws Exception {
    StackGresExtensionMetadata extensionMetadata = extensionMetadataManager
        .getExtensionCandidateSameMajorBuild(context.getCluster(), extension);
    final URI extensionsRepositoryUri = ExtensionUtil
        .getExtensionRepositoryUri(extension, extensionMetadata)
        .orElseThrow(() -> new RuntimeException("URI not found for extension "
            + ExtensionUtil.getDescription(context.getCluster(), extension)));
    return new ExtensionInstaller(context, extension, extensionMetadata,
        extensionsRepositoryUri);
  }

  public ExtensionInstaller getExtensionInstaller(ClusterContext context,
      StackGresClusterInstalledExtension installedExtension) throws Exception {
    StackGresExtensionMetadata extensionMetadata = extensionMetadataManager
        .getExtensionCandidate(installedExtension);
    final URI extensionsRepositoryUri = URI.create(installedExtension.getRepository());
    return new ExtensionInstaller(context, installedExtension, extensionMetadata,
        extensionsRepositoryUri);
  }

  public ExtensionUninstaller getExtensionUninstaller(ClusterContext context,
      StackGresClusterInstalledExtension installedExtension) throws Exception {
    return new ExtensionUninstaller(context, installedExtension);
  }

  public class ExtensionInstaller {
    private final ClusterContext context;
    private final StackGresClusterInstalledExtension installedExtension;
    private final StackGresExtensionMetadata extensionMetadata;
    private final URI extensionsRepositoryUri;
    private final URI extensionUri;

    private ExtensionInstaller(ClusterContext context,
        StackGresClusterExtension extension,
        StackGresExtensionMetadata extensionMetadata,
        URI extensionsRepositoryUri) {
      this.context = context;
      this.installedExtension = ExtensionUtil.getInstalledExtension(extension, extensionMetadata);
      this.extensionMetadata = extensionMetadata;
      this.extensionsRepositoryUri = extensionsRepositoryUri;
      this.extensionUri = ExtensionUtil.getExtensionPackageUri(
          extensionsRepositoryUri, extension, extensionMetadata);
    }

    private ExtensionInstaller(ClusterContext context,
        StackGresClusterInstalledExtension installedExtension,
        StackGresExtensionMetadata extensionMetadata,
        URI extensionsRepositoryUri) {
      this.context = context;
      this.installedExtension = installedExtension;
      this.extensionMetadata = extensionMetadata;
      this.extensionsRepositoryUri = extensionsRepositoryUri;
      this.extensionUri = ExtensionUtil.getExtensionPackageUri(
          extensionsRepositoryUri, installedExtension, extensionMetadata);
    }

    public StackGresClusterInstalledExtension getInstalledExtension() {
      return installedExtension;
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "False positive")
    public boolean isExtensionInstalled() throws Exception {
      return fileSystemHandler.exists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + INSTALLED_SUFFIX));
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "False positive")
    public boolean isLinksCreated() throws Exception {
      return fileSystemHandler.exists(
          Paths.get(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + LINKS_CREATED_SUFFIX));
    }

    @SuppressFBWarnings(value = { "UPM_UNCALLED_PRIVATE_METHOD",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" },
        justification = "False positive")
    public void downloadAndExtract() throws Exception {
      boolean skipHostnameVerification =
          getUriQueryParameter(extensionsRepositoryUri, SKIP_HOSTNAME_VERIFICATION_PARAMETER)
          .map(Boolean::valueOf).orElse(false);
      LOGGER.info("Downloading {} from {}",
          ExtensionUtil.getDescription(extensionMetadata), extensionUri);
      try (WebClient client = webClientFactory.create(skipHostnameVerification)) {
        try (InputStream inputStream = client.getInputStream(extensionUri)) {
          extractTar(inputStream);
        }
      }
    }

    @SuppressFBWarnings(value = { "UPM_UNCALLED_PRIVATE_METHOD",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" },
        justification = "False positive")
    public void verify() throws Exception {
      try (InputStream signatureInputStream = fileSystemHandler.newInputStream(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + SHA256_SUFFIX));
          InputStream extensionPackageInputStream = fileSystemHandler.newInputStream(
              Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
              .resolve(extensionMetadata.getPackageName() + TGZ_SUFFIX))) {
        if (!SignatureUtil.verify(extensionMetadata.getPublisher().getPublicKey(),
            signatureInputStream, extensionPackageInputStream)) {
          throw new SignatureException("Signature verification failed");
        }
      }
    }

    @SuppressFBWarnings(value = { "UPM_UNCALLED_PRIVATE_METHOD",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" },
        justification = "False positive")
    public boolean doesInstallOverwriteAnySharedLibrary() throws Exception {
      try (
          InputStream extensionPackageInputStream = fileSystemHandler.newInputStream(
              Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
              .resolve(extensionMetadata.getPackageName() + TGZ_SUFFIX));
          InputStream extensionPackageInputStreamUncompressed = new GZIPInputStream(
              extensionPackageInputStream)) {
        return doesInstallOverwriteAnySharedLibrary(extensionPackageInputStreamUncompressed);
      }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "False positive")
    public boolean doesInstallOverwriteAnySharedLibrary(InputStream inputStream) throws Exception {
      return visitTar(Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context)),
          inputStream,
          ExtensionManager.this::isSharedLibraryOverwrite,
          false, (prev, next) -> prev || next);
    }

    @SuppressFBWarnings(value = { "UPM_UNCALLED_PRIVATE_METHOD",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" },
        justification = "False positive")
    public void installExtension() throws Exception {
      try (
          InputStream extensionPackageInputStream = fileSystemHandler.newInputStream(
              Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
              .resolve(extensionMetadata.getPackageName() + TGZ_SUFFIX));
          InputStream extensionPackageInputStreamUncompressed = new GZIPInputStream(
              extensionPackageInputStream)) {
        extractTar(extensionPackageInputStreamUncompressed);
      }
      createExtensionLinks();
      fileSystemHandler.createOrReplaceFile(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + INSTALLED_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + PENDING_SUFFIX));
    }

    public void createExtensionLinks() throws Exception {
      fileSystemHandler
          .list(Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_LIB_PATH.path(context)))
          .map(libFile -> Tuple.tuple(libFile,
              Paths.get(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.path(context))
                  .resolve(libFile.getFileName())))
          .forEach(
              Unchecked.consumer(t -> fileSystemHandler
                  .createOrReplaceSymbolicLink(t.v2, t.v1)));
      fileSystemHandler.createOrReplaceFile(
          Paths.get(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + LINKS_CREATED_SUFFIX));
    }

    private void extractTar(InputStream inputStream)
        throws Exception {
      visitTar(Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context)),
          inputStream,
          ExtensionManager.this::extractFile,
          null, (prev, next) -> null);
    }

    public boolean isExtensionPendingOverwrite() {
      return fileSystemHandler.exists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + PENDING_SUFFIX));
    }

    public void setExtensionAsPending() throws Exception {
      fileSystemHandler.createOrReplaceFile(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(extensionMetadata.getPackageName() + PENDING_SUFFIX));
    }
  }

  public class ExtensionUninstaller {
    private final ClusterContext context;
    private final String packageName;

    private ExtensionUninstaller(ClusterContext context,
        StackGresClusterInstalledExtension extension) {
      this.context = context;
      this.packageName = ExtensionUtil.getExtensionPackageName(extension);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "False positive")
    public boolean isExtensionInstalled() throws Exception {
      return fileSystemHandler.exists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(packageName + INSTALLED_SUFFIX));
    }

    @SuppressFBWarnings(value = { "UPM_UNCALLED_PRIVATE_METHOD",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" },
        justification = "False positive")
    public void uninstallExtension() throws Exception {
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(packageName + INSTALLED_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.path(context))
          .resolve(packageName + LINKS_CREATED_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(packageName + PENDING_SUFFIX));
      try (
          InputStream extensionPackageInputStream = fileSystemHandler
              .newInputStream(
                  Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
                  .resolve(packageName + TGZ_SUFFIX));
          InputStream extensionPackageInputStreamUncompressed = new GZIPInputStream(
              extensionPackageInputStream)) {
        removeTarFiles(extensionPackageInputStreamUncompressed);
      }
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(packageName + TGZ_SUFFIX));
      fileSystemHandler.deleteIfExists(
          Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context))
          .resolve(packageName + SHA256_SUFFIX));
    }

    private void removeTarFiles(InputStream inputStream) throws Exception {
      visitTar(Paths.get(ClusterStatefulSetPath.PG_EXTENSIONS_PATH.path(context)),
          inputStream,
          ExtensionManager.this::removeFileIfExists,
          null, (prev, next) -> null);
    }
  }

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "False positive")
  private Optional<String> getUriQueryParameter(URI uri, String parameter) {
    return Optional.ofNullable(uri.getQuery())
        .stream()
        .flatMap(query -> Stream.of(query.split("&")))
        .map(paramAndValue -> paramAndValue.split("="))
        .filter(paramAndValue -> paramAndValue.length == 2)
        .map(paramAndValue -> Tuple.tuple(paramAndValue[0], paramAndValue[1]))
        .map(t -> t.map1(v -> URLDecoder.decode(v, StandardCharsets.UTF_8)))
        .map(t -> t.map2(v -> URLDecoder.decode(v, StandardCharsets.UTF_8)))
        .filter(t -> t.v1.equals(parameter))
        .map(Tuple2::v2)
        .findAny();
  }

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "False positive")
  private <T> T visitTar(Path extensionsPath, InputStream inputStream,
      BiFunction<TarArchiveInputStream, Path, T> visitor,
      T initialValue, BiFunction<T, T, T> accumulator) throws Exception {
    try (TarArchiveInputStream tarEntryInputStream = new TarArchiveInputStream(inputStream)) {
      TarArchiveEntry tarArchiveEntry = tarEntryInputStream.getNextTarEntry();
      if (tarArchiveEntry == null) {
        throw new IllegalStateException("Can not find any entry in the output");
      }
      for (; tarArchiveEntry != null; tarArchiveEntry = tarEntryInputStream.getNextTarEntry()) {
        final Path entryPath = Paths.get(tarArchiveEntry.getName());
        final Path targetPath;
        if (entryPath.isAbsolute()) {
          targetPath = entryPath;
        } else {
          targetPath = extensionsPath.resolve(entryPath);
        }
        initialValue = accumulator.apply(initialValue,
            visitor.apply(tarEntryInputStream, targetPath));
      }
    } catch (UncheckedIOException ex) {
      throw ex.getCause();
    }
    return initialValue;
  }

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "False positive")
  private boolean isSharedLibraryOverwrite(TarArchiveInputStream tarEntryInputStream,
      Path targetPath) throws UncheckedIOException {
    if (isScriptOrControlFile(targetPath)) {
      return false;
    }
    final TarArchiveEntry tarEntry = tarEntryInputStream.getCurrentEntry();
    return tarEntry.isFile() && fileSystemHandler.exists(targetPath);
  }

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "False positive")
  private Void extractFile(TarArchiveInputStream tarEntryInputStream, Path targetPath)
      throws UncheckedIOException {
    try {
      final TarArchiveEntry tarEntry = tarEntryInputStream.getCurrentEntry();

      if (tarEntry.isFile()) {
        Path targetParent = targetPath.getParent();
        if (targetParent != null) {
          fileSystemHandler.createDirectories(targetParent);
        }
      }
      if (tarEntry.isFile() && !tarEntry.isSymbolicLink()) {
        fileSystemHandler.copyOrReplace(tarEntryInputStream, targetPath);
        int fileMode = tarEntry.getMode();
        Set<PosixFilePermission> permissions = parseMode(fileMode);
        fileSystemHandler.setPosixFilePermissions(targetPath, permissions);
      } else if (tarEntry.isSymbolicLink()) {
        Path linkTarget = Paths.get(tarEntry.getLinkName());
        if (linkTarget.isAbsolute()) {
          fileSystemHandler.createOrReplaceSymbolicLink(targetPath, linkTarget);
        } else {
          Path targetParent = targetPath.getParent();
          if (targetParent == null) {
            fileSystemHandler.createOrReplaceSymbolicLink(
                targetPath, Paths.get(".").resolve(linkTarget));
          } else {
            fileSystemHandler.createOrReplaceSymbolicLink(
                targetPath, targetParent.resolve(linkTarget));
          }
        }
      } else if (tarEntry.isDirectory()) {
        fileSystemHandler.createDirectories(targetPath);
        int fileMode = tarEntry.getMode();
        Set<PosixFilePermission> permissions = parseMode(fileMode);
        fileSystemHandler.setPosixFilePermissions(targetPath, permissions);
      } else {
        LOGGER.warn("Can not extract file {}", targetPath);
        return null;
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Error while extracting " + targetPath, ex);
    }
    return null;
  }

  private Set<PosixFilePermission> parseMode(int mode) {
    Set<PosixFilePermission> permissions = new HashSet<>();
    if ((mode & 0001) != 0) { // NOPMD
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((mode & 0002) != 0) { // NOPMD
      permissions.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & 0004) != 0) { // NOPMD
      permissions.add(PosixFilePermission.OTHERS_READ);
    }
    if ((mode & 0010) != 0) { // NOPMD
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & 0020) != 0) { // NOPMD
      permissions.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & 0040) != 0) { // NOPMD
      permissions.add(PosixFilePermission.GROUP_READ);
    }
    if ((mode & 0100) != 0) { // NOPMD
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((mode & 0200) != 0) { // NOPMD
      permissions.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & 0400) != 0) { // NOPMD
      permissions.add(PosixFilePermission.OWNER_READ);
    }
    return permissions;
  }

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "False positive")
  private Void removeFileIfExists(TarArchiveInputStream tarEntryInputStream, Path targetPath)
      throws UncheckedIOException {
    if (!tarEntryInputStream.getCurrentEntry().isFile()) {
      return null;
    }
    if (!isScriptOrControlFile(targetPath)) {
      LOGGER.warn("Not removing {}", targetPath);
      return null;
    }
    try {
      fileSystemHandler.deleteIfExists(targetPath);
    } catch (IOException ex) {
      throw new UncheckedIOException("Error while removing " + targetPath, ex);
    }
    return null;
  }

  private boolean isScriptOrControlFile(Path targetPath) {
    return targetPath.toString().endsWith(".sql")
        || targetPath.toString().endsWith(".control");
  }

}
