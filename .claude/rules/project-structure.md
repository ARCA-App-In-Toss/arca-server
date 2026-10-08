---
description: 프로젝트 디렉토리 구조. 새 파일을 생성하거나 패키지 위치를 결정할 때 참조
paths:
  - "src/main/kotlin/**/*.kt"
  - "src/test/kotlin/**/*.kt"
---

# 프로젝트 구조

루트 패키지는 `com.arca`다.

```
src/main/kotlin/com/arca/
├── ArcaApplication.kt
├── global/              # 공통 설정(config), 예외(exception), 어노테이션, HTTP, 공용 infra
└── auth/                # 인증 (세션 교환, 토큰 검증, 세션 mode 검사)
```

도메인 패키지는 생길 때마다 위 트리에 `{domain}/  # 한 줄 설명` 형태로 추가한다.

## global 내부 구조

```
global/
├── config/                          # 스프링 설정 (CORS, Swagger, ArgumentResolver 등)
├── property/                        # @ConfigurationProperties 설정 묶음 (CorsProperties, AuthSessionProperties)
├── annotation/                      # 커스텀 어노테이션 (검증용 등)
├── infra/                           # 어노테이션 구현체, 서블릿 필터, 공용 헬퍼
└── exception/
    ├── domain/                      # ExceptionCode, ExceptionCategory, ExceptionRecovery, ExceptionRecoveryKind, RestApiException
    ├── dto/response/                # ErrorResponse, ErrorDetail
    └── handler/                     # GlobalExceptionHandler
```

## 도메인 패키지 내부 구조

```
{domain}/
├── controller/
│   ├── {Domain}Controller.kt
│   └── {Domain}ControllerDocs.kt     # Swagger 문서 인터페이스 (같은 controller/ 패키지에 둔다)
├── service/
│   └── {Domain}Service.kt
├── repository/
│   └── {Domain}Repository.kt
├── domain/
│   └── {Domain}.kt                   # Entity (+ 관련 enum)
├── dto/
│   ├── request/
│   ├── response/
│   └── internal/                     # 공용, projection, 캐시 DTO (선택). 이름은 `~Dto`
└── infra/                            # 도메인 전용 검증/헬퍼 (선택)
```

레이어 흐름은 **Controller → Service → Repository**다 (Facade 레이어 없음). 필요할 때만 `infra/`에 도메인 보조 로직을 둔다.

테스트는 `src/test/kotlin/com/arca/` 아래에 같은 패키지 경로로 미러링한다 (`.claude/spec/test-convention.md`).
