/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.util.List;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import org.jetbrains.annotations.NotNull;

public interface CrdLoader {

  CustomResourceDefinition load(@NotNull String kind);

  List<CustomResourceDefinition> scanDefinitions();

}
