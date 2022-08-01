/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.fixture.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PathsAsConstantsProcessorTest {

  @Test
  void validProject_shouldGenerateTheInterface() throws IOException {
    final String testSource =
        "@io.stackgres.fixture.processor.PathsAsConstants(\"src/test/resources\") enum Test {}";
    Compilation compilation =
        javac()
        .withProcessors(new PathsAsConstantsProcessor())
        .compile(JavaFileObjects.forSourceString("Test", testSource));
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation).generatedSourceFile("TestWithPaths");
    try (InputStream in = compilation.generatedSourceFiles().get(0).openInputStream()) {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
      Assertions.assertEquals("""
          import java.lang.String;

          public interface TestWithPaths {
            String SUBFOLDER_TEST_FILE = "subfolder/test_file";

            String TEST = "test";
          }""",
          bufferedReader.lines().collect(Collectors.joining("\n")));
    }
  }

}
