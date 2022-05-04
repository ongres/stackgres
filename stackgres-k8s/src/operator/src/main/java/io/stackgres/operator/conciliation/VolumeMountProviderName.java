/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

public enum VolumeMountProviderName {

  POSTGRES_SOCKET,
  POSTGRES_DATA,
  CONTAINER_USER_OVERRIDE,
  MAJOR_VERSION_UPGRADE,
  POSTGRES_EXTENSIONS,
  CONTAINER_LOCAL_OVERRIDE,
  SCRIPT_TEMPLATES,
  LOCAL_BIN,
  BACKUP,
  RESTORE,
  POSTGRES_LOG,
  HUGE_PAGES

}
