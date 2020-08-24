/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.apiweb.dto.secret.SecretDto;

public class SecretMapper {

  public static SecretDto map(Secret secret) {
    SecretDto secretDto = new SecretDto();
    secretDto.setMetadata(MetadataMapper.map(secret.getMetadata()));
    secretDto.setKeys(ImmutableList.copyOf(secret.getData().keySet()));
    return secretDto;
  }

}
