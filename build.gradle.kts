import com.google.protobuf.gradle.id

plugins {
    java
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.5"
}

group = "com.troica.msa"
version = providers.gradleProperty("version").get()

object Versions {
    const val GRPC = "1.75.0"
    const val GRPC_KOTLIN = "1.4.1"
    const val PROTOBUF = "4.34.1"
    const val JWT = "0.12.6"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "GitHubPackagesCommonLibs"
        url = uri("https://maven.pkg.github.com/KTCloud-CloudNative-Troica-Team/msa-common-libs")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
                ?: providers.gradleProperty("gpr.user").orNull
            password = System.getenv("GITHUB_TOKEN")
                ?: providers.gradleProperty("gpr.token").orNull
        }
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    jvmToolchain(21)
}

dependencyManagement {
    imports {
        // Spring Boot 3.5.14 BOM
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.14")
        // Spring Cloud 2025.0.2 (Northfields) — SB 3.5.x 공식 짝, Q1 (c) 결정 BOM
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.2")
    }
}

dependencies {
    implementation("com.troica.msa:common:0.3.1")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Spring Cloud Gateway (WebFlux) — Q1 (c) 혼합 패턴.
    // 2025.0.x에서 `spring-cloud-gateway-server-webflux` 명시 아티팩트 권장.
    // BFF REST controller (gRPC client→) + Gateway routes (reverse proxy) 둘 다 한 deployable.
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // R-50: SC Gateway RequestRateLimiter filter는 Reactive Redis로 Token Bucket을 구현함.
    // Phase 5 Redis Cluster 배포 전까지는 dev 단계 localhost:6379 또는 컨테이너로 검증함.
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // gRPC client (auth-service / order / product / inventory 호출)
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
    implementation("com.google.protobuf:protobuf-java:${Versions.PROTOBUF}")
    implementation("com.google.protobuf:protobuf-kotlin:${Versions.PROTOBUF}")
    implementation("io.grpc:grpc-protobuf:${Versions.GRPC}")
    implementation("io.grpc:grpc-stub:${Versions.GRPC}")
    implementation("io.grpc:grpc-kotlin-stub:${Versions.GRPC_KOTLIN}")
    implementation("io.grpc:grpc-netty-shaded:${Versions.GRPC}")

    // Netty CVE override — webflux/reactor-netty가 netty-codec-* 4.1.132.Final을 가져오나
    // 다음 CVE들이 4.1.133.Final에서 fix됨 (SB 3.5.14 BOM은 아직 4.1.132 그대로):
    //   CVE-2026-42583 (netty-codec Lz4FrameDecoder resource exhaustion)
    //   CVE-2026-42579 (netty-codec-dns input validation bypass)
    //   CVE-2026-42584/42587 (netty-codec-http response desync + decompression bypass)
    //   CVE-2026-42577 (netty-transport-native-epoll DoS via RST)
    // grpc-netty-shaded는 shaded라 별개 — 영향 없음.
    implementation("io.netty:netty-codec:4.1.133.Final")
    implementation("io.netty:netty-codec-dns:4.1.133.Final")
    implementation("io.netty:netty-codec-http:4.1.133.Final")
    implementation("io.netty:netty-codec-http2:4.1.133.Final")
    implementation("io.netty:netty-transport-native-epoll:4.1.133.Final")

    // BouncyCastle CVE override — Spring Security가 1.80을 transitively 가져옴.
    //   CVE-2026-5598 (private key leakage via non-constant-time comparisons) fix in 1.84.
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")

    // JWT — JwtHeaderCheckFilter가 토큰 parse용 사용 (검증은 auth-service gRPC CheckValidity)
    implementation("io.jsonwebtoken:jjwt-api:${Versions.JWT}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.JWT}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.JWT}")

    // springdoc — swagger UI (springdoc-openapi 2.5.0은 SB 3.5와 호환)
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")
}

sourceSets {
    main {
        java.srcDirs(
            "build/generated/source/proto/main/java",
            "build/generated/source/proto/main/kotlin",
        )
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.PROTOBUF}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.GRPC}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.GRPC_KOTLIN}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("kotlin")
            }
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

springBoot {
    mainClass.set("dev.ktcloud.black.user.api.gateway.UserApiGatewayApplicationKt")
}
