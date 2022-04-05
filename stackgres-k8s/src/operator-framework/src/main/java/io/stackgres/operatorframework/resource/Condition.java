/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

public interface Condition {

  String getLastTransitionTime();

  void setLastTransitionTime(String lastTransitionTime);

  String getMessage();

  void setMessage(String message);

  String getReason();

  void setReason(String reason);

  String getStatus();

  void setStatus(String status);

  String getType();

  void setType(String type);

  static void setTransitionTimes(List<? extends Condition> conditions) {
    String currentDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    conditions.forEach(condition -> condition.setLastTransitionTime(currentDateTime));
  }

}
