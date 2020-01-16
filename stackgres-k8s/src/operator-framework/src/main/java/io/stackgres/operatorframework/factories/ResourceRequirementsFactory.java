/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.factories;

import io.fabric8.kubernetes.api.model.ResourceRequirements;

public interface ResourceRequirementsFactory<T> {

  ResourceRequirements getPodRequirements(T config);
}
