/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;

public abstract class AbstractLabelFactory<T extends CustomResource<?, ?>>
    implements LabelFactory<T> {

  @Override
  public Map<String, String> appLabel() {
    return Map.of(labelMapper().appKey(), labelMapper().appName());
  }

  @Override
  public Map<String, String> genericLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)));
  }

}
