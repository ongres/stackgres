/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

class JdbcStatementTemplateTest {

  @Test
  void templateWithoutParameter_shouldRenderAsIs() {
    JdbcStatementTemplate template = new JdbcStatementTemplate("SELECT 1");

    assertEquals("SELECT 1", template.getStatement());
    assertThrows(IllegalArgumentException.class, () -> template.getIndexes("test"));
    assertThrows(IllegalArgumentException.class, () -> template.getStatement(ImmutableMap.of("test", "value")));
  }

  @Test
  void templateWithParameter_shouldRenderWithPlaceholders() {
    JdbcStatementTemplate template = new JdbcStatementTemplate("SELECT ${test}");

    assertEquals("SELECT ?", template.getStatement());
    assertIterableEquals(ImmutableList.of(1), template.getIndexes("test"));
    assertThrows(IllegalArgumentException.class, () -> template.getStatement(ImmutableMap.of("test", "value")));
  }

  @Test
  void templateWithStaticParameter_shouldRenderWithParameterValue() {
    JdbcStatementTemplate template = new JdbcStatementTemplate("SELECT @{test}");

    assertEquals("SELECT value", template.getStatement(ImmutableMap.of("test", "value")));
    assertThrows(IllegalArgumentException.class, () -> template.getIndexes("test"));
  }

  @Test
  void templateWithSpecialCharacter_shouldRenderWithCharacterUnescaped() {
    JdbcStatementTemplate template = new JdbcStatementTemplate("SELECT \\${test}");

    assertEquals("SELECT ${test}", template.getStatement());
  }

  @Test
  void templateWithSpecialCharactersAndParameter_shouldRenderWithCharactersUnescapedAndPlaceholders() {
    JdbcStatementTemplate template = new JdbcStatementTemplate("SELECT \\${test}, \\, \\\\, ${test}");

    assertEquals("SELECT ${test}, \\, \\, ?", template.getStatement());
    assertIterableEquals(ImmutableList.of(1), template.getIndexes("test"));
    assertThrows(IllegalArgumentException.class, () -> template.getStatement(ImmutableMap.of("test", "value")));
  }

  @Test
  void templateWithParameters_shouldRenderWithPlaceholders() {
    JdbcStatementTemplate template = new JdbcStatementTemplate("SELECT ${test1}, ${test2}");

    assertEquals("SELECT ?, ?", template.getStatement());
    assertIterableEquals(ImmutableList.of(1), template.getIndexes("test1"));
    assertIterableEquals(ImmutableList.of(2), template.getIndexes("test2"));
    assertThrows(IllegalArgumentException.class, () -> template.getStatement(ImmutableMap.of("test1", "value")));
    assertThrows(IllegalArgumentException.class, () -> template.getStatement(ImmutableMap.of("test2", "value")));
  }

  @Test
  void templateWithRepeatedParameter_shouldRenderWithPlaceholders() {
    JdbcStatementTemplate template = new JdbcStatementTemplate("SELECT ${test}, ${test}");

    assertEquals("SELECT ?, ?", template.getStatement());
    assertIterableEquals(ImmutableList.of(1, 2), template.getIndexes("test"));
    assertThrows(IllegalArgumentException.class, () -> template.getStatement(ImmutableMap.of("test", "value")));
  }

}