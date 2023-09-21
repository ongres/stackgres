/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.stackgres.apiweb.dto.configmap.ConfigMapDto;
import io.stackgres.apiweb.transformer.util.TransformerUtil;

public class ConfigMapMapper {

  public static ConfigMapDto map(ConfigMap configMap) {
    ConfigMapDto configMapDto = new ConfigMapDto();
    configMapDto.setMetadata(TransformerUtil.fromResource(configMap.getMetadata()));
    if (configMap.getData() != null) {
      configMapDto.setData(Map.copyOf(configMap.getData()));
    }
    return configMapDto;
  }

}
