+++
title = "The SSL mode behavior in authentication-hooked extensions"
date = 2023-12-13T11:00:00+01:00
author = ["asotolongo","dbonne","3manuek"]
tags = ["postgresql", "extension","hook"]
planet = "asotolongo"
draft = false
readtime = "6 min"
thumbnail = "img/blog/ssl_ClientAuthentication-hook-v1.jpg"
description = "How the sslmode connection setting behaves with authentication-hooked Postgres extensions, and the side effects of prefer-mode retries."
+++

## Handling SSL Negotiation Failures: How ClientAuthentication Hook behaves under different scenarios

Among the [many connection parameters](https://www.postgresql.org/docs/current/libpq-connect.html#LIBPQ-PARAMKEYWORDS) that PostgreSQL supports, there’s a set of SSL-related settings (with the “ssl*” prefix) in which “sslmode” sets the negotiation priority with the server through [six modes](https://www.postgresql.org/docs/current/libpq-connect.html#LIBPQ-CONNECT-SSLMODE). 

Hooks allow inserting your code when some types of calls take place in the database. Leveraging the [ClientAuthentication_hook](https://github.com/taminomara/psql-hooks/blob/master/Detailed.md#ClientAuthentication_hook), we started to code a new extension (more news to come!) for adding a connection management feature.
 
While we were coding and testing, we detected a (not so) “weird” behavior when using the [sslmode](https://www.postgresql.org/docs/current/libpq-connect.html#LIBPQ-CONNECT-SSLMODE) in allow and prefer modes. In this blog, we will cover what was found, how to deal with extensions using this hook, and what can be done if you find yourself coding an authentication extension.

## The case of the auth_delay extension 

Just for an example case, we picked the [auth_delay](https://www.postgresql.org/docs/current/auth-delay.html) extension, which uses the [ClientAuthentication_hook](https://github.com/taminomara/psql-hooks/blob/master/Detailed.md#ClientAuthentication_hook) to implement a pause on the server before reporting an authentication failure. This is useful against brute-force attacks for DoS exploiting the connection handling. It has an `auth_delay.milliseconds` parameter that defines the number of milliseconds to be paused before the response. 

To use this extension, it is required to set the auth_delay value to shared_preload_libraries (restart required). Once these parameters are configured, we can use this extension.


```conf
# postgresql.conf
shared_preload_libraries = 'auth_delay'
auth_delay.milliseconds = 2000 # 2 seconds
ssl=on
```
In our tests, we detected an extra 2 seconds of delay. As you may see in the above settings, we set the delay to 2 seconds, but the real execution time was 4 seconds:

```bash
time PGPASSWORD='wrong' psql -U test -h localhost -d postgres
psql: error: connection to server at "localhost" (127.0.0.1), port 5432 failed: FATAL:  password authentication failed for user "test"
connection to server at "localhost" (127.0.0.1), port 5432 failed: FATAL:  password authentication failed for user "test"

real	0m4,066s <------------------------
user	0m0,044s
sys	0m0,013s
```

Digging down the logs, it is possible to see two entries reporting “password authentication failed for user”  using two distinct PIDs within a difference of  2 seconds.

```bash
2023-06-09 15:08:44.959 -04 [38921]-[2/33]-[1] test@postgres FATAL:  password authentication failed for user "test"
2023-06-09 15:08:44.959 -04 [38921]-[2/33]-[2] test@postgres DETAIL:  Connection matched pg_hba.conf line 98: "host    all             all             0.0.0.0/0            scram-sha-256"
2023-06-09 15:08:46.977 -04 [38922]-[2/34]-[1] test@postgres FATAL:  password authentication failed for user "test"
2023-06-09 15:08:46.977 -04 [38922]-[2/34]-[2] test@postgres DETAIL:  Connection matched pg_hba.conf line 98: "host    all             all             0.0.0.0/0            scram-sha-256"
```


Another thing that you may catch is that none of these matched connections against the HBA file are SSL ones. So, what is the relationship with sslmode?

In the pg_hba file documentation, it is possible to see the following details for each entry type:

| Mode  | Description  | 
|---|---|
| "host"  | is a TCP/IP socket (encrypted [ssl] or not[not ssl])
  |
| "hostssl"  | is a TCP/IP socket that is SSL-encrypted only
  |
| "hostnossl" |  is a TCP/IP socket that is not SSL-encrypted only
  | 


This means that “host” entries accept both types of connections. Our connection string matches with a host entry, and that’s when sslmode enters the scene.

The [sslmode](https://www.postgresql.org/docs/current/libpq-connect.html#LIBPQ-CONNECT-SSLMODE) has “prefer” as the default mode, which is “first try an SSL connection; if that fails, try a non-SSL connection”.  The “allow” mode has similar behavior but in a different order: “First try a non-SSL connection; if that fails, try an SSL connection”. 

So, authentication failures spawn more than one connection in these modes, even if the HBA configuration isn’t explicitly “hostssl”. As the SSL handshake failed, the next try will be a non-SSL connection. As both connection tries failed, the extension code has been executed twice.

![sslmode_prefer](../img/sslmode_prefer.png)

## Tackling authentication failures with single-try modes and HBA entries 

To demonstrate a different scenario, we set the sslmode to 'disable' or 'require' so that both use just one sort of connection.

```bash
time PGPASSWORD='wrong' psql "sslmode=disable user=report dbname=test host=localhost"
psql: error: connection to server at "localhost" (127.0.0.1), port 5432 failed: FATAL:  password authentication failed for user "report"

real	0m2,048s
user	0m0,036s
sys	0m0,011s
```

Only two seconds, and only one entry in the logs:

```
2023-06-09 15:33:37.691 -04 [39610]-[2/37]-[1] report@test FATAL:  password authentication failed for user "report"
2023-06-09 15:33:37.691 -04 [39610]-[2/37]-[2] report@test DETAIL:  Connection matched pg_hba.conf line 98: "host    all             all             0.0.0.0/0            scram-sha-256"
```

For the sslmode using "require”, the result is similar.

Now, it is possible to cap the HBA entry to hostssl or hostnossl for delimiting the authentication methods. 

The below tests are using the hostssl entry: 

```bash
time PGPASSWORD='wrong' psql -U test -h localhost -d postgres
psql: error: connection to server at "localhost" (127.0.0.1), port 5432 failed: FATAL:  password authentication failed for user "test"
connection to server at "localhost" (127.0.0.1), port 5432 failed: FATAL:  no pg_hba.conf entry for host "127.0.0.1", user "test", database "postgres", no encryption

real	0m2,051s
user	0m0,040s
sys	0m0,005s
```

The response is in the two expected seconds, and the logs show only one failed password authentication failed for user, and no entry for hostnossl connections:

```bash
2023-06-09 16:15:06.660 -04 [41089]-[2/48]-[1] test@postgres FATAL:  password authentication failed for user "test"
2023-06-09 16:15:06.660 -04 [41089]-[2/48]-[2] test@postgres DETAIL:  Connection matched pg_hba.conf line 99: "hostssl    all             all             0.0.0.0/0            scram-sha-256"
2023-06-09 16:15:06.664 -04 [41090]-[2/49]-[1] test@postgres FATAL:  no pg_hba.conf entry for host "127.0.0.1", user "test", database "postgres", no encryption
```

## Conclusions 

Either if you’re using an extension that hooks the Client Authentication or developing one that requires this mechanism, you may expect this behavior of duplicated execution if you keep the sslmode in its default mode (“prefer”) or in “allow” in authentication failures. 

Also, keeping the HBA entries in their explicit methods could improve the security settings over brute-force attacks exploiting the Postgres connection authentication.

If you know of other cases like this please share them with us, using Twitter @ongresinc.
