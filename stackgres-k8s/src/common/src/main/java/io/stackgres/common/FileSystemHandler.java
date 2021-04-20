/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class FileSystemHandler {

  public boolean exists(Path path) {
    return Files.exists(path);
  }

  public void createOrReplaceFile(Path path) throws IOException {
    Path temporaryPath = getTemporaryPath(path);
    Files.createFile(temporaryPath);
    Files.move(temporaryPath, path,
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  public void createDirectories(Path path) throws IOException {
    Files.createDirectories(path);
  }

  public void copyOrReplace(InputStream inputStream, Path path) throws IOException {
    Path temporaryPath = getTemporaryPath(path);
    Files.copy(inputStream, temporaryPath,
        StandardCopyOption.REPLACE_EXISTING);
    Files.move(temporaryPath, path,
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  public Path setPosixFilePermissions(Path path, Set<PosixFilePermission> perms)
      throws IOException {
    return Files.setPosixFilePermissions(path, perms);
  }

  public void createOrReplaceSymbolicLink(Path path, Path target) throws IOException {
    if (Files.exists(path)
        && Files.isSymbolicLink(path)
        && Files.readSymbolicLink(path).equals(target)) {
      return;
    }
    Path temporaryPath = getTemporaryPath(path);
    Files.createSymbolicLink(temporaryPath, target);
    Files.move(temporaryPath, path,
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private Path getTemporaryPath(Path path) {
    return Optional.ofNullable(path.getParent())
        .orElseGet(() -> Paths.get("."))
        .resolve(Optional.ofNullable(path.getFileName())
            .map(Object::toString).orElse("")
            + Long.toHexString(System.currentTimeMillis())
            + ".tmp");
  }

  public void deleteIfExists(Path path) throws IOException {
    Files.deleteIfExists(path);
  }

  public InputStream newInputStream(Path path) throws IOException {
    return Files.newInputStream(path);
  }

  public Stream<Path> list(Path path) throws IOException {
    return Files.list(path);
  }

  public long size(Path targetPath) throws IOException {
    return Files.size(targetPath);
  }

  public boolean isSymbolicLink(Path targetPath) {
    return Files.isSymbolicLink(targetPath);
  }

}
