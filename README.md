<a id="readme-top"></a>

<div align="center">

# xmemcached-extension

**Pure-Java extension library for xmemcached**

[![License](https://img.shields.io/badge/license-Apache-2.0-green)](https://www.apache.org/licenses/LICENSE-2.0)

[简体中文](./README.zh-CN.md) | [English](./README.md)

[Positioning](#1-positioning) · [Capabilities](#2-core-capabilities) ·
[Dependency](#5-dependency) · [Quick Start](#6-quick-start) ·
[Build](#7-build-and-test) · [License](#9-license)

</div>

---

> **Current Version**：`2.0.1-SNAPSHOT`<br>
> **JDK Baseline**：`17`<br>
> **Group ID**：`io.github.easy4j`<br>
> **Artifact ID**：`xmemcached-extension`<br>
> **License**：Apache License 2.0<br>

## 1. Positioning

**xmemcached-extension** is a pure-Java extension library for applications that use **xmemcached**.
It extracts every line of non-Spring logic out of `xmemcached-spring-boot-starter` into a standalone
artifact so that the same runtime code can be reused by:

- the Spring Boot starter (which adds auto-configuration on top), and
- non-Spring applications or frameworks that want the operation template, key builders, transcoders
  and geo utilities without pulling in Spring.

| Dimension | Description |
|---|---|
| Type | Pure-Java library |
| Spring Dependency | **None** (enforced at build time) |
| Consumers | xmemcached-spring-boot-starter, and any non-Spring app using xmemcached |
| JDK | `17` |
| Coordinates | `io.github.easy4j:xmemcached-extension:2.0.1-SNAPSHOT` |

## 2. Core Capabilities

| Capability | Status | Description |
|---|:---:|---|
| `XmemcachedOperationTemplate` | ✅ Stable | High-level cache operation wrapper (get/set/cas/incr/decr/del/batch) over `XMemcachedClient` |
| `XmemcachedKey` / `XmemcachedKeyConstant` | ✅ Stable | Cache key builders with a typed enum and constant holder |
| `AuthInfoProvider` | ✅ Stable | SPI for supplying SASL auth info to the underlying xmemcached client |
| `XMemcachedOperationException` | ✅ Stable | Runtime exception used by the operation template |
| Transcoders (`BooleanTranscoder`, `CustomTypeTranscoder`) | ✅ Stable | Drop-in transcoders that live in the `net.rubyeye.xmemcached.transcoders` package so they retain package-private access to xmemcached SDK internals |
| `GeoTemplate` | ✅ Stable | Great-circle / ellipsoidal distance calculations backed by `org.gavaghan:geodesy` |
| `Strings` | ✅ Stable | Tiny local `hasText` helper (Spring-free replacement for `org.springframework.util.StringUtils#hasText`) |

## 3. Requirements and Compatibility

| Dependency | Minimum | Evidence |
|---|---:|---|
| JDK | `17` | `pom.xml` (`spring-boot-starter-parent` 2.6.0 baseline) |
| Maven | `3.6+` | Maven Enforcer |
| xmemcached | `2.4.7` | `pom.xml` |
| geodesy | `1.1.3` | `pom.xml` (used by `GeoTemplate`) |

## 4. Spring-Free Invariant

This module **must not** depend on Spring. The invariant is enforced at build time by the
`maven-enforcer-plugin` `ban-spring-dependencies` rule. Any attempt to introduce a transitive
`org.springframework.*` or `org.springframework.boot.*` dependency will fail the build.

## 5. Dependency

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>xmemcached-extension</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

If you only need the runtime classes (no logging, no testing), `xmemcached-extension` brings
`xmemcached`, `geodesy` and `slf4j-api` transitively.

## 6. Quick Start

### 6.1 Build a client and a template by hand (no Spring)

```java
XMemcachedClient client = new XMemcachedClientBuilder(
        AddrUtil.getAddresses("127.0.0.1:11211")).build();

XmemcachedOperationTemplate template =
        new XmemcachedOperationTemplate(client, Duration.ofSeconds(5));

template.set("user:42:name", "wandl");
String name = template.getString("user:42:name");
```

### 6.2 Use the typed key builder

```java
String fullKey = XmemcachedKey.USER_GEO_LOCATION.getKey();
String userKey = XmemcachedKey.USER_GEO_LOCATION.getKey(42L);
```

### 6.3 Use the geo template

```java
GeoTemplate geo = new GeoTemplate();
double meters = geo.getWGS84Distance(39.9, 116.4, 31.2, 121.5);
```

### 6.4 From the Spring Boot starter

`xmemcached-spring-boot-starter` depends on this module transitively. You do not need to add
`xmemcached-extension` directly when using the starter.

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>xmemcached-spring-boot-starter</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## 7. Build and Test

```bash
mvn clean install
mvn -pl xmemcached-extension -am test
```

## 8. Contribution

1. Fork the repository.
2. Create a feature branch.
3. Run `mvn clean verify` before submitting.
4. Submit a pull request.

## 9. License

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

<div align="center">

[Back to top](#readme-top) · [Issues](https://github.com/easy-4-java/xmemcached-extension/issues) · [Repository](https://github.com/easy-4-java/xmemcached-extension)

</div>