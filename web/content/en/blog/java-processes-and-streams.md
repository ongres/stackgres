+++
title = "Java, Processes and Streams"
date = 2020-11-24T10:00:00+00:00
author = ["teoincontatto"]
tags = ["java", "process", "streams"]
draft = false
readtime = "10 min"
thumbnail = "img/blog/java-process-and-streams.jpg"
description = "FluentProcess brings the Java Streams interface to OS processes: spawn commands, pipe them together and handle I/O without ProcessBuilder boilerplate."
+++

When it comes to spawning processes in Java, `ProcessBuilder` offers a good way to get started. The
problem with this approach is when you want to work with the inputs and outputs of a process and you
do not want to write another time a lot of boilerplate code just to handle `Process`'s `OutputStream`
and `InputStream`s.

If you also want to use the powerful interface offered by Java Streams and / or pipelining more
commands together, the code to write and the not too easy problems to solve make the task really
annoying and error prone.

In the Java library land there are some options to work with. Let's have a look at some of them.

## Apache Commons Exec

[Apache Commons Exec](http://commons.apache.org/proper/commons-exec/) is probably one of the oldest and
more well tested wrappers for Java Processes. It uses `Runtime.getRuntime().exec(...)` under the hood
since it is compatible with Java 1.3. It also offers some handy methods to parse command line
arguments and to execute a process wrapped for some old operating systems --like OS2, VMS or
WindowsNT. For modern development this is probably not the most appealing option though, since you
still have to write code for `InputStream` and `OutputStream` if you want to work with newer Java
APIs like Streams. It is also a bit heavy weight since the default implementation uses 3 additional
threads to handle reading and writing of process stdin, stdout and stderr, respectively.

## ZT Process Executor

[ZT Process Executor](https://github.com/zeroturnaround/zt-exec) is a much more modern library providing
a lot of functionality around Java 1.6+ `ProcessBuilder` and `Process`. In particular it offers an
interface to watch for process input, output and other events, and a wrapper for Java Future.
Still, the implementation mimics the one used by Apache Commons Exec when it comes to handle reading and
writing of the process' stdin, stdout and stderr, with 3 additional threads running in background.

## JProc

[JProc](https://github.com/fleipold/jproc) is also a modern library, supporting Java 1.7+ and
provides a fluent API that is really nice to use. The only drawback is that running a process
also creates 3 additional threads to handle reading and writing of process stdin, stdout and
stderr.

## Fluent Process

[Fluent Process](https://gitlab.com/ongresinc/fluent-process) is a new, simple and lightweight
wrapper around Java 1.8+ `ProcessBuilder` and `Process` that offers a fluent Java API. It allows to
send outputs to a Java Stream and/or send a Java Stream as input of a process. You can work with a
process outputs or inputs in a single thread. For more complex cases you will have to use threads
and the library offer some handy method for that too, leveraging Completable Future Java API
and allowing to override the default executor.

## Usage examples

### Simple example

Let's try to write down some code using what Java offers, and how it compares using those 3 libraries.

We want to make this simple BASH script that is a `sed` command receiving as input two lines ("hello"
and "world") and converting them into "hello" and "process":

```shell
(echo hello; echo world) | sed 's/world/process/'
```

- JDK Only

Quite some boilerplate code is involved when it comes to handle reading and writing from and to
an `InputStream` and `OutputStream`. We also relied on `CompletableFuture` to make things a bit
more agile --this would have become much more complex for an older Java version.

```java
    ProcessBuilder sedBuilder = new ProcessBuilder("sed", "s/world/process/");

    Process sed = sedBuilder.start();

    InputStream inputStream = new ByteArrayInputStream("hello\nworld".getBytes(StandardCharsets.UTF_8));
    CompletableFuture<Void> sedInput = CompletableFuture.runAsync(() -> {
      try {
        try {
          byte[] buffer = new byte[8192];
          while (true) {
            int size = inputStream.read(buffer);
            if (size < 0) {
              break;
            }
            if (size > 0) {
              sed.getOutputStream().write(buffer, 0, size);
            }
          }
        } finally {
          sed.getOutputStream().close();
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });

    try {
      byte[] buffer = new byte[8192];
      while (true) {
        int size = sed.getInputStream().read(buffer);
        if (size < 0) {
          break;
        }
        if (size > 0) {
          System.out.write(buffer, 0, size);
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    sed.waitFor();
    if (sed.exitValue() != 0) {
      throw new Exception(
          "Process " + sedBuilder.command() + " has failed with exit code " + sed.exitValue());
    }
    sedInput.join();
```

- Apache Commons Exec

The same code using Apache Commons Exec is significantly smaller, even if the error code handling requires
some extra ifs.

```java
    CommandLine sed = new CommandLine("sed")
        .addArgument("s/world/process/");

    DefaultExecuteResultHandler sedExecutionHandler = new DefaultExecuteResultHandler();
    Executor sedExecutor = new DefaultExecutor();
    sedExecutor.setStreamHandler(new PumpStreamHandler(System.out, null,
        new ByteArrayInputStream("hello\nworld".getBytes(StandardCharsets.UTF_8))));

    sedExecutor.execute(sed, sedExecutionHandler);

    sedExecutionHandler.waitFor();
    if ((sedExecutionHandler.getExitValue() != 0)
        || sedExecutionHandler.getException() != null) {
      if (sedExecutionHandler.getException() != null) {
        throw new Exception(
            "Process " + sed + " has failed with exit code " + sedExecutionHandler.getExitValue(),
            sedExecutionHandler.getException());
      }
      throw new Exception(
          "Process " + sed + " failed with exit code " + sedExecutionHandler.getExitValue());
    }
```

- ZT Process Executor

ZT Process Executor adds some more sugar to the problem compared to Apache Commons Exec. The
resulting code is indeed quite small and looks simpler.

```java
    ProcessExecutor sedExecutor = new ProcessExecutor("sed", "s/world/process/")
        .redirectInput(new ByteArrayInputStream("hello\nworld".getBytes(StandardCharsets.UTF_8)))
        .redirectOutput(System.out);

    StartedProcess sed = sedExecutor
        .start();

    ProcessResult sedResult = sed.getFuture().get();
    if (sedResult.getExitValue() != 0) {
      throw new Exception(
          "Process " + sedExecutor.getCommand() + " has failed with exit code " + sedResult.getExitValue());
    }
```

- JProc

Similar to ZT Process Executor we can see some candies here!

```java
    ProcResult sedResult = new ProcBuilder("sed", "s/world/process/")
        .withInputStream(new ByteArrayInputStream("hello\nworld".getBytes(StandardCharsets.UTF_8)))
        .withOutputStream(System.out)
        .run();
```

- Fluent Process

Fluent Process further simplifies this example to only 3 lines of code and can be run using the main
thread only.

```java
    FluentProcess.start("sed", "s/world/process/")
        .inputStream(new ByteArrayInputStream("hello\nworld".getBytes(StandardCharsets.UTF_8)))
        .writeToOutputStream(System.out);
```

### Complex example

Let's now try to make things a bit more complex, by adding a `cat` between the lines and the
`sed` command. We also introduced an `exit 79` just to spice up things a bit and a check to make
sure our intermediate command just fails if `79` is not returned:

```shell
set -o pipefail
(echo hello; echo world) | (sh -c 'cat; exit 79'; [ \"\$?\" = 79 ]) | sed 's/world/process/')"
```

To make it less boring we will have the output be a Strings Java Stream.

- JDK Only

The code is now quite huge using the JDK. It is doable but we need some sugar to make it more
readable.

```java
    ProcessBuilder shBuilder = new ProcessBuilder("sh", "-c",
        Stream.of("cat", "exit 79").collect(Collectors.joining("\n")));
    ProcessBuilder sedBuilder = new ProcessBuilder("sed", "s/world/process/");
    Stream<String> inputStream = Stream.of(
        "hello",
        "world"
        );

    Process sh = shBuilder.start();
    Process sed = sedBuilder.start();

    CompletableFuture<Void> shInput = CompletableFuture.runAsync(() -> {
      try {
        try {
          Iterator<byte[]> iterator = inputStream
              .map(line -> (line + "\n").getBytes(StandardCharsets.UTF_8))
              .iterator();
          while (iterator.hasNext()) {
            sh.getOutputStream().write(iterator.next());
          }
        } finally {
          sh.getOutputStream().close();
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });

    CompletableFuture<Void> sedInput = CompletableFuture.runAsync(() -> {
      try {
        try {
          byte[] buffer = new byte[8192];
          while (true) {
            int size = sh.getInputStream().read(buffer);
            if (size < 0) {
              break;
            }
            if (size > 0) {
              sed.getOutputStream().write(buffer, 0, size);
            }
          }
        } finally {
          sed.getOutputStream().close();
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });

    try (BufferedReader sedReader = new BufferedReader(
        new InputStreamReader(sed.getInputStream(), StandardCharsets.UTF_8))) {
      sedReader.lines()
          .peek(System.out::println)
          .collect(Collectors.toList());
      sh.waitFor();
      sed.waitFor();
      if (sh.exitValue() != 79) {
        throw new Exception(
            "Process " + shBuilder.command() + " has failed with exit code " + sh.exitValue());
      }
      if (sed.exitValue() != 0) {
        throw new Exception(
            "Process " + sedBuilder.command() + " has failed with exit code " + sed.exitValue());
      }
      shInput.join();
      sedInput.join();
    }
```

- Apache Commons Exec

With Apache Commons Exec, conterintuitively, the code becomes larger compared to using just the JDK. And, ...it
does not work?! This code will fail since at some point the process OutputStream is closed making
it impossible to achieve the wanted result. It may be due to the use of `PipeInputStream` and
`PipeOutputStream` but not using them would require some more threads, including the 3 threads per
process that the library is spawning, to make the things work.

```java
    final CommandLine sh = new CommandLine("sh")
        .addArgument("-c")
        .addArgument(Stream.of(
            "cat",
            "exit 79"
            )
            .collect(Collectors.joining("\n")));
    final CommandLine sed = new CommandLine("sed")
        .addArgument("s/world/process/");
    Stream<String> inputStream = Stream.of(
        "hello",
        "world"
        );
    InputStream shInputStream = new InputStream() {
      final Iterator<Byte> iterator = inputStream
          .map(line -> line.getBytes(StandardCharsets.UTF_8))
          .flatMap(bytes -> {
            List<Byte> byteList = new ArrayList<>(bytes.length);
            for (byte value : bytes) {
              byteList.add(value);
            }
            return byteList.stream();
          })
          .iterator();
      @Override
      public int read() throws IOException {
        if (!iterator.hasNext()) {
          return -1;
        }
        return iterator.next().byteValue();
      }
    };

    DefaultExecuteResultHandler shExecutionHandler = new DefaultExecuteResultHandler();
    Executor shExecutor = new DefaultExecutor();
    PipedOutputStream shOutputStream = new PipedOutputStream();
    PipedInputStream sedInputStream = new PipedInputStream(shOutputStream);
    shExecutor.setStreamHandler(new PumpStreamHandler(shOutputStream, null, shInputStream));

    DefaultExecuteResultHandler sedExecutionHandler = new DefaultExecuteResultHandler();
    Executor sedExecutor = new DefaultExecutor();
    PipedOutputStream sedOutputStream = new PipedOutputStream();
    PipedInputStream sedInputStreamForOutput = new PipedInputStream(sedOutputStream);
    sedExecutor.setStreamHandler(new PumpStreamHandler(sedOutputStream, null, sedInputStream));

    shExecutor.execute(sh, shExecutionHandler);
    sedExecutor.execute(sed, sedExecutionHandler);

    try (BufferedReader sedReader = new BufferedReader(
        new InputStreamReader(sedInputStreamForOutput, StandardCharsets.UTF_8))) {
      sedReader.lines()
          .peek(System.out::println)
          .collect(Collectors.toList());
      shExecutionHandler.waitFor();
      if ((shExecutionHandler.getExitValue() != 79)
          || shExecutionHandler.getException() != null) {
        if (shExecutionHandler.getException() != null) {
          throw new Exception(
              "Process " + sh + " has failed with exit code " + shExecutionHandler.getExitValue(),
              shExecutionHandler.getException());
        }
        throw new Exception(
            "Process " + sh + " has failed with exit code " + shExecutionHandler.getExitValue());
      }
      sedExecutionHandler.waitFor();
      if ((sedExecutionHandler.getExitValue() != 0)
          || sedExecutionHandler.getException() != null) {
        if (sedExecutionHandler.getException() != null) {
          throw new Exception(
              "Process " + sed + " has failed with exit code " + sedExecutionHandler.getExitValue(),
              sedExecutionHandler.getException());
        }
        throw new Exception(
            "Process " + sed + " failed with exit code " + sedExecutionHandler.getExitValue());
      }
    }
```

- ZT Process Executor

The boilerplate code is quite similar to the Apache Commons Exec. The library also indroduces an
error, not sure if it is a bug or not, that makes the `PipeInputStream` to fail with a
`IOException` saying "Write end dead" probably due to the `PipeOutputStream` being closed
prematurely. It can be fixed with an ugly wrapper that get rid of the error, but I hope this will
be fixed in future releases.

```java
    Stream<String> inputStream = Stream.of(
        "hello",
        "world"
        );
    InputStream shInputStream = new InputStream() {
      final Iterator<Byte> iterator = inputStream
          .map(line -> (line + "\n").getBytes(StandardCharsets.UTF_8))
          .flatMap(bytes -> {
            List<Byte> byteList = new ArrayList<>(bytes.length);
            for (byte value : bytes) {
              byteList.add(value);
            }
            return byteList.stream();
          })
          .iterator();
      @Override
      public int read() throws IOException {
        if (!iterator.hasNext()) {
          return -1;
        }
        return iterator.next().byteValue();
      }
    };
    PipedOutputStream shOutputStream = new PipedOutputStream();
    PipedInputStream sedInputStream = new PipedInputStream(shOutputStream);

    ProcessExecutor shExecutor = new ProcessExecutor("sh", "-c",
        Stream.of("cat", "exit 79")
        .collect(Collectors.joining("\n")))
        .exitValue(79)
        .redirectInput(shInputStream)
        .redirectOutput(shOutputStream);
    PipedOutputStream sedOutputStream = new PipedOutputStream();
    PipedInputStream sedInputStreamForOutput = new PipedInputStream(sedOutputStream);
    InputStream endProtectedInputStream = new FilterInputStream(sedInputStreamForOutput) {

      @Override
      public int read() throws IOException {
        return checkEndDead(() -> super.read());
      }

      @Override
      public int read(byte[] b) throws IOException {
        return checkEndDead(() -> super.read(b));
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return checkEndDead(() -> super.read(b, off, len));
      }

      private int checkEndDead(Callable<Integer> readCall) throws IOException {
        try {
          return readCall.call();
        } catch (IOException ex) {
          if (ex.getMessage().equals("Write end dead")) {
            return -1;
          }
          throw ex;
        } catch (RuntimeException ex) {
          throw ex;
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    };
    ProcessExecutor sedExecutor = new ProcessExecutor("sed", "s/world/process/")
        .redirectInput(sedInputStream)
        .redirectOutput(sedOutputStream);

    StartedProcess sh = shExecutor
        .start();
    StartedProcess sed = sedExecutor
        .start();

    try (BufferedReader sedReader = new BufferedReader(
        new InputStreamReader(endProtectedInputStream, StandardCharsets.UTF_8))) {
      sedReader.lines()
          .peek(System.out::println)
          .collect(Collectors.toList());
      ProcessResult shResult = sh.getFuture().get();
      ProcessResult sedResult = sed.getFuture().get();
      if (shResult.getExitValue() != 79) {
        throw new Exception(
            "Process " + shExecutor.getCommand()
            + " has failed with exit code " + shResult.getExitValue());
      }
      if (sedResult.getExitValue() != 0) {
        throw new Exception(
            "Process " + sedExecutor.getCommand()
            + " has failed with exit code " + sedResult.getExitValue());
      }
    }
```

- JProc

The code has improved a lot with this library. We still miss some utility to transform a Stream
into an `InputStream` and back. Also, there's some need of a mechanism to extract the collection
to the main thread (in this case using the Completable Future Java API).

```java
    Stream<String> inputStream = Stream.of(
        "hello",
        "world"
        );
    InputStream shInputStream = new InputStream() {
      final Iterator<Byte> iterator = inputStream
          .map(line -> (line + "\n").getBytes(StandardCharsets.UTF_8))
          .flatMap(bytes -> {
            List<Byte> byteList = new ArrayList<>(bytes.length);
            for (byte value : bytes) {
              byteList.add(value);
            }
            return byteList.stream();
          })
          .iterator();
      @Override
      public int read() throws IOException {
        if (!iterator.hasNext()) {
          return -1;
        }
        return iterator.next().byteValue();
      }
    };
    CompletableFuture<List<String>> futureOutput = new CompletableFuture<>();
    ProcResult shResult = new ProcBuilder("sh", "-c",
        Stream.of("cat", "exit 79")
        .collect(Collectors.joining("\n")))
        .withExpectedExitStatuses(79)
        .withInputStream(shInputStream)
        .withOutputConsumer(sedInputStream -> {
          ProcResult sedResult = new ProcBuilder("sed", "s/world/process/")
              .withInputStream(sedInputStream)
              .withOutputConsumer(sedInputStreamForOutput -> {
                try (BufferedReader sedReader = new BufferedReader(
                    new InputStreamReader(sedInputStreamForOutput, StandardCharsets.UTF_8))) {
                  futureOutput.complete(sedReader.lines()
                      .peek(System.out::println)
                      .collect(Collectors.toList()));
                } catch (Exception ex) {
                  futureOutput.completeExceptionally(ex);
                }
              })
              .run();
        })
        .run();
    futureOutput.join();
```

- Fluent Process

This is still easy with Fluent Process. Thanks to the fluent API of this library we just need to
add 6 lines to the previous code. And since it already allows to work with streams, the
code is very clear compared to the other libraries.

```java
FluentProcess.builder("sh").arg("-c")
    .multilineArg("cat", "exit 79")
    .allowedExitCodes(Arrays.asList(79))
    .start()
    .inputStream(Stream.of("hello", "world"))
    .pipe("sed", "s/world/process/")
    .stream()
    .peek(System.out::println)
    .collect(Collectors.toList());
```

## Conclusions

If you need to support Java 1.3+ or Java 1.5+ [Apache Commons Exec](http://commons.apache.org/proper/commons-exec/)
and [ZT Process Executor](https://github.com/zeroturnaround/zt-exec) are well tested alternatives
that will make your code more readable.

[JProc](https://github.com/fleipold/jproc) offer a fleunt API for those of you who are on Java 1.7+
and can benefit Java Lambdas.

Finally, if you are on Java 1.8+ and want to benefit of the new Java APIs, you need a lightweight
approach or you simply want simpler code, [Fluent Process](https://gitlab.com/ongresinc/fluent-process)
is here to help be more productive allowing yuo to write less and do stuffs faster. Fluent Process
is open source created at StackGres. If you like it, please star it and share the project!

You can find the source code of this blog post code examples here: https://gitlab.com/ongresinc/blog-posts-src/-/tree/master/20200820-java_process_and_streams/examples
