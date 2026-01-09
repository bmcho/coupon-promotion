# Coupon Promotion 플랫폼

쿠폰 발급, 포인트 적립, 타임세일 운영 같은 프로모션 업무를 서비스별로 분리해 운영 효율과 확장성을 높이기 위한 플랫폼입니다. 각 서비스가 독립적으로 배포·확장되도록 구성되어 있어, 이벤트 성격에 맞는 트래픽 대응과 장애 격리가 가능합니다.

## 주요 특징

- 프로모션 도메인을 쿠폰/포인트/타임세일로 분리해 이벤트 성격에 맞는 확장성과 장애 격리를 지원합니다.
- Gateway에서 인증/레이트리밋을 처리하고, Eureka로 서비스 디스커버리를 수행합니다.
- Redis, Kafka, 배치 작업을 통해 대량 발급/적립 시나리오와 비동기 처리를 지원합니다.
- Prometheus/Grafana로 서비스 메트릭을 수집하고 관측성을 확보합니다.

## 구성 서비스

| 서비스 | 역할 | 포트 | 비고 |
| --- | --- | --- | --- |
| `discovery-service` | Eureka Server | `8761` | 서비스 레지스트리 |
| `api-gateway` | Spring Cloud Gateway | `8000` | JWT 인증 필터, Rate Limiter |
| `user-service` | 회원/인증 서비스 | `8004` | H2 기본 설정 |
| `coupon-service` | 쿠폰 발급/관리 | `8080` | MySQL + Redis + Kafka |
| `point-service` | 포인트 적립/차감 | `8083` | MySQL + Redis |
| `point-service-batch` | 포인트 배치 작업 | - | Spring Batch |
| `time-sale-service` | 타임세일 이벤트 | `8084` | MySQL + Redis + Kafka |

## 인프라 구성 (Docker Compose)

`infrastructure/docker-compose.yml` 기준으로 다음 컴포넌트가 포함되어 있습니다.

| 구성 요소 | 포트 | 용도 |
| --- | --- | --- |
| Redis | `6379` | 캐시/분산락 |
| Kafka | `9092`(내부), `9091`(외부) | 이벤트 스트림 |
| Kafka UI | `9090` | 토픽/메시지 관리 |
| MySQL | `3306` | 트랜잭션 데이터베이스 |
| Prometheus | `9000` | 메트릭 수집 |
| Grafana | `3000` | 대시보드 |
| Jenkins | `8888` | CI/CD |

## 실행 방법

### 1) 인프라 기동

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

### 2) 서비스 실행 (로컬 개발)

Eureka → Gateway → 도메인 서비스 순서로 실행을 권장합니다.

```bash
./gradlew :discovery-service:bootRun
./gradlew :api-gateway:bootRun
./gradlew :user-service:bootRun
./gradlew :coupon-service:bootRun
./gradlew :point-service:bootRun
./gradlew :time-sale-service:bootRun
```

배치 작업은 필요 시 별도로 실행합니다.

```bash
./gradlew :point-service-batch:bootRun
```

## Observability

`coupon-service`, `point-service`, `time-sale-service`는 `/actuator/prometheus` 엔드포인트를 통해 메트릭을 노출합니다. Prometheus/Grafana 설정은 `infrastructure/prometheus` 및 `infrastructure/grafana` 디렉터리를 참고하세요.

## 향후 개선점 (코드 분석 기반 제안)

- **비밀정보 외부화**: DB 계정/비밀번호와 JWT 키가 `application.yaml`에 하드코딩되어 있어, 환경 변수 또는 Vault/Secrets Manager로 분리 필요.
- **Kafka 설정 정리**: `docker-compose.yml`의 `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` 환경 변수에 오탈자가 있어 수정 필요.
- **환경별 프로파일 정리**: `dev/local/prod` 프로파일을 분리해 데이터소스/Redis/Kafka 설정을 명확히 구분.
- **API 문서화**: Springdoc(OpenAPI) 추가로 Gateway 뒤 서비스별 API 문서 제공.
- **통합 테스트 강화**: Kafka/Redis를 포함한 통합 테스트와 부하 테스트를 파이프라인에 통합.

## 기술 스택

- Java 17, Spring Boot 3.5.x, Spring Cloud 2025.0.0
- Spring Cloud Gateway, Eureka
- JPA, MySQL, H2
- Redis, Redisson
- Kafka, Spring Batch
- Prometheus, Grafana
- Docker, Docker Compose
