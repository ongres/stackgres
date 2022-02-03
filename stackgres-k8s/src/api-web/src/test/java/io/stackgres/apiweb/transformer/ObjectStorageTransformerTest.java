/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageStatus;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.Test;

public class ObjectStorageTransformerTest {

  ObjectStorageTransformer transformer = new ObjectStorageTransformer(
      new BackupStorageTransformer(),
      new JsonMapper()
  );

  public static TransformerTuple<ObjectStorageDto, StackGresObjectStorage> createObjectStorage() {
    var metadataTuple = TransformerTestUtil.createMetadataTuple();
    StackGresObjectStorage crd = new StackGresObjectStorage();
    ObjectStorageDto dto = new ObjectStorageDto();

    crd.setMetadata(metadataTuple.getSource());
    dto.setMetadata(metadataTuple.getTarget());

    var specTuple = BackupStorageTransformerTest.createS3BackupStorage();

    crd.setSpec(specTuple.getSource());
    dto.setSpec(specTuple.getTarget());

    dto.setStatus(new ObjectStorageStatus());
    dto.getStatus().setClusters(List.of(StringUtils.getRandomClusterName()));
    return new TransformerTuple<>(dto, crd);
  }

  @Test
  void testObjectStorageTransformation() {

    var s3ObjectStorageTuple = createObjectStorage();

    final List<String> clusters = Optional.of(s3ObjectStorageTuple.getTarget())
        .map(ObjectStorageDto::getStatus)
        .map(ObjectStorageStatus::getClusters).orElse(List.of());

    TransformerTestUtil.assertTransformation(
        transformer,
        s3ObjectStorageTuple,
        clusters);

  }

}
