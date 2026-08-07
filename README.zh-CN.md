<a id="readme-top"></a>

<div align="center">

# xmemcached-extension

**xmemcached 的纯 Java 扩展库**

[![License](https://img.shields.io/badge/license-Apache-2.0-green)](https://www.apache.org/licenses/LICENSE-2.0)

[简体中文](./README.zh-CN.md) | [English](./README.md)

[定位](#1-定位) · [核心能力](#2-核心能力) ·
[依赖](#5-依赖) · [快速开始](#6-快速开始) ·
[构建](#7-构建与测试) · [许可证](#9-许可证)

</div>

---

> **当前版本**：`2.0.1-SNAPSHOT`<br>
> **JDK 基线**：`17`<br>
> **Group ID**：`io.github.easy4j`<br>
> **Artifact ID**：`xmemcached-extension`<br>
> **许可证**：Apache License 2.0<br>

## 1. 定位

**xmemcached-extension** 是一个面向 **xmemcached** 的纯 Java 扩展库。它把
`xmemcached-spring-boot-starter` 中所有与 Spring 无关的代码抽取为一个独立的工件,
让同一套运行时代码可被两类消费者复用:

- **Spring Boot starter**:在它之上叠加自动装配。
- **非 Spring 应用或框架**:直接使用操作模板、Key 构建器、Transcoder 与 GEO 工具,
  无需引入任何 Spring 依赖。

| 维度 | 描述 |
|---|---|
| 类型 | 纯 Java 库 |
| Spring 依赖 | **无**(构建期强制校验) |
| 消费者 | `xmemcached-spring-boot-starter` 以及任何使用 xmemcached 的非 Spring 应用 |
| JDK | `17` |
| Maven 坐标 | `io.github.easy4j:xmemcached-extension:2.0.1-SNAPSHOT` |

## 2. 核心能力

| 能力 | 状态 | 描述 |
|---|:---:|---|
| `XmemcachedOperationTemplate` | ✅ 稳定 | 面向 `XMemcachedClient` 的高层缓存操作封装(get/set/cas/incr/decr/del/批量) |
| `XmemcachedKey` / `XmemcachedKeyConstant` | ✅ 稳定 | 类型化的缓存 Key 枚举 + 常量容器 |
| `AuthInfoProvider` | ✅ 稳定 | 为底层 xmemcached 客户端提供 SASL 认证信息的 SPI |
| `XMemcachedOperationException` | ✅ 稳定 | 操作模板使用的运行时异常 |
| 自定义 Transcoder(`BooleanTranscoder`、`CustomTypeTranscoder`) | ✅ 稳定 | 与 xmemcached SDK 同包(`net.rubyeye.xmemcached.transcoders`),保留对 SDK 包内字段的访问 |
| `GeoTemplate` | ✅ 稳定 | 基于 `org.gavaghan:geodesy` 的大圆距离 / 椭球距离计算 |
| `Strings` | ✅ 稳定 | 本地实现的 `hasText` 工具,等价于 `org.springframework.util.StringUtils#hasText`,但零 Spring 依赖 |

## 3. 依赖与兼容性

| 依赖 | 最低版本 | 依据 |
|---|---:|---|
| JDK | `17` | `pom.xml`(继承自 `spring-boot-starter-parent` 2.6.0) |
| Maven | `3.6+` | Maven Enforcer |
| xmemcached | `2.4.7` | `pom.xml` |
| geodesy | `1.1.3` | `pom.xml`(`GeoTemplate` 使用) |

## 4. 零 Spring 依赖保证

本模块 **不允许** 依赖 Spring。该约束在构建期由 `maven-enforcer-plugin` 的
`ban-spring-dependencies` 规则强制执行:任何引入 `org.springframework.*` 或
`org.springframework.boot.*` 的传递依赖都会导致构建失败。

## 5. 依赖

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>xmemcached-extension</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

运行时只需 `xmemcached-extension`,它会传递引入 `xmemcached`、`geodesy` 和 `slf4j-api`。

## 6. 快速开始

### 6.1 手动构建客户端与模板(无 Spring)

```java
XMemcachedClient client = new XMemcachedClientBuilder(
        AddrUtil.getAddresses("127.0.0.1:11211")).build();

XmemcachedOperationTemplate template =
        new XmemcachedOperationTemplate(client, Duration.ofSeconds(5));

template.set("user:42:name", "wandl");
String name = template.getString("user:42:name");
```

### 6.2 使用类型化 Key 构建器

```java
String fullKey = XmemcachedKey.USER_GEO_LOCATION.getKey();
String userKey = XmemcachedKey.USER_GEO_LOCATION.getKey(42L);
```

### 6.3 使用 GEO 工具

```java
GeoTemplate geo = new GeoTemplate();
double meters = geo.getWGS84Distance(39.9, 116.4, 31.2, 121.5);
```

### 6.4 在 Spring Boot starter 中使用

`xmemcached-spring-boot-starter` 通过依赖传递引入本模块,使用 starter 时无需单独添加。

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>xmemcached-spring-boot-starter</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## 7. 构建与测试

```bash
mvn clean install
mvn -pl xmemcached-extension -am test
```

## 8. 贡献

1. Fork 仓库。
2. 新建特性分支。
3. 提交前执行 `mvn clean verify`。
4. 提交 Pull Request。

## 9. 许可证

本项目基于 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源。

---

<div align="center">

[回到顶部](#readme-top) · [Issues](https://github.com/easy-4-java/xmemcached-extension/issues) · [仓库](https://github.com/easy-4-java/xmemcached-extension)

</div>