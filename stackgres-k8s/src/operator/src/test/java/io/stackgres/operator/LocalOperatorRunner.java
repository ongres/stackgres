/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.io.Closer;
import com.ongres.junit.docker.Container;
import io.fabric8.kubernetes.client.Config;
import io.quarkus.bootstrap.BootstrapClassLoaderFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runner.TransformerTarget;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Unchecked;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalOperatorRunner implements OperatorRunner {

  private final static Logger LOGGER = LoggerFactory.getLogger(LocalOperatorRunner.class);

  /**
   * As part of the test run we need to create files in the test-classes directory
   *
   * We attempt to clean these up with a shutdown hook, but if the processes is killed (e.g. hitting the red
   * IDE button) it can leave these files behind which interfere with subsequent runs.
   *
   * To fix this we create a file that contains the names of all the files we have created, and at the start of a new
   * run we remove them if this file exists.
   */
  private static final String CREATED_FILES = "CREATED_FILES.txt";

  private final Container k8s;
  private final Class<?> testClass;
  private final int port;
  private final int sslPort;

  private URLClassLoader appCl;
  private ClassLoader originalCl;
  private RuntimeRunner runtimeRunner;
  private CompletableFuture<Void> running = new CompletableFuture<>();

  public LocalOperatorRunner(Container k8s, Class<?> testClass, int port, int sslPort) {
    super();
    this.k8s = k8s;
    this.testClass = testClass;
    this.port = port;
    this.sslPort = sslPort;
  }

  @Override
  public void close() throws IOException {
    running.join();
    if (runtimeRunner != null) {
      runtimeRunner.close();
    }
    if (originalCl != null) {
      setCCL(originalCl);
    }
    if (appCl != null) {
      appCl.close();
    }
  }

  @Override
  public void run() throws Exception {
    setup();
    runtimeRunner.run();
    running.complete(null);
  }

  private void setup() throws Exception {
    List<String> kubeconfig = k8s.execute("sh", "-l", "-c", "kubectl config view --minify --raw")
        .collect(Collectors.toList());
    System.setProperty("quarkus.http.ssl.certificate.file", "src/test/resources/certs/server.crt");
    System.setProperty("quarkus.http.ssl.certificate.key-file", "src/test/resources/certs/server-key.pem");
    System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, Boolean.FALSE.toString());
    System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, Boolean.FALSE.toString());
    System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, kubeconfig.stream()
        .filter(line -> line.startsWith("    server: "))
        .findAny().get()
        .substring("    server: ".length()));

    LOGGER.info("Setup fabric8 to connect to {}", System.getProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY));
    System.setProperty("quarkus.http.test-port", String.valueOf(port));
    System.setProperty("quarkus.http.test-ssl-port", String.valueOf(sslPort));

    final Path appClassLocation = getAppClassLocation(testClass);
    final Path testClassLocation = getTestClassesLocation(testClass);
    //Ugly hack to remove duplicated class files from target/test-class generated by quarkus that breaks CDI on re-run
    try (Closer closer = Closer.create()) {
      final Path frameworkClassLocation = getClassLocation(Validator.class, closer);
      final Path commonClassLocation = getClassLocation(StackGresUtil.class, closer);
      Files.walkFileTree(frameworkClassLocation, new DeleteVisitor(frameworkClassLocation, testClassLocation));
      Files.walkFileTree(commonClassLocation, new DeleteVisitor(commonClassLocation, testClassLocation));
      Files.walkFileTree(appClassLocation, new DeleteVisitor(appClassLocation, testClassLocation));
    }

    appCl = createQuarkusBuildClassLoader(testClass, appClassLocation);
    originalCl = setCCL(appCl);

    final ClassLoader testClassLoader = testClass.getClassLoader();
    final Path testWiringClassesDir;
    final RuntimeRunner.Builder runnerBuilder = RuntimeRunner.builder();

    if (Files.isDirectory(testClassLocation)) {
      testWiringClassesDir = testClassLocation;
    } else {
      runnerBuilder.addAdditionalArchive(testClassLocation);
      testWiringClassesDir =
          Paths.get("").normalize().toAbsolutePath().resolve("target").resolve("test-wiring-classes");
      if (Files.exists(testWiringClassesDir)) {
        IoUtils.recursiveDelete(testWiringClassesDir);
      }
      try {
        Files.createDirectories(testWiringClassesDir);
      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to create a directory for wiring test classes at " + testWiringClassesDir, e);
      }
    }

    Path createdFilesPath = testWiringClassesDir.resolve(CREATED_FILES);
    if (Files.exists(createdFilesPath)) {
      cleanupOldRun(createdFilesPath);
    }
    this.runtimeRunner = runnerBuilder
        .setLaunchMode(LaunchMode.TEST)
        .setClassLoader(appCl)
        .setTarget(appClassLocation)
        .addAdditionalArchive(testWiringClassesDir)
        .setClassOutput(new ClassOutput() {
          @Override
          public void writeClass(boolean applicationClass, String className, byte[] data) throws IOException {
            Path location = testWiringClassesDir.resolve(className.replace('.', '/') + ".class");
            Optional.ofNullable(location.getParent()).ifPresent(Unchecked.consumer(Files::createDirectories));
            writeToCreatedFile(testWiringClassesDir, createdFilesPath, location);
            Files.write(location, data);
          }

          @Override
          public void writeResource(String name, byte[] data) throws IOException {
            Path location = testWiringClassesDir.resolve(name);
            Optional.ofNullable(location.getParent()).ifPresent(Unchecked.consumer(Files::createDirectories));
            writeToCreatedFile(testWiringClassesDir, createdFilesPath, location);
            Files.write(location, data);
          }
        })
        .setTransformerTarget(new TransformerTarget() {
          @Override
          public void setTransformers(
              Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions) {
            ClassLoader main = Thread.currentThread().getContextClassLoader();

            //we need to use a temp class loader, or the old resource location will be cached
            ClassLoader temp = new ClassLoader() {
              @Override
              protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // First, check if the class has already been loaded
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                  c = findClass(name);
                }
                if (resolve) {
                  resolveClass(c);
                }
                return c;
              }

              @Override
              public URL getResource(String name) {
                return main.getResource(name);
              }

              @Override
              public Enumeration<URL> getResources(String name) throws IOException {
                return main.getResources(name);
              }
            };
            for (Map.Entry<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> e : functions
                .entrySet()) {
              String resourceName = e.getKey().replace('.', '/') + ".class";
              try (InputStream stream = temp.getResourceAsStream(resourceName)) {
                if (stream == null) {
                  LOGGER.error("Failed to transform " + e.getKey());
                  continue;
                }
                byte[] data = IoUtil.readBytes(stream);

                ClassReader cr = new ClassReader(data);
                ClassWriter cw = new QuarkusClassWriter(cr,
                    ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {

                  @Override
                  protected ClassLoader getClassLoader() {
                    return temp;
                  }
                };
                ClassLoader old = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(temp);
                try {
                  ClassVisitor visitor = cw;
                  for (BiFunction<String, ClassVisitor, ClassVisitor> i : e.getValue()) {
                    visitor = i.apply(e.getKey(), visitor);
                  }
                  cr.accept(visitor, 0);
                } finally {
                  Thread.currentThread().setContextClassLoader(old);
                }

                Path location = testWiringClassesDir.resolve(resourceName);
                Optional.ofNullable(location.getParent()).ifPresent(Unchecked.consumer(Files::createDirectories));
                writeToCreatedFile(testWiringClassesDir, createdFilesPath, location);
                Files.write(location, cw.toByteArray());
              } catch (IOException ex) {
                LOGGER.error("Error", ex);
              }
            }
          }
        })
        .addChainCustomizer(new Consumer<BuildChainBuilder>() {
          @Override
          public void accept(BuildChainBuilder buildChainBuilder) {
            buildChainBuilder.addBuildStep(new BuildStep() {
              @Override
              public void execute(BuildContext context) {
                context.produce(new TestClassPredicateBuildItem(new Predicate<String>() {
                  @Override
                  public boolean test(String className) {
                    return PathTestHelper.isTestClass(className, testClassLoader);
                  }
                }));
              }
            }).produces(TestClassPredicateBuildItem.class)
            .build();
          }
        })
        .addChainCustomizer(new Consumer<BuildChainBuilder>() {
          @Override
          public void accept(BuildChainBuilder buildChainBuilder) {
            buildChainBuilder.addBuildStep(new BuildStep() {
              @Override
              public void execute(BuildContext context) {
                context.produce(new TestAnnotationBuildItem(QuarkusTest.class.getName()));
              }
            }).produces(TestAnnotationBuildItem.class)
            .build();
          }
        })
        .build();
  }

  private ClassLoader setCCL(ClassLoader cl) {
      final Thread thread = Thread.currentThread();
      final ClassLoader original = thread.getContextClassLoader();
      thread.setContextClassLoader(cl);
      return original;
  }

  private URLClassLoader createQuarkusBuildClassLoader(Class<?> testClass, Path appClassLocation) {
    // The deployment classpath could be passed in as a system property.
    // This is how integration with the Gradle plugin is achieved.
    final String deploymentCp =
        PropertyUtils.getProperty(BootstrapClassLoaderFactory.PROP_DEPLOYMENT_CP);
    if (deploymentCp != null && !deploymentCp.isEmpty()) {
      final List<URL> list = new ArrayList<>();
      for (String entry : deploymentCp.split("\\s")) {
        try {
          list.add(new URL(entry));
        } catch (MalformedURLException e) {
          throw new IllegalStateException("Failed to parse a deployment classpath entry " + entry,
              e);
        }
      }
      return new URLClassLoader(list.toArray(new URL[list.size()]), testClass.getClassLoader());
    }
    try {
      return BootstrapClassLoaderFactory.newInstance().setAppClasses(appClassLocation)
          .setParent(testClass.getClassLoader())
          .setOffline(PropertyUtils.getBooleanOrNull(BootstrapClassLoaderFactory.PROP_OFFLINE))
          .setLocalProjectsDiscovery(
              PropertyUtils.getBoolean(BootstrapClassLoaderFactory.PROP_WS_DISCOVERY, true))
          .setEnableClasspathCache(
              PropertyUtils.getBoolean(BootstrapClassLoaderFactory.PROP_CP_CACHE, true))
          .newDeploymentClassLoader();
    } catch (BootstrapException e) {
      throw new IllegalStateException("Failed to create the boostrap class loader", e);
    }
  }

  private void writeToCreatedFile(final Path testWiringClassesDir,
      Path createdFilesPath, Path location) throws IOException {
    try (OutputStream created = Files.newOutputStream(createdFilesPath)) {
      created.write((testWiringClassesDir.relativize(location).toString() + "\n").getBytes(StandardCharsets.UTF_8));
      created.flush();
    }
  }

  private void cleanupOldRun(Path createdFilesPath) {
      try (BufferedReader reader = Files.newBufferedReader(createdFilesPath)) {
        String line;
        while ((line = reader.readLine()) != null) {
          final String currentLine = line;
          Optional.ofNullable(createdFilesPath.getParent())
              .map(parent -> parent.resolve(currentLine))
              .ifPresent(Unchecked.consumer(Files::deleteIfExists));
        }
        Files.deleteIfExists(createdFilesPath);
      } catch (IOException ex) {
        LOGGER.error("Error", ex);
      }
  }

  private Path getClassLocation(Class<?> reference, Closer closer) throws URISyntaxException, IOException {
    String resource = reference.getName().replace('.', File.separatorChar) + ".class";
    URI resourceUri = URI.create(reference.getClassLoader()
        .getResource(resource).toURI().toString().replace(resource, ""));
    final Path classLocation;
    closer.register(createFileSystemIfNotFound(resourceUri));
    classLocation = Paths.get(resourceUri);
    return classLocation;
  }

  private Closeable createFileSystemIfNotFound(URI uri) throws IOException, URISyntaxException {
    try {
      Paths.get(uri);
    } catch (FileSystemNotFoundException ex) {
      return FileSystems.newFileSystem(uri, new HashMap<>());
    }
    return () -> { };
  }

  private class DeleteVisitor extends SimpleFileVisitor<Path> {
    private final Path sourcePath;
    private final Path targetPath;

    private DeleteVisitor(Path sourcePath, Path targetPath) {
      this.sourcePath = sourcePath;
      this.targetPath = targetPath;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
        throws IOException {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException {
      Files.deleteIfExists(targetPath.resolve(sourcePath.relativize(file).toString()));
      return FileVisitResult.CONTINUE;
    }
  }
}
