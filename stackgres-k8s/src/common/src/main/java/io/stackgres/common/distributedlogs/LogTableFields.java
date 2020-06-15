/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.distributedlogs;

public interface LogTableFields {

  String LOG_TIME = "log_time";
  String LOG_TIME_INDEX = "log_time_index";
  String POD_NAME = "pod_name";
  String ROLE = "role";
  String ERROR_SEVERITY = "error_severity";
  String MESSAGE = "message";

}
