/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.config;

import java.util.Optional;

public interface ConfigContext {

  String PROMETHEUS_AUTOBIND = "PROMETHEUS_AUTOBIND";

  String OPERATOR_NAME = "OPERATOR_NAME";

  String OPERATOR_NAMESPACE = "OPERATOR_NAMESPACE";

  String OPERATOR_VERSION = "OPERATOR_VERSION";

  String CRD_GROUP = "CRD_GROUP";

  String CRD_VERSION = "CRD_VERSION";

  String CONTAINER_BUILD = "CONTAINER_BUILD";

  Optional<String> getProp(String prop);
}
