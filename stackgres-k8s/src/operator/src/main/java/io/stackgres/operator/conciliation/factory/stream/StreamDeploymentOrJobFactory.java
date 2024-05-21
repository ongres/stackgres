/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import io.stackgres.common.StackGresProperty;

public interface StreamDeploymentOrJobFactory {

  String IMAGE_TEMPLATE = "%s/stackgres/stream:%s";

  default String getImageName() {
    return String.format(IMAGE_TEMPLATE,
        getContainerRegistry(),
        StackGresProperty.OPERATOR_IMAGE_VERSION.getString());
  }

  default String getContainerRegistry() {
    return StackGresProperty.SG_CONTAINER_REGISTRY.getString();
  }

}
