/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Optional;

import io.stackgres.common.patroni.PatroniMember.MemberRole;
import io.stackgres.common.patroni.PatroniMember.MemberState;
import org.immutables.value.Value;

@Value.Immutable
public interface PatroniInformation {

  Optional<MemberState> getState();

  Optional<MemberRole> getRole();

  boolean isPendingRestart();

}
