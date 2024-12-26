# PubSub

![Banner][banner]

<div align="center">

  [![Modrinth][badge-modrinth]][modrinth]
  [![License][badge-license]][license]
  <br>

  [![Discord][badge-discord]][social-discord]
  [![Follow @simplecloudapp][badge-x]][social-x]
  [![Follow @simplecloudapp][badge-bluesky]][social-bluesky]
  [![Follow @simplecloudapp][badge-youtube]][social-youtube]
  <br>

  [Report a Bug][issue-bug-report]
  ·
  [Request a Feature][issue-feature-request]
  <br>

🌟 Give us a star — your support means the world to us!
</div>
<br>

> All information about this project can be found in our detailed [documentation][docs-thisproject].

SimpleCloud PubSub is a lightweight, high-performance publish-subscribe messaging system built on gRPC. It provides reliable message delivery and automatic reconnection handling for distributed systems.

## Features

- [x] **gRPC-based Communication**: High-performance message delivery using Protocol Buffers
- [x] **Automatic Reconnection**: Built-in reconnection handling for network interruptions
- [x] **Type-Safe Messages**: Strongly typed message handling with Protocol Buffers
- [x] **Concurrent Processing**: Thread-safe message publishing and subscription
- [x] **Connection Management**: Flexible connection configuration with optional credentials
- [x] **Flow-based API**: Modern Kotlin Flow-based subscription handling
- [x] **Error Handling**: Comprehensive error handling and recovery mechanisms

## Dependency

> For always up-to-date artifacts visit [dev artifacts][dev-artifacts] or [artifacts][artifacts].

> Note: If you want to use the dev version, you have to use the [snapshot repository][snapshots].

### Gradle Kotlin DSL
```kotlin
repositories {
    mavenCentral()
    maven("https://buf.build/gen/maven")
}

dependencies {
    implementation("app.simplecloud:pubsub:VERSION")
}
```

### Gradle Groovy
```groovy
repositories {
    mavenCentral()
    maven { url 'https://buf.build/gen/maven' }
}

dependencies {
    implementation 'app.simplecloud:pubsub:VERSION'
}
```

### Maven
```xml
<repositories>
    <repository>
        <id>buf-build</id>
        <url>https://buf.build/gen/maven</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>app.simplecloud</groupId>
        <artifactId>pubsub</artifactId>
        <version>VERSION</version>
    </dependency>
```

## Contributing
Contributions to SimpleCloud are welcome and highly appreciated. However, before you jump right into it, we would like you to read our [Contribution Guide][docs-contribute].

## License
This repository is licensed under [Apache 2.0][license].

<!-- LINK GROUP -->

<!-- ✅ PLEASE EDIT -->
[banner]: banner/pubsub/pubsub.png
[issue-bug-report]: https://github.com/theSimpleCloud/pubsub/issues/new?labels=bug&projects=template=01_BUG-REPORT.yml&title=%5BBUG%5D+%3Ctitle%3E
[issue-feature-request]: https://github.com/theSimpleCloud/pubsub/discussions/new?category=ideas
[docs-thisproject]: https://docs.simplecloud.app/api/pubsub
[docs-contribute]: https://docs.simplecloud.app/contribute

[modrinth]: https://modrinth.com/organization/simplecloud

[maven-central]: https://central.sonatype.com/artifact/app.simplecloud.controller/controller-api
[dev]: https://repo.simplecloud.app/#/snapshots/app/simplecloud/droplet/api/droplet-api

[artifacts]: https://repo.simplecloud.app/#/snapshots/app/simplecloud/droplet/api/droplet-api
[dev-artifacts]: https://repo.simplecloud.app/#/snapshots/app/simplecloud/droplet/api/droplet-api
[badge-maven-central]: https://img.shields.io/maven-central/v/app.simplecloud/simplecloud-pubsub?labelColor=18181b&style=flat-square&color=65a30d&label=Release
[badge-dev]: https://repo.simplecloud.app/api/badge/latest/snapshots/app/simplecloud/simplecloud-pubsub?name=Dev&style=flat-square&color=0ea5e9

<!-- ⛔ DON'T TOUCH -->
[license]: https://opensource.org/licenses/Apache-2.0
[snapshots]: https://repo.simplecloud.app/#/snapshots

[social-x]: https://x.com/simplecloudapp
[social-bluesky]: https://bsky.app/profile/simplecloud.app
[social-youtube]: https://www.youtube.com/@thesimplecloud9075
[social-discord]: https://discord.simplecloud.app

[badge-modrinth]: https://img.shields.io/badge/modrinth-18181b.svg?style=flat-square&logo=modrinth
[badge-license]: https://img.shields.io/badge/apache%202.0-blue.svg?style=flat-square&label=license&labelColor=18181b&style=flat-square&color=e11d48
[badge-discord]: https://img.shields.io/badge/Community_Discord-d95652.svg?style=flat-square&logo=discord&color=27272a
[badge-x]: https://img.shields.io/badge/Follow_@simplecloudapp-d95652.svg?style=flat-square&logo=x&color=27272a
[badge-bluesky]: https://img.shields.io/badge/Follow_@simplecloud.app-d95652.svg?style=flat-square&logo=bluesky&color=27272a
[badge-youtube]: https://img.shields.io/badge/youtube-d95652.svg?style=flat-square&logo=youtube&color=27272a
