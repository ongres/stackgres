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
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class FileSystemHandler {

  public boolean exists(Path path) {
    return Files.exists(path);
  }

  public boolean identical(Path path, InputStream inputStream) throws IOException {
    try (InputStream pathInputStream = Files.newInputStream(path)) {
      return compareInputStreams(inputStream, pathInputStream);
    }
  }

  public boolean identicalLink(Path path, Path target) throws IOException {
    final Path resolvedTarget;
    if (target.isAbsolute()) {
      resolvedTarget = target;
    } else {
      resolvedTarget = Optional.of(path.getParent()).orElseThrow().resolve(target);
    }
    return Files.isSymbolicLink(path) && Files.readSymbolicLink(path).equals(resolvedTarget);
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

  public void copyOrReplace(Path path, Path target) throws IOException {
    Path temporaryPath = getTemporaryPath(path);
    Files.copy(path, temporaryPath,
        StandardCopyOption.REPLACE_EXISTING);
    Files.move(temporaryPath, target,
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  public void copyOrReplace(InputStream inputStream, Path target) throws IOException {
    Path temporaryPath = getTemporaryPath(target);
    Files.copy(inputStream, temporaryPath,
        StandardCopyOption.REPLACE_EXISTING);
    Files.move(temporaryPath, target,
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

  public long size(Path path) throws IOException {
    return Files.size(path);
  }

  public boolean isSymbolicLink(Path path) {
    return Files.isSymbolicLink(path);
  }

  public boolean compareInputStreams(InputStream inputStream1, InputStream inputStream2)
      throws IOException {
    final byte[] inputStreamBuffer1 = new byte[8192];
    final byte[] inputStreamBuffer2 = new byte[8192];
    int offset1 = 0;
    int offset2 = 0;
    int inputStreamReaded1 = 0;
    int inputStreamReaded2 = 0;
    while (true) {
      if (offset1 == 0) {
        inputStreamReaded1 = inputStream1.read(inputStreamBuffer1);
      }
      if (offset2 == 0) {
        inputStreamReaded2 = inputStream2.read(inputStreamBuffer2);
      }
      if (inputStreamReaded1 == -1 || inputStreamReaded2 == -1) {
        return inputStreamReaded1 == inputStreamReaded2;
      }
      final int minLength = Math.min(
          inputStreamReaded1 - offset1,
          inputStreamReaded2 - offset2);
      if (!Arrays.equals(
          inputStreamBuffer1, offset1, minLength + offset1,
          inputStreamBuffer2, offset2, minLength + offset2)) {
        return false;
      }
      if (inputStreamReaded1 - offset1 > minLength) {
        offset1 += minLength;
      } else {
        offset1 = 0;
      }
      if (inputStreamReaded2 - offset2 > minLength) {
        offset2 += minLength;
      } else {
        offset2 = 0;
      }
    }
  }

}
