+++
title = "How to Build Postgres with Zig: A Drop-In GCC/Clang Alternative"
date = 2025-05-23T16:52:54+02:00
author = ["aht"]
tags = ["postgres", "compiler", "gcc", "clang", "zig"]
draft = false
thumbnail = "img/blog/zig+postgres.jpg"
readtime = "7 min"
planet = "aht"
description = "Building Postgres with Zig as a drop-in GCC/Clang alternative: a step-by-step guide and why Zig is interesting for Postgres development."
+++


## What's Zig and Zig's C/C++ compiler?

In case you are not familiar with it, [Zig](https://ziglang.org/) is a programming language. Among other characteristics, Zig prides itself on being a low-level / systems programming language with great [interoperability with C and C++](https://ziglang.org/documentation/0.13.0/#C). Unlike other comparable languages like [Rust](https://www.rust-lang.org/es), it does explicit memory allocation / freeing (even though it adds cool features like the <code>[defer keyword](https://ziglang.org/documentation/0.13.0/#defer)</code>), so it seems to have a mental model closer to C, in which Postgres is programmed. This also makes it a very interesting language for developing Postgres extensions in Zig, and there's the [pgzx](https://xata.io/pgzx) framework to help with that.

But other than the extensions, what's Zig bringing to Postgres and what's this post really about? Zig's compiler. It's quite an advanced piece of technology that, apart from compiling Zig code, can also compile C/C++ code, and does so really well. There's a mind-blowing [blog post](https://andrewkelley.me/post/zig-cc-powerful-drop-in-replacement-gcc-clang.html) from Andrew Kelly, creator of Zig, that I'd recommend reading, about using Zig as a C/C++ compiler, claiming it is a powerful drop-in replacement for GCC/Clang.

`zig cc`, the command line for Zig's C compiler, is included with the Zig distribution, which is by itself a self-contained, small downloadable package (41-50MiB on Linux, depending on the architecture). `zig cc` "*supports the same options as[ Clang](https://clang.llvm.org/), which, in turn, supports the same options as[ GCC](https://gcc.gnu.org/)*", **making it a drop-in replacement**. To achieve this level of compatibility, `zig cc` uses LLVM behind the scenes (it's technically a layer on top of an LLVM frontend). As a curiosity, Andrew's post details how it's possible that Zig's distribution is (significantly) smaller than even LLVM binaries!

So if it is a drop-in replacement, building Postgres with `zig cc` should be easy, right? Let's give it a try.


## Building Postgres with `zig cc`

It turns out to be quite straightforward.

First we need to download Zig. Zig is statically linked ("*Zig's Linux tarballs are fully statically linked, and therefore work correctly on all Linux distributions.*").

Download and install Zig (YMMV on different architectures):


```
$ wget https://ziglang.org/download/0.14.0/zig-linux-x86_64-0.14.0.tar.xz
$ tar xf zig-linux-x86_64-0.14.0.tar.xz -C /tmp
```


Download Postgres source code:


```
$ wget https://github.com/postgres/postgres/archive/refs/tags/REL_17_5.tar.gz
$ tar xzf REL_17_5.tar.gz -C /tmp
$ cd /tmp/postgres-REL_17_5
```


For simplicity, Postgres will be built here with the "classical" Autotools build. Meson-based build is left as an exercise. The only thing that is required here is to define the `CC` environment variable before calling `./configure`:


```
$ CC="/tmp/zig-linux-x86_64-0.14.0/zig cc" ./configure
$ make -j$(nproc)
```


Compilation succeeds, yet it produces a few warnings. They are probably caused by renaming/repurposing of some warning flags in the version of LLVM/Clang on which `zig cc` is based. While not critically important, they deserve a bit of further research.

Other than the warnings, all is good right? Well, almost. The built binaries seem to be… a bit oversized.


```
$ du -k src/backend/postgres
42748   src/backend/postgres
```


