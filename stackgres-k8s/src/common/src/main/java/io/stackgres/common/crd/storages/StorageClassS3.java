/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

/**
 * Allowed values used for WALG_S3_STORAGE_CLASS. By default, WAL-G uses the "STANDARD" storage
 * class. Other supported values include "STANDARD_IA" for Infrequent Access and
 * "REDUCED_REDUNDANCY" for Reduced Redundancy.
 *
 */
public enum StorageClassS3 {

  /**
   * Default "STANDARD" storage class.
   */
  STANDARD,
  /**
   * Standard Infrequent Access storage class.
   */
  STANDARD_IA,
  /**
   * Reduced Redundancy storage class.
   */
  REDUCED_REDUNDANCY

}
