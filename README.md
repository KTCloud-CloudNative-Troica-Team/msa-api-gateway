# msa-api-gateway

Troica Market Service의 **외부 진입점** — BFF + Spring Cloud Gateway 혼합 (Q1 (c) 결정).

> Single source of truth: [TROICA_SPEC.md](https://github.com/KTCloud-CloudNative-Troica-Team/msa-argocd-manifest/blob/main/TROICA_SPEC.md)

## 단일 모듈 구조

```
msa-api-gateway/
├── src/main/
│   ├── kotlin/dev/ktcloud/black/user/api/gateway/
│   │   ├── UserApiGatewayApplication.kt
│   │   ├── adapter/presentation/web/configuration/   # SecurityConfig + JwtHeaderCheckFilter
│   │   ├── adapter/presentation/web/inbound/         # BFF REST controllers (auth/order/product/inventory)
│   │   └── application/                              # BFF application layer
│   ├── proto/{auth, order, product, inventory}.proto # 옵션 2 (Q1 후속): 자체 proto 보유
│   └── resources/application.yaml + dev/prod
```

모노레포 `user-api-gateway/` 그대로 가져옴 + Spring Cloud Gateway routes 신규 추가.

## Q1 (c) 혼합 패턴

| 경로 패턴 | 처리 방식 | 비고 |
|----------|---------|------|
| `/api/v1/orders/**`      | **BFF** (REST → gRPC OrderService)     | 도메인 변환 + 응답 가공 |
| `/api/v1/products/**`    | **BFF** (REST → gRPC ProductService)   | 동일 |
| `/api/v1/inventories/**` | **BFF** (REST → gRPC InventoryService) | 동일 |
| `/api/v1/auth/**`        | **BFF** (REST → gRPC AuthService)      | signUp / signIn / checkValidity |
| `/api/v1/users/**`       | **SC Gateway route** (HTTP passthrough)| user-service의 REST endpoint 직접 노출 |
| `/admin/v1/orders/**`    | **SC Gateway route** (HTTP passthrough)| order-service admin (state machine 시뮬레이션 트리거) |

## 의존성

- Spring Cloud BOM **2025.0.2** (Northfields) — Spring Boot 3.5.x 공식 짝
- `spring-cloud-starter-gateway-server-webflux` (명시 아티팩트, 2025.0+ 권장)
- `grpc-client-spring-boot-starter:3.1.0.RELEASE`
- jjwt 0.12.6 (JWT parse 용도. 실제 검증은 auth-service gRPC `CheckValidity`)
- springdoc-openapi-starter-webflux-ui:2.5.0
- `com.troica.msa:common:0.3.0`

JitPack client-redis 미사용 (gateway는 Redis 직접 안 씀).

## JWT 검증 흐름

```
[Client] → Bearer token → [api-gateway/JwtHeaderCheckFilter]
                          ├─ /api/v1/orders/** 매치 → auth-service.checkValidity gRPC
                          │  ├─ valid: SecurityContext에 UsernamePasswordAuthenticationToken 주입
                          │  └─ invalid: 401 / 403
                          └─ 그 외 경로: passthrough (permitAll)
```

모노레포 `JwtHeaderCheckFilter.kt` 코드 그대로. 보호 경로는 `MATCH_LIST = ["/api/v1/orders"]` (점진적 활성화).

## 포트 / 인프라

| 항목 | 값 |
|------|----|
| HTTP | 8100 |
| DB / Kafka / Redis | 미사용 (gRPC client만) |
| 백엔드 호출 (gRPC) | product-service:9001, order-service:9002, inventory-service:9003, auth-service:9005 |

## 빌드 + 실행

```bash
./gradlew build -x test

docker build \
  --build-arg GPR_USER=$GITHUB_ACTOR \
  --build-arg GPR_TOKEN=$GITHUB_TOKEN \
  -t msa/api-gateway:local .

docker run --rm -p 8100:8100 \
  -e SPRING_PROFILES_ACTIVE=dev \
  msa/api-gateway:local
```

## CI

- PR + push: `build-test`
- Push to main + `vars.AWS_DEPLOYMENTS_ENABLED == 'true'`: ECR push + manifest update (Phase 0 후)