While no `-g` is being passed, and `-O2` is being passed, binaries seem to have some "bloat". This is apparently due to `zig cc` still trying to add debug symbols, which is its default. This can be prevented by adding the "`-s`" flag to the compiler, which is a [GCC compiler flag](https://gcc.gnu.org/onlinedocs/gcc/Link-Options.html#Link-Options) to "*remove all symbol table and relocation information from the executable*" (and it's compatible with Zig's C compiler). It's interesting that it's a different default, though, and I believe Zig's C compiler should probably be aligned with GCC/Clang's default behavior.

Adding this flag leads to the desired result:


```
$ make clean; CC="/tmp/zig-linux-x86_64-0.14.0/zig cc -s" ./configure
$ make -j$(nproc)
$ du -k src/backend/postgres
9212   src/backend/postgres
```


Once Postgres is built, it's easy to initialize PGDATA and run it as usual. It just works.


## Where `zig cc` shines

Zig C compiler can actually cross compile. Just by appropriately setting the target (with `-target`) you can generate binaries for other architectures, operating systems and/or different `libc` libraries and versions. For example, let's trivially compile Postgres for ARM64 on a x86_64 linux box:


```
$ make clean; CC="/tmp/zig-linux-x86_64-0.14.0/zig cc -s -target aarch64-linux-gnu" \
  ./configure --host=arm64-linux --without-readline --without-zlib --without-icu
$ make -j$(nproc)
$ file src/backend/postgres
src/backend/postgres: ELF 64-bit LSB executable, ARM aarch64, version 1 (SYSV),
dynamically linked, interpreter /lib/ld-linux-aarch64.so.1, for GNU/Linux 2.0.0, stripped
$ qemu-aarch64 -L /usr/aarch64-linux-gnu src/backend/postgres -V
postgres (PostgreSQL) 17.5
```


(Off-topic, but isn't QEMU amazing?)

What we see here is that to compile Postgres for ARM64/linux there's no need to build on an ARM64 server or to have a toolchain for cross-compilation –that's already built into `zig cc`.

You may have realized that readline, zlib and ICU are excluded from the build. It's easy to understand why: `zig cc` is able to compile for other targets, but if third party dependencies are needed, we should have them ready in the build system belonging to the appropriate target binaries are being built for. So for a more featured build of Postgres you will need to provide these libraries for the right platform (or build them with Zig cc from source!).

If you are curious, you can run `zig targets` to see all available targets. Hint: use `jq` for the output.

`zig cc` also supports musl targets, which can be used to generate static binaries. It would be awesome to generate Postgres static binaries, but [unfortunately Postgres does not support being built as a static binary](https://www.postgresql.org/message-id/10384.1639260044%40sss.pgh.pa.us).


## So what? When and where can or should I use Zig to compile Postgres?

As usual, it really depends. Probably for most people the answer will be to stick with your GCC or Clang. But `zig cc` (which anyway uses Clang) can be a quite powerful toolchain to build Postgres in some cases.

The main one is to use `zig cc` as a hermetic toolchain, given that Zig is statically compiled and self-contained. Just place the untarred Zig archive within the build environment, and you're set. And size is much smaller than GCC or Clang distributions.

Another use of `zig cc` is for building Postgres for multiple architectures. Given how easy Zig makes cross-compilation, it is ideal for this use case. You still need to bring in the architecture-dependent dependencies for Postgres, or build them (again, easily!) for the target architecture with Zig.

There is another very interesting situation in which using `zig cc` to build Postgres may be a great idea: to generate binaries linked against older versions of the glibc, or more broadly speaking, to link Postgres against a different version of the glibc than the one you have in the build system (which is the one picked when building Postgres with other systems).

If you want to distribute distribution-agnostic builds of Postgres, you probably want to target linking against a conservatively old glibc, or otherwise the Postgres compiled on one system will not run on an older one. For example this is what you will see on a Ubuntu 16.04 (which has glibc 2.23) when trying to run a Postgres built on a Ubuntu 20.04 (with glibc 2.31):


```
$ ./bin/postgres -V
./bin/postgres: /lib/x86_64-linux-gnu/libm.so.6: version `GLIBC_2.29' not found (required by ./bin/postgres)
./bin/postgres: /lib/x86_64-linux-gnu/libc.so.6: version `GLIBC_2.25' not found (required by ./bin/postgres)
```


`zig cc` has the ability to specify which version of the glibc to link. It should be as easy as this:


```
CC="/tmp/zig-linux-x86_64-0.14.0/zig cc -s -target x86_64-linux-gnu.2.23" ./configure
```


However, there's a [current bug](https://github.com/ziglang/zig/issues/22269) in `zig cc` that is preventing the `–version` flag to work correctly when you specify a version for the glibc (as in the target string above). This bug causes the Postgres configure script to fail when using this flag. It is left as an exercise to make this work once this bug is fixed (or doing it with an older version of Zig that does not exhibit this bug). Still, the potential value of this feature is clear: to be able to link Postgres against any version of the glibc, and not necessarily the one present in the build system. Which in turn is good from a hermeticity perspective (since build is not influenced by what's present in the host system in terms of glibc).


# Final thought

At the end of the day, `zig cc` just calls Clang. So the trust level that you place on binaries compiled with Zig should be at the same level of the trust placed on Clang. It's not a new compiler in the strict sense. It's "just" a very smart way to call the Clang compiler :)

