/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.fixture.processor;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.jooq.lambda.Unchecked;

@AutoService(Processor.class)
public class PathsAsConstantsProcessor extends AbstractProcessor {

  private static final SecureRandom RANDOM = new SecureRandom();

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(PathsAsConstants.class.getCanonicalName());
  }

  private String generateRandomTmpFileName() {
    long n = RANDOM.nextLong();
    return "tmp" + Long.toUnsignedString(n);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    annotations.forEach(annotaion -> processAnnotation(annotaion, roundEnv));
    return true;
  }

  private void processAnnotation(TypeElement annotation, RoundEnvironment roundEnv) {
    roundEnv.getElementsAnnotatedWith(annotation)
        .forEach(this::processAnnotatedElement);
  }

  private void processAnnotatedElement(Element element) {
    try {
      createSource(element);
    } catch (Exception ex) {
      processingEnv.getMessager().printMessage(Kind.ERROR, ex.getMessage());
      printError(ex.getClass().getCanonicalName() + ": " + ex.getMessage() + "\n\n"
          + Arrays.asList(ex.getStackTrace()).stream()
          .map(StackTraceElement::toString)
          .collect(Collectors.joining("\n")));
    }
  }

  private void createSource(Element clazz) throws IOException {
    final String fullName = ((TypeElement) clazz).getQualifiedName() + "WithPaths";
    final String packageName = Optional.of(fullName.lastIndexOf('.'))
        .filter(index -> index >= 0)
        .map(index -> fullName.substring(0, index))
        .orElse("");
    final String name = Optional.of(fullName.lastIndexOf('.'))
        .filter(index -> index >= 0)
        .map(index -> fullName.substring(index + 1))
        .orElse(fullName);
    JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(fullName);

    TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(name)
        .addModifiers(Modifier.PUBLIC);

    PathsAsConstants pathsAsEnum = clazz.getAnnotation(PathsAsConstants.class);

    Path projectPath = Paths.get("");
    Filer filer = processingEnv.getFiler();
    FileObject resource = filer.createResource(
        StandardLocation.CLASS_OUTPUT, "", generateRandomTmpFileName(), (Element[]) null);
    try {
      URI resourceUri = resource.toUri();
      if (resourceUri.getScheme().equals("file")) {
        projectPath = Optional.of(Paths.get(resourceUri))
            .map(Path::getParent)
            .map(path -> path.resolve("../.."))
            .map(Unchecked.function(Path::toRealPath))
            .orElse(projectPath);
      }
    } finally {
      try {
        resource.delete();
      } catch (Exception ex) {
        processingEnv.getMessager().printMessage(Kind.ERROR, ex.getMessage());
      }
    }
    printNote("Project path: " + projectPath);
    if (javaFileObject.toUri().getScheme().equals("file")) {
      printNote("Creating file: " + Paths.get(javaFileObject.toUri()));
    }

    Path root = projectPath.resolve(pathsAsEnum.value());
    Map<String, FieldSpec> fieldSpecsMap = new HashMap<>();
    TreeVisitor treeVisitor = new TreeVisitor(root, pathsAsEnum.regExp(), fieldSpecsMap);
    Files.walkFileTree(root, treeVisitor);
    fieldSpecsMap.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(Map.Entry::getValue)
        .forEach(interfaceBuilder::addField);

    TypeSpec interfaceSpec = interfaceBuilder
        .build();

    JavaFile javaFile = JavaFile.builder(packageName, interfaceSpec)
        .build();

    try (Writer writer = javaFileObject.openWriter()) {
      javaFile.writeTo(writer);
    }
  }

  private class TreeVisitor implements FileVisitor<Path> {

    final Path root;
    final Pattern filePattern;
    final Map<String, FieldSpec> fieldSpecsMap;

    private TreeVisitor(Path root, String filePattern, Map<String, FieldSpec> fieldSpecsMap) {
      this.root = root;
      this.filePattern = Pattern.compile(filePattern);
      this.fieldSpecsMap = fieldSpecsMap;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (filePattern.matcher(file.toString()).matches()) {
        final String fileName = root.relativize(file).toString();
        final String name = fileName.toUpperCase(Locale.US).replaceAll("[^A-Z0-9_]+", "_");
        printNote("Found path " + name + " = " + fileName);
        FieldSpec fieldSpec = FieldSpec.builder(String.class, name)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", fileName)
            .build();
        fieldSpecsMap.put(name, fieldSpec);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      if (exc != null) {
        throw new RuntimeException(exc);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (exc != null) {
        throw new RuntimeException(exc);
      }
      return FileVisitResult.CONTINUE;
    }
  }

  private void printNote(String message) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
  }

  private void printError(String message) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
  }
}
