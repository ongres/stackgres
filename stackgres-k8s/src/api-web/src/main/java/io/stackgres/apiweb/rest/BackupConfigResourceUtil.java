/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Optional;
import java.util.function.Consumer;

import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigSpec;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.crd.SecretKeySelector;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

public class BackupConfigResourceUtil {

  public static final String S3COMPATIBLE_ACCESS_KEY = BackupStorageDtoUtil.S3COMPATIBLE_ACCESS_KEY;
  public static final String S3COMPATIBLE_SECRET_KEY = BackupStorageDtoUtil.S3COMPATIBLE_SECRET_KEY;

  BackupConfigResourceUtil() {
  }

  String secretName(BackupConfigDto resource) {
    return BackupStorageDtoUtil.secretName(resource);
  }

  Seq<Tuple2<String, Tuple4<String, Consumer<String>,
      SecretKeySelector, Consumer<SecretKeySelector>>>> extractSecretInfo(
      BackupConfigDto resource
  ) {
    Optional<BackupStorageDto> storage = Optional.of(resource)
        .map(BackupConfigDto::getSpec)
        .map(BackupConfigSpec::getStorage);

    if (storage.isPresent()) {
      return BackupStorageDtoUtil.extractSecretInfo(storage.get());
    } else {
      return Seq.of();
    }
  }

}
