/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

public interface VolumePath {

  String filename();

  String filename(ClusterContext context);

  String filename(ClusterContext context, Map<String, String> envVars);

  String filename(Map<String, String> envVars);

  String path();

  String path(ClusterContext context);

  String path(ClusterContext context, Map<String, String> envVars);

  String path(Map<String, String> envVars);

  String subPath();

  String subPath(ClusterContext context);

  String subPath(ClusterContext context, Map<String, String> envVars);

  String subPath(Map<String, String> envVars);

  String subPath(VolumePath relativeTo);

  String subPath(ClusterContext context, VolumePath relativeTo);

  String subPath(Map<String, String> envVars, VolumePath relativeTo);

  String subPath(ClusterContext context, Map<String, String> envVars, VolumePath relativeTo);

}
