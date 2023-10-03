/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

public interface VolumePath<T> {

  String filename();

  String filename(T context);

  String filename(T context, Map<String, String> envVars);

  String filename(Map<String, String> envVars);

  String path();

  String path(T context);

  String path(T context, Map<String, String> envVars);

  String path(Map<String, String> envVars);

  String subPath();

  String subPath(T context);

  String subPath(T context, Map<String, String> envVars);

  String subPath(Map<String, String> envVars);

  String subPath(VolumePath<T> relativeTo);

  String subPath(T context, VolumePath<T> relativeTo);

  String subPath(Map<String, String> envVars, VolumePath<T> relativeTo);

  String subPath(T context, Map<String, String> envVars, VolumePath<T> relativeTo);

}
