/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class KubernetesClientObjectMapperCustomizerImpl implements KubernetesClientObjectMapperCustomizer {

  @Override
  public void customize(ObjectMapper objectMapper) {
    objectMapper.setSerializationInclusion(Include.NON_EMPTY);
  }

}
