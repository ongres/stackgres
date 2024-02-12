/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.controller;

/**
 * Serves as a place-holder for the most elemental configuration values of a pod local controller.
 */
public interface PodLocalControllerContext {

  /**
   * Returns the StackGres Cluster name or similar objects that it's the owner of the pod local
   * controller.
   *
   * @return the cluster name that owns this pod local controller
   */
  String getClusterName();

  /**
   * Returns the namespace in which is installed pod local controller.
   *
   * @return the namespace
   */
  String getNamespace();

  /**
   * Returns the Pod's name of the pod local controller.
   * @return the pod's name
   */
  String getPodName();

}
