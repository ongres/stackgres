/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;

import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.conciliation.AnnotationFinder;

public interface SidecarFinder extends AnnotationFinder {

  StackGresClusterSidecarResourceFactory<?> getSidecarTransformer(String name);

  List<String> getAllOptionalSidecarNames();

  List<String> getAllSidecars();

}
