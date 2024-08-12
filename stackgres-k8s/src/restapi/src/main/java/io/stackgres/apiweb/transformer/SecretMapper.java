/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.apiweb.dto.secret.SecretDto;
import io.stackgres.apiweb.transformer.util.TransformerUtil;

public class SecretMapper {

  public static SecretDto map(Secret secret) {
    SecretDto secretDto = new SecretDto();
    secretDto.setMetadata(TransformerUtil.fromResource(secret.getMetadata()));
    secretDto.setKeys(List.copyOf(secret.getData().keySet()));
    return secretDto;
  }

}
