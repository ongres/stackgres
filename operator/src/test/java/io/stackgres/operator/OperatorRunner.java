package io.stackgres.operator;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ongres.junit.docker.Container;

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

import org.jooq.lambda.fi.lang.CheckedRunnable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperatorRunner implements CheckedRunnable, Closeable {

  private final static Logger LOGGER = LoggerFactory.getLogger(OperatorRunner.class);

  private final Class<?> testClass;
  private final Container kind;
  private final int port;
  private final int sslPort;

  private URLClassLoader appCl;
  private ClassLoader originalCl;
  private RuntimeRunner runtimeRunner;

  public OperatorRunner(Class<?> testClass, Container kind, int port, int sslPort) {
    super();
    this.testClass = testClass;
    this.kind = kind;
    this.port = port;
    this.sslPort = sslPort;
  }

  @Override
  public void close() throws IOException {
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
  }

  private void setup() throws Exception {
    List<String> kubeconfig = kind.execute("sh", "-l", "-c", "cat $KUBECONFIG")
        .collect(Collectors.toList());
    List<String> operatorSecret = kind.execute("sh", "-l", "-c",
        "kubectl get secret -n stackgres -o yaml"
            + " \"$(kubectl get secret -n stackgres"
            + " | grep stackgres-operator-token-"
            + " | sed 's/\\s\\+/ /g'"
            + " | cut -d ' ' -f 1)\"")
        .collect(Collectors.toList());
    System.setProperty("kubernetes.master", kubeconfig.stream()
        .filter(line -> line.startsWith("    server: "))
        .findAny().get()
        .substring("    server: ".length()));
    System.setProperty("kubernetes.certs.ca.data", operatorSecret.stream()
        .filter(line -> line.startsWith("  ca.crt: "))
        .map(line -> line.substring("  ca.crt: ".length()))
        .map(secret -> new String(Base64.getDecoder().decode(secret), StandardCharsets.UTF_8))
        .findAny().get());
    System.setProperty("kubernetes.auth.token", operatorSecret.stream()
        .filter(line -> line.startsWith("  token: "))
        .map(line -> line.substring("  token: ".length()))
        .map(secret -> new String(Base64.getDecoder().decode(secret), StandardCharsets.UTF_8))
        .findAny().get());
    LOGGER.info("Setup fabric8 to connect to {}", System.getProperty("kubernetes.master"));
    System.setProperty("quarkus.http.test-port", String.valueOf(port));
    System.setProperty("quarkus.http.test-ssl-port", String.valueOf(sslPort));

    Path appClassLocation = getAppClassLocation(testClass);

    appCl = createQuarkusBuildClassLoader(testClass, appClassLocation);
    originalCl = setCCL(appCl);

    final Path testClassLocation = getTestClassesLocation(testClass);
    final ClassLoader testClassLoader = testClass.getClassLoader();
    final Path testWiringClassesDir;
    final RuntimeRunner.Builder runnerBuilder = RuntimeRunner.builder();

    if (Files.isDirectory(testClassLocation)) {
      testWiringClassesDir = testClassLocation;
    } else {
      runnerBuilder.addAdditionalArchive(testClassLocation);
      testWiringClassesDir =
          Paths.get("").normalize().toAbsolutePath().resolve("target").resolve("wiring-classes");
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

    runtimeRunner = runnerBuilder.setLaunchMode(LaunchMode.TEST).setClassLoader(appCl)
        .setTarget(appClassLocation).addAdditionalArchive(testWiringClassesDir)
        .setClassOutput(new ClassOutput() {
          @Override
          public void writeClass(boolean applicationClass, String className, byte[] data)
              throws IOException {
            Path location = testWiringClassesDir.resolve(className.replace('.', '/') + ".class");
            Files.createDirectories(location.getParent());
            try (FileOutputStream out = new FileOutputStream(location.toFile())) {
              out.write(data);
            }
          }

          @Override
          public void writeResource(String name, byte[] data) throws IOException {
            Path location = testWiringClassesDir.resolve(name);
            Files.createDirectories(location.getParent());
            try (FileOutputStream out = new FileOutputStream(location.toFile())) {
              out.write(data);
            }
          }
        }).setTransformerTarget(new TransformerTarget() {
          @Override
          public void setTransformers(
              Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions) {
            ClassLoader main = Thread.currentThread().getContextClassLoader();

            // we need to use a temp class loader, or the old resource location will be cached
            ClassLoader temp = new ClassLoader() {
              @Override
              protected Class<?> loadClass(String name, boolean resolve)
                  throws ClassNotFoundException {
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
                  System.err.println("Failed to transform " + e.getKey());
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
                Files.createDirectories(location.getParent());
                try (FileOutputStream out = new FileOutputStream(location.toFile())) {
                  out.write(cw.toByteArray());
                }
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            }
          }
        }).addChainCustomizer(new Consumer<BuildChainBuilder>() {
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
            }).produces(TestClassPredicateBuildItem.class).build();
          }
        }).addChainCustomizer(new Consumer<BuildChainBuilder>() {
          @Override
          public void accept(BuildChainBuilder buildChainBuilder) {
            buildChainBuilder.addBuildStep(new BuildStep() {
              @Override
              public void execute(BuildContext context) {
                context.produce(new TestAnnotationBuildItem(QuarkusTest.class.getName()));
              }
            }).produces(TestAnnotationBuildItem.class).build();
          }
        }).build();
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
}
