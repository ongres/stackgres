/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;

public abstract class AbstractLabelFactory<T extends CustomResource<?, ?>>
    implements LabelFactory<T> {

  @Override
  public Map<String, String> genericLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)));
  }

}
