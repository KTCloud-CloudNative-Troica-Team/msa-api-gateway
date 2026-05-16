import com.google.protobuf.gradle.id

plugins {
    java
    jacoco                                                       // R-45: 커버리지 측정
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.5"

    // R-45 (평가 심화 (2)-1): SonarCloud 정적 분석 + 커버리지 게이트.
    id("org.sonarqube") version "5.1.0.4882"
}

// R-45: single module이라 root에 직접 sonar { } + jacoco task config 적용.
// multi-module 패턴 (msa-product-service 등)과 달리 subprojects 블록 없음.
sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "ktcloud-cloudnative-troica-team")
        property("sonar.projectKey", "KTCloud-CloudNative-Troica-Team_msa-api-gateway")
        // wait=false: SonarCloud 무료 plan은 프로젝트별 custom Quality Gate 적용 불가
        // (Sonar way default Coverage 80% 강제, PoC 단계에서 항상 fail). 분석은 정상 수행
        // 되고 결과는 Dashboard 에 표시되나 CI 는 결과를 기다리지 않고 통과시킴. 단위
        // 테스트 정착 + paid plan 전환 시 true 로 복구.
        property("sonar.qualitygate.wait", "false")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        // Protobuf 코드젠 + build 산출물은 분석 제외
        property("sonar.exclusions", "**/generated/**, **/build/**, src/main/proto/**")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.matching { it.name == "jacocoTestReport" })
}
tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

group = "com.troica.msa"
version = providers.gradleProperty("version").get()

object Versions {
    const val GRPC = "1.75.0"
    const val GRPC_KOTLIN = "1.4.1"
    const val PROTOBUF = "4.34.1"
    const val JWT = "0.12.6"
    const val RESILIENCE4J = "2.2.0"   // R-41: Spring Boot 3.5 호환 최신
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
    // 0.3.1 → 0.4.0: common-libs 의 spring-boot-starter-web (servlet 전체) 가
    // transitive 로 흘러들어와 SCG validation fail. 0.4.0 부터 spring-web 만 (servlet
    // 없음) → reactive 호환. msa-common-libs PR #8 / tag v0.4.0 참조.
    implementation("com.troica.msa:common:0.4.0")

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

    // R-41 (평가 기본 (2)-4 + 선택): Resilience4j Circuit Breaker.
    // - resilience4j-spring-boot3: auto-config (Actuator endpoint + registry Bean 주입)
    // - resilience4j-kotlin: suspend 함수에 executeSuspendFunction 확장 제공
    // - resilience4j-reactor: Mono/Flux 어댑터 (BFF 외 reactive 호출용)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${Versions.RESILIENCE4J}")
    implementation("io.github.resilience4j:resilience4j-kotlin:${Versions.RESILIENCE4J}")
    implementation("io.github.resilience4j:resilience4j-reactor:${Versions.RESILIENCE4J}")

    // R-57: 단위 테스트 — JUnit 5 + AssertJ + Mockito (starter-test) + MockK (suspend 함수 mock 친화) + coroutines-test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // Gradle 8.x + JUnit Platform 1.12+ 에서 launcher 명시 필요 (OutputDirectoryProvider)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
