# msa-api-gateway

Troica Market Service의 **외부 진입점** — BFF (REST → gRPC) + Spring Cloud Gateway routes 혼합 (ADR-0005).

> SPEC + ADR ([ADR-0005](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/adr/0005-api-gateway-bff-with-cloud-gateway.md)): [msa-argocd-manifest/docs](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/tree/main/docs)
> 트러블슈팅: [TROUBLESHOOTING.md](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/TROUBLESHOOTING.md)

---

## 빠른 시작 (L2 — 로컬 docker로 끝까지 실행)

> ⚠️ 본 서비스는 **5개 백엔드 gRPC** (auth/user/product/order/inventory)에 의존. 진짜 end-to-end 실행은 모든 백엔드 서비스가 떠야. 본 README는 **gateway 자체 startup 검증**까지.

### 사전 요구사항

| 항목 | 버전 |
|---|---|
| Java | 21 (Temurin) |
| Docker | 24+ |
| GitHub PAT | `read:packages` |

### 1. GH Packages 인증 (1회)

`~/.gradle/gradle.properties`:
```
gpr.user=<github-username>
gpr.token=<PAT-with-read:packages>
```

### 2. 빌드 + 테스트

```bash
./gradlew build
```

api-gateway는 외부 DB/Kafka/Redis 없음 — gRPC client 설정만 있으면 startup 가능.

### 3. 로컬 실행 (gateway 단독, 백엔드 미연결)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

기대:
```
Started ApiGatewayApplicationKt in X seconds
Netty started on port 8100 (http)
```

⚠️ gRPC client는 lazy connect 초기에는 backend 미연결 OK. 실제 요청 시 connection refused.

### 4. 백엔드 5개와 함께 실행 (전체 end-to-end)

다른 5개 서비스가 모두 떠 있어야 진짜 작동:
- auth-service: localhost:9005
- user-service: (gRPC 미노출, REST 8004)
- product-service: localhost:9001
- order-service: localhost:9002
- inventory-service: localhost:9003

각 서비스의 README에 따라 띄운 후:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 5. 검증

```bash
# Gateway health
curl -s http://localhost:8100/healthz | jq

# BFF REST endpoint (auth-service gRPC가 떠 있어야)
curl -X POST http://localhost:8100/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# SC Gateway route (order-service admin endpoint 직통)
curl http://localhost:8100/admin/v1/orders/<order-id>/status
```

### 6. Docker로 실행

```bash
./gradlew bootJar
docker build -t msa/api-gateway:local .

docker run --rm \
  -p 8100:8100 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e GRPC_AUTH_SERVICE_ADDR=static://host.docker.internal:9005 \
  -e GRPC_PRODUCT_SERVICE_ADDR=static://host.docker.internal:9001 \
  -e GRPC_ORDER_SERVICE_ADDR=static://host.docker.internal:9002 \
  -e GRPC_INVENTORY_SERVICE_ADDR=static://host.docker.internal:9003 \
  msa/api-gateway:local
```

### 7. 정리

```bash
# 백엔드 컨테이너 (각 서비스 README의 정리 단계 참조)
docker rm -f pg-auth redis-auth pg-product pg-order kafka-shared pg-inventory redis-inventory pg-user
```

---

## 아키텍처 (ADR-0005 혼합 패턴)

```
[Client]
   ↓ HTTP
[api-gateway :8100]
   │
   ├─── BFF (Spring WebFlux + gRPC client)
   │    /api/v1/users/**         → user-service (gRPC)
   │    /api/v1/products/**      → product-service (gRPC)
   │    /api/v1/orders/**        → order-service (gRPC)
   │    /api/v1/inventory/**     → inventory-service (gRPC)
   │    /api/v1/auth/**          → auth-service (gRPC)
   │
   └─── Spring Cloud Gateway routes (reverse proxy)
        /admin/v1/orders/**      → order-service:8002 (직통)
```

JWT 검증: `JwtHeaderCheckFilter`가 모든 요청에 적용 (auth-service의 gRPC `CheckValidity` 호출).

---

## 모듈 구조 (single module)

```
msa-api-gateway/
└── src/main/
    ├── kotlin/                 # WebFlux controllers (BFF) + gRPC clients + filter
    ├── proto/                  # 자체 proto (auth/user/product/order/inventory client stubs)
    └── resources/application.yaml   # SC Gateway routes + gRPC client addresses
```

