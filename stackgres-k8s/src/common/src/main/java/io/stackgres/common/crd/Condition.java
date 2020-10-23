/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

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

}
