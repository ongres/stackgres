/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface PatroniCtlInstance {

  List<PatroniMember> list();

  List<PatroniHistoryEntry> history();

  PatroniConfig showConfig();

  ObjectNode showConfigJson();

  void editConfig(PatroniConfig patroniConfig);

  void editConfigJson(ObjectNode patroniConfig);

  void restart(String username, String password, String member);

  void switchover(String username, String password, String leader, String candidate);

  void remove(String username, String password);

  JsonNode queryPrimary(String query, String username, String password);

}