---

## 포트

| 프로토콜 | 포트 | 용도 |
|---------|------|------|
| HTTP | 8100 | 외부 진입점 (REST + SC Gateway routes) |
| gRPC | (미노출) | Gateway는 server 아닌 client만 |

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | (none) | `dev` / `prod` |
| `SERVER_PORT` | 8100 | HTTP listen |
| `GRPC_AUTH_SERVICE_ADDR` | `static://localhost:9005` | auth-service gRPC |
| `GRPC_PRODUCT_SERVICE_ADDR` | `static://localhost:9001` | product-service gRPC |
| `GRPC_ORDER_SERVICE_ADDR` | `static://localhost:9002` | order-service gRPC |
| `GRPC_INVENTORY_SERVICE_ADDR` | `static://localhost:9003` | inventory-service gRPC |
| `ROUTE_ORDER_SERVICE_URI` | `http://localhost:8002` | SC Gateway route 백엔드 |
| `JWT_SECRET` | (필요 시) | JWT 검증용 (auth-service gRPC 호출이 기본) |

---

## 외부 의존성

| 의존 | 용도 | 로컬 실행 시 |
|------|------|-------------|
| **auth-service** (gRPC :9005) | JWT `CheckValidity` | msa-auth-service 띄우기 |
| **user-service** (gRPC) | 사용자 정보 조회 | msa-user-service (현재 gRPC 미노출) |
| **product-service** (gRPC :9001) | 상품 조회/수정 | msa-product-service |
| **order-service** (gRPC :9002) | 주문 처리 | msa-order-service |
| **inventory-service** (gRPC :9003) | 재고 조회 | msa-inventory-service |
| `com.troica.msa:common:0.3.1` | 공통 예외 | GH Packages 자동 |

**DB / Kafka / Redis 미사용** — stateless gateway.

---

## CI/CD

`.github/workflows/ci.yml`: PR build-test / push main → Trivy → ECR push → manifest auto-bump.

### .trivyignore (CVE-2026-42577)

api-gateway는 webflux + reactor-netty → netty 4.1.x 직접 의존. CVE-2026-42577 (netty-transport-native-epoll DoS)의 fix는 netty 4.2.13.Final에만 있음 — 4.1.x line은 fix 없음. Spring Boot 3.5.x BOM이 netty 4.2.x로 갱신될 때까지 [accepted risk](./.trivyignore) (위협 모델: private k8s + Istio Gateway 뒤 — half-closed RST exploit 비현실적). 자세한 근거는 [ADR-0028 추후 등재] (현재 [BACKLOG R-28](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/BACKLOG.md)).

빌드 시간: ~2-3분 (R-27 (a) 적용).

---

## 트러블슈팅

- **백엔드 gRPC connection refused** → 5개 서비스가 떠 있어야. 각 서비스 README의 STEP 4 참조
- **JWT 검증 실패** → auth-service가 떠 있어야 (`CheckValidity` gRPC)
- **netty 관련 startup 에러** → `reactor-netty` 1.2.x + netty 4.1.x 의존. 4.2.x로 임의 override 시 ABI 호환 깨짐 ([BACKLOG R-28](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/BACKLOG.md))
- **`@Configuration class may not be final`** → common-libs 0.3.1+ 사용 ([TROUBLESHOOTING §1.7](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/TROUBLESHOOTING.md#17-kotlin-configuration-class가-final--spring-cglib-proxy-실패-r-38))

---

## 관련 문서

- [msa-argocd-manifest](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest) — `applications/values/api-gateway/`
- [ADR-0005](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/adr/0005-api-gateway-bff-with-cloud-gateway.md) — BFF + SC Gateway 혼합 결정
- [msa-auth-service](https://github.com/KTCloud-CloudNative-Troica-Team/msa-auth-service) — JWT 검증 의존
- [msa-product-service](https://github.com/KTCloud-CloudNative-Troica-Team/msa-product-service) / [order](https://github.com/KTCloud-CloudNative-Troica-Team/msa-order-service) / [inventory](https://github.com/KTCloud-CloudNative-Troica-Team/msa-inventory-service) / [user](https://github.com/KTCloud-CloudNative-Troica-Team/msa-user-service) — gRPC 백엔드
- [TROUBLESHOOTING.md](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/docs/TROUBLESHOOTING.md)
