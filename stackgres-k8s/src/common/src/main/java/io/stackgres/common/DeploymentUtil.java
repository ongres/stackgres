/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;

public interface DeploymentUtil {

  static Optional<Map<String, String>> getDeploymentPodsMatchLabels(Deployment deployment) {
    return Optional.ofNullable(deployment)
        .map(Deployment::getSpec)
        .map(DeploymentSpec::getSelector)
        .map(LabelSelector::getMatchLabels);
  }

}
