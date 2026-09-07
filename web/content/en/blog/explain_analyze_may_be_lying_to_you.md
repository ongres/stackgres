+++
title = "EXPLAIN ANALYZE may be lying to you"
date = 2020-05-26T12:50:46+02:00
author = ["aht"]
tags = ["postgresql", "performance"]
planet = "aht"
draft = false
readtime = "15 min"
thumbnail = "img/blog/explain_analyze-cover.jpg"
description = "EXPLAIN ANALYZE timings can be distorted by the observer effect: how measurement overhead skews Postgres query analysis and how to account for it."
+++


## The Observer effect

> In physics, the observer effect is the theory that the mere observation of a
  phenomenon inevitably changes that phenomenon. This is often the result of
  instruments that, by necessity, alter the state of what they measure in some
  manner.\
  \
  [Observer effect, Wikipedia](https://en.wikipedia.org/wiki/Observer_effect_(physics)) \
  \
  (_edit; this previously referred to the
  [Uncertainty principle](https://en.wikipedia.org/wiki/Uncertainty_principle)_)


In layman's terms, what the Observer effect states is that by measuring a
property of a system, you may be altering that system itself: your observation
becomes a distorted version of the reality.

In most cases, this distortion is negligible and we can simply ignore it. If
we use a thermometer to measure someone's temperature, some heat will be
transferred from the person to the termometer, effectively lowering the person's
temperature. But it should not be noticeable, and well below the error margin of
the thermometer.

But what happens when the measurement may not just affect, but rather completely
ruin the measurement?


## Where the potential lie is

You are probably resorting a lot to use Postgres'
[`EXPLAIN ANALYZE`](https://www.postgresql.org/docs/current/sql-explain.html)
command when you want to optimize a query's performance. You probably look at
the query nodes, see which ones have the highest execution time and then try to
optimize them. The costlier the node is, the biggest return of investment you
get if you can optimize it. Obviously, a query optimization may change the query
plan altogether, but you get the point: you want to know where most of the query
execution time is going.

Now grab your favorite Postgres and run the following commands:

```sql
create table i1 as select i from generate_series(1,1000*1000) as i;
create table i2 as select i from generate_series(1,1000) as i;
analyze;
explain analyze select sum(i1.i * i2.i) from i1 inner join i2 using (i);
```

***
Note that the first `analyze` command is not related at all with the `explain
analyze` command that follows it.
***

Run the query. Note the time reported by `explain analyze`. Now run the query
again and note the execution time without `explain analyze`. You can do this,
for example, by:

* Running from `psql` and using both `\timing` and `\o /dev/null`.
* Using [`pg_stat_statements`](https://www.postgresql.org/docs/12/pgstatstatements.html).

The latter is a better method, as the former includes client round-trip time and processing. But
this overhead should be negligible for this case.

Do you see anything wrong? The execution time as reported by `EXPLAIN ANALYZE`
is substantially higher than that of the real query execution time. On my
system, running 20 times after another 20 times of warmup:

| query                                                                   | calls |      total       |       mean       |       min        |       max        |      stddev      |
|:------------------------------------------------------------------------|------:|-----------------:|-----------------:|-----------------:|-----------------:|-----------------:|
| explain analyze select sum(i1.i * i2.i) from i1 inner join i2 using (i) | 20    | 917.20           | 45.86            | 45.32            | 49.24            | 0.84             |
| select sum(i1.i * i2.i) from i1 inner join i2 using (i)                 | 20    | 615.73           | 30.79            | 30.06            | 34.48            | 0.92             |


That's about a 50% overhead! As we can see here, the measurement is
significantly altering the observed fact. But it can get much worse. For instance,
on a virtual instance running on a non
[Nitro](https://aws.amazon.com/ec2/nitro/) EC2 instance (`r4.large`):

| query                                                                   | calls |      total       |       mean       |       min        |       max        |      stddev      |
|:------------------------------------------------------------------------|------:|-----------------:|-----------------:|-----------------:|-----------------:|-----------------:|
| explain analyze select sum(i1.i * i2.i) from i1 inner join i2 using (i) | 20    | 21080.18         | 1054.01          | 1053.36          | 1055.96          | 0.55             |
| select sum(i1.i * i2.i) from i1 inner join i2 using (i)                 | 20    | 2596.85          | 129.84           | 129.33           | 130.45           | 0.28             |

Here `EXPLAIN ANALYZE` got 8 times slower, a 700% overhead!


![EXPLAIN ANALYZE might be lying to you](../../img/blog/explain_analyze.jpg)


Astute readers may realize that this effect is related to the system clock.
Non Nitro instances are virtualized with Xen, which exposes a `xen` virtualized
clock to the VMs. On Nitro instances and other virtualized environments where
KVM is used, clock is as fast as the hypervisor's, and results are similar to
the first ones shown here. We may also mitigate this effort on `r4.large` by
switching to the `tsc` time source:

```sh
echo tsc | sudo tee -a /sys/devices/system/clocksource/clocksource0/current_clocksource
```


| query                                                                   | calls |      total       |       mean       |       min        |       max        |      stddev      |
|:------------------------------------------------------------------------|------:|-----------------:|-----------------:|-----------------:|-----------------:|-----------------:|
| explain analyze select sum(i1.i * i2.i) from i1 inner join i2 using (i) | 20    | 3747.07          | 187.37           | 187.12           | 187.56           | 0.12             |
| select sum(i1.i * i2.i) from i1 inner join i2 using (i)                 | 20    | 2602.45          | 130.12           | 129.88           | 130.77           | 0.21             |


Also note that results will change if you configure differently 
max_parallel_workers_per_gather as these results are affected by the level of
parallelism used.


## The good news

You shouldn't be surprised, however. This behavior is known and documented. As
usual, Postgres documentation is as complete as it can be:

> The measurement overhead added by EXPLAIN ANALYZE can be significant,
  especially on machines with slow gettimeofday() operating-system calls. You
  can use the pg_test_timing tool to measure the overhead of timing on your
  system.\
  \
  [EXPLAIN caveats](https://www.postgresql.org/docs/current/using-explain.html#USING-EXPLAIN-CAVEATS)

However, I have found that many users and DBAs are either unaware of this effect
or not aware of how significant it may be. This post is my humble contribution
to make this effect more widely unserstood.


## The Volcano

Why is this happening, after all? Postgres, like other OLTP databases, follows a
query execution model named
[the Volcano model](https://paperhub.s3.amazonaws.com/dace52a42c07f7f8348b08dc2b186061.pdf).
Under this model, also known as one-row-at-a-time, each node of the query
execution tree contains code to process rows one by one. Instead of every node
gathering all the rows belonging to it before combining with the next node, as
soon as a row is gathered at one node, it is processed through the rest of the
tree. This makes sense --gathering all the rows at a given node may require to
hold all that data in memory, which could be impossible--, but it introduced the
`EXPLAIN ANALYZE` problem described here.

> The executor processes a tree of "plan nodes". The plan tree is essentially
  a demand-pull pipeline of tuple processing operations. Each node, when
  called, will produce the next tuple in its output sequence, or NULL if no
  more tuples are available. If the node is not a primitive relation-scanning
  node, it will have child node(s) that it calls in turn to obtain input
  tuples.\
  \
  [src/backend/executor/README](https://github.com/postgres/postgres/blob/master/src/backend/executor/README#L6)

So we can already explain exactly where the overhead comes from: in order to
measure the execution time of a given node, as shown by `explain analyze`, you
need to measure the execution time on a per-row basis, and then aggregate them
per node, to obtain the total execution time per node.

Since rows are not executed one after the other (since a row may be processed by
other nodes first), you basically need to get the system time before and after
processing every row. In other words: you are calling the system twice per row.
On a node that processes millions of rows, you are then calling the system time
millions of times.

But how cheap (or expensive) is it to call the system clock? In Postgres, this
is implemented in the
[`elapsed_time`](https://github.com/postgres/postgres/blob/d9101e9806e446a413bcef16d3ab65602ed028ad/src/backend/commands/explain.c#L1028)
function, which in turn relies on the
`INSTR_TIME` macros defined in
[instr_time.h](https://github.com/postgres/postgres/blob/7559d8ebfa11d98728e816f6b655582ce41150f3/src/include/portability/instr_time.h).
Which calls the system call `clock_gettime`, a fast system call on most systems.
In particular, on Linux, is typically implemented as a
[VDSO](https://en.wikipedia.org/wiki/VDSO), meaning that there's no context
switch between user and kernel space, making the call significantly faster.

But again, how fast is "fast", if we might be calling this millions of times?
Again, [Postgres
documentation](https://www.postgresql.org/docs/12/pgtesttiming.html) comes to
the rescue, as there's a binary included in Postgres to precisely do this,
`pg_test_timing`. Indeed, it has a [documentation
section](https://www.postgresql.org/docs/12/pgtesttiming.html#id-1.9.5.11.7.3)
explaining how to use it to measure the `EXPLAIN ANALYZE` overhead.

On one of the systems used for the measurements above, it reports:

```
Testing timing overhead for 3 seconds.
Per loop time including overhead: 4620,82 ns
Histogram of timing durations:
  < us   % of total      count
     1      0,00000          0
     2      0,00000          0
     4      0,00000          0
     8     99,85491     648295
    16      0,01586        103
    32      0,12060        783
    64      0,00863         56
```


Basically, the overhead is for most cases around 5 micro seconds. That time
multiplied by millions means seconds or dozens of seconds of overhead. I
recommend you to read [Clock sources in
Linux](http://btorpey.github.io/blog/2014/02/18/clock-sources-in-linux/) if you
want to dive deeper into the topic.


## The not-that-good news

Let's go back to our goal of using the execution timing information to see how
we can optimize a query. If profiling overhead is substantial, but it is
proportional to real execution time, it wouldn't matter much --as all query
execution times would be scaled alike, and the slowest node would remain the
slowest node. But the problem is that they aren't: some nodes suffer
significantly higher overhead, and may appear to be slower than others,
while it's not the case in reality. Unfortunately, this means that **you cannot
trust `EXPLAIN ANALYZE` to optimize your queries**.

It completely depends on the query and its execution nodes. We can use `ltrace`
to count the number of times the `clock_gettime` is called:

```sh
sudo ltrace -p $postgres_backend_pid -c -e clock_gettime
```

| query                                                                   | clock_gettime calls | parallel |
|:------------------------------------------------------------------------|--------------------:|:--------:|
| explain analyze select sum(i1.i * i2.i) from i1 inner join i2 using (i) | 2004028             | off      |
| select sum(i1.i * i2.i) from i1 inner join i2 using (i)                 | 4                   | off      |
| explain analyze select sum(i2.i) from i2                                | 2016                | off      |
| explain analyze select sum(i1.i * i2.i) from i1 inner join i2 using (i) | 38656               | on       |

Here are some examples. For the join query, we can observe that the clock is
called `2N+4M+K` times, where `N` is the number of rows of `i1`, `M` the number of
rows of `i2` and `K` is a constant factor; `28` in this case, `16` in the case
of the summing query (the third one).

It is a very interesting the case when parallel mode is activated. Here the time
is reported in blocks to some degree, significantly lowering the number of times
the clock is called.

**The overhead `EXPLAIN ANALYZE` introduces is not proportional to the real
duration of the query plan, but rather proportional to the number of rows
processed by the node**. While they may be aligned, more rows processed does not
always lead to higher execution times, and counting on this assumption may lead to
believing a node is slower when it is in fact faster than another one. In turn
leading to a bad query optimization strategy.

I have worked deeper on the topic and tried the following:

*  Take some queries and the number of calls to `clock_gettime`, as in the
   previous table, and measure the `EXPLAIN ANALYZE` execution times (without
   the additional overhead introduced by `ltrace`). Then solve the equation with
   the clock time as an unknown. However, the results vary significantly from
   query execution to query execution, and are not comparable. I have obtained
   results diverging up to one order of magnitude. Not even a linear regression
   helps here with such disparate results.

*  Try measuring the `clock_gettime` overhead with the fastest and most advanced
   perf profiler available:
   [eBPF](http://www.brendangregg.com/blog/2019-01-01/learn-ebpf-tracing.html).
   However, even then, BPF's overhead is higher than that of the
  `clock_gettime`, making it uneffective.


## How to become a bit more truthful

I guess it's not easy. The fact that parallel mode appears to call the system
time in blocks, at certain cases, could be a good way to move forward.

Another alternative would be to provide a "correction mechanism". If the clock
time can be measured precisely, and the number of times the clock is called is
known --Postgres certainly could keep track of it--, its countribution could be
substracted from the total measured time. While probably not 100% exact, it
would be much better than what it is as of today.


## Extra thoughts

In reality, `EXPLAIN ANALYZE` is a **query execution profiler**. Being aware of
this, we all know that profilers introduce more or less **profiling overhead**.
This is the key takeaway: `EXPLAIN ANALYZE` is a profiler, and its overhead
ranges from high to very/extremely high on systems with slow virtualized clocks.

***
Why `EXPLAIN` and `EXPLAIN ANALYZE` share the same "command"? They are, in
reality, two very different things: the former gives you the query execution
plan; the latter profiles the query.  While the output is similar --but only
that, similar-- they do very different things. I'd rename the latter to
`PROFILE SELECT...`.

Pretty much the same story with `VACUUM` and `VACUUM FULL`. The latter should
be renamed to `DEFRAG` or `REWRITE TABLE` --or `VACUUM FULL YES I REALLY KNOW
WHAT I AM DOING PLEASE DO LOCK MY TABLE`, for that matter.
***
