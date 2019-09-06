package io.stackgres.operator;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

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

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.lang.CheckedRunnable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItHelper {

  private final static Logger LOGGER = LoggerFactory.getLogger(ItHelper.class);

  public final static Predicate<String> EXCLUDE_TTY_WARNING = line -> !line.equals("stdin: is not a tty");


  /**
   * IT helper method.
   */
  public static void copyResources(Container kind) throws Exception {
    kind.execute("rm", "-Rf", "/resources").forEach(line -> LOGGER.info(line));
    kind.copyResourcesIn("/", ItHelper.class, "/resources");
  }

  /**
   * It helper method.
   */
  public static void restartKind(Container kind) throws Exception {
    LOGGER.info("Restarting kind");
    kind.execute("bash", "/scripts/restart-kind.sh")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void createNamespace(Container kind, String namespace) throws Exception {
    LOGGER.info("Create namespace '" + namespace + "'");
    kind.execute("bash", "-l", "-c",
        "kubectl create namespace " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteNamespaceIfExists(Container kind, String namespace) throws Exception {
    LOGGER.info("Deleting namespace if exists '" + namespace + "'");
    kind.execute("bash", "-l", "-c",
        "kubectl delete namespace --ignore-not-found " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteStackGresOperatorHelmChartIfExists(Container kind) throws Exception {
    LOGGER.info("Deleting if exists stackgres-operator helm");
    kind.execute("bash", "-l", "-c", "helm template /resources/stackgres-operator"
        + " --name stackgres-operator --set-string deploy.create=false"
        + " | kubectl delete --ignore-not-found -f -")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    kind.execute("bash", "-l", "-c", "helm delete stackgres-operator --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void installStackGresOperatorHelmChart(Container kind) throws Exception {
    LOGGER.info("Installing stackgres-operator helm chart");
    kind.execute("bash", "-l", "-c", "helm install /resources/stackgres-operator"
        + " --name stackgres-operator --set-string deploy.create=false")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void createStackGresConfigs(Container kind, String namespace) throws Exception {
    Arrays.asList(new String[] {
        "/resources/stackgres-v1alpha1-sgprofile-cr.yaml",
        "/resources/stackgres-v1alpha1-sgpgconfig-cr.yaml",
        "/resources/stackgres-v1alpha1-sgpgbouncerconfig-cr.yaml"
    })
        .forEach(Unchecked.consumer(crFile -> {
          LOGGER.info("Creating CR from file '" + crFile + "' in namespace '" + namespace + "'");
          kind.execute("bash", "-l", "-c",
              "kubectl create -n " + namespace + " -f " + crFile)
              .filter(EXCLUDE_TTY_WARNING)
              .forEach(line -> LOGGER.info(line));
        }));
  }

  /**
   * It helper method.
   */
  public static void createStackGresCluster(Container kind, String namespace) throws Exception {
    Arrays.asList(new String[] {
        "/resources/stackgres-v1alpha1-sgcluster-cr.yaml"
    })
        .forEach(Unchecked.consumer(crFile -> {
          LOGGER.info("Creating CR from file '" + crFile + "' in namespace '" + namespace + "'");
          kind.execute("bash", "-l", "-c",
              "kubectl create -n " + namespace + " -f " + crFile)
              .filter(EXCLUDE_TTY_WARNING)
              .forEach(line -> LOGGER.info(line));
        }));
  }

  /**
   * It helper method.
   */
  public static void waitUntilOperatorIsReady(WebTarget operatorClient) throws Exception {
    Instant timeout = Instant.now().plusSeconds(30);
    while (true) {
      if (Instant.now().isAfter(timeout)) {
        throw new TimeoutException();
      }
      TimeUnit.MILLISECONDS.sleep(100);
      try {
        if (operatorClient.path("/health")
            .request(MediaType.APPLICATION_JSON)
            .get().getStatusInfo().equals(Status.OK)) {
          break;
        }
      } catch (ProcessingException ex) {
        if (ex.getCause() instanceof ConnectException) {
          continue;
        }
        throw ex;
      }
    }
  }

  public interface OperatorRunner extends CheckedRunnable, Closeable {
  }

  /**
   * IT helper method.
   * Code has been copied and adapted from {@code QuarkusTestExtension} to allow start/stop
   * quarkus application inside a test.
   */
  public static OperatorRunner createOperator(Class<?> testClass, Container kind, int port) throws Exception {
    return new OperatorRunner() {
      private URLClassLoader appCl;
      private ClassLoader originalCl;
      private RuntimeRunner runtimeRunner;

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
        List<String> kubeconfig = kind.execute("bash", "-l", "-c", "cat $KUBECONFIG")
            .collect(Collectors.toList());
          System.setProperty("kubernetes.certs.ca.data", kubeconfig.stream()
              .filter(line -> line.startsWith("    certificate-authority-data: "))
              .findAny().get()
              .substring("    certificate-authority-data: ".length()));
          System.setProperty("kubernetes.master", kubeconfig.stream()
              .filter(line -> line.startsWith("    server: "))
              .findAny().get()
              .substring("    server: ".length()));
          System.setProperty("kubernetes.auth.basic.username", "kubernetes-admin");
          System.setProperty("kubernetes.certs.client.data", kubeconfig.stream()
              .filter(line -> line.startsWith("    client-certificate-data: "))
              .findAny().get()
              .substring("    client-certificate-data: ".length()));
          System.setProperty("kubernetes.certs.client.key.data", kubeconfig.stream()
              .filter(line -> line.startsWith("    client-key-data: "))
              .findAny().get()
              .substring("    client-key-data: ".length()));
          LOGGER.info("Setup fabric8 to connect to {}", System.getProperty("kubernetes.master"));
          System.setProperty("quarkus.http.test-port", String.valueOf(port));

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
                Paths.get("").normalize().toAbsolutePath().resolve("target").resolve("test-classes");
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
        runtimeRunner.run();
      }
    };
  }

  private static URLClassLoader createQuarkusBuildClassLoader(Class<?> testClass, Path appClassLocation) {
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

  private static ClassLoader setCCL(ClassLoader cl) {
      final Thread thread = Thread.currentThread();
      final ClassLoader original = thread.getContextClassLoader();
      thread.setContextClassLoader(cl);
      return original;
  }

  public static <T> void waitUntil(Supplier<T> supplier, Predicate<T> condition, int timeout,
      TemporalUnit unit, Consumer<T> onTimeout) throws Exception {
    Instant end = Instant.now().plus(Duration.of(timeout, unit));
    while (true) {
      if (Instant.now().isAfter(end)) {
        onTimeout.accept(supplier.get());
      }
      TimeUnit.SECONDS.sleep(1);
      try {
        if (condition.test(supplier.get())) {
          break;
        }
      } catch (Exception ex) {
        continue;
      }
    }
  }

  private ItHelper() {
  }
}
