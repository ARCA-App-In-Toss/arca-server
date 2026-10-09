---
description: Entity(domain) 클래스, enum, DB 매핑 작성 패턴
paths:
  - "src/main/kotlin/**/domain/**/*.kt"
---

# Domain (Entity) Convention

객체 생성(`private constructor` + 팩토리)과 예외 처리 규칙은 `common.md`를 따른다.

## Entity 클래스

- 일반 `class`로 선언하라. `data class`를 쓰지 마라 (자동 생성되는 `equals`/`hashCode`/`toString`이 지연 로딩 연관관계까지 건드린다)
- JPA용 기본 생성자는 `kotlin-jpa` 플러그인이, 프록시용 `open`은 `allOpen` 설정이 만든다. 코드에 직접 쓰지 마라
- 생성자는 `private constructor`로 감추고 `companion object`의 `create()`로만 생성하라
  - `create()`는 값을 개별 파라미터로 받는다. 파라미터가 많아도 파라미터 객체로 묶지 않는다 (호출은 이름 붙인 인자라 위치가 섞이지 않는다)
- 주 생성자 파라미터는 프로퍼티(`val`/`var`)로 선언하지 말고 값만 받아라. 영속 필드는 클래스 본문에 선언한다 (생성자 프로퍼티에는 `protected set`을 지정할 수 없다)
- `createdAt` 등 시각 필드는 각 Entity에서 직접 관리하라 (공통 `BaseEntity` 없음)
- 시각은 `Instant`로 선언하라. `LocalDateTime`을 쓰지 마라. KST 날짜 자체가 값인 필드(`createdDateKst` 등)는 `LocalDate`다
- 검증 로직은 Entity 내부 private 함수로 구현하라

## 영속 필드

- 영속 필드는 `var` + `protected set`으로 선언하라
  - `val`은 final 필드가 되는데, JPA 명세는 영속 필드에 final을 허용하지 않는다
  - `allOpen`으로 프로퍼티가 open이 되므로 `private set`은 컴파일되지 않는다
- 값 변경은 Entity의 도메인 함수로만 하라 (외부에는 읽기만 노출)
- `@Column(nullable = false)`면 non-null 타입, nullable 컬럼이면 `?` 타입으로 맞춰라
- 식별자는 `var id: Long = 0L`로 선언하라. 0이면 저장 전 상태로 판정된다
- 컬렉션 연관관계는 `MutableList`로 선언하고 `mutableListOf()`로 초기화하라

```kotlin
@Entity
@Table(name = "post")
class Post private constructor(
    member: Member,
    title: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_id")
    var member: Member = member
        protected set

    @Column(nullable = false, name = "title")
    var title: String = title
        protected set

    @Column(nullable = false, name = "created_at")
    var createdAt: Instant = Instant.now()
        protected set

    companion object {
        fun create(
            member: Member,
            title: String
        ): Post {
            return Post(
                member = member,
                title = title
            )
        }
    }
}
```

## DB 매핑

- Entity 클래스명과 테이블명이 다르면 `@Table(name = "...")`을 명시하라 (테이블명은 snake_case 단수형)
- 컬럼명은 `@Column(name = "snake_case", ...)`으로 명시하라 (`@Column(name = "view_count", nullable = false)`)
- 컬럼명은 필드명을 snake_case로 옮긴 것이어야 한다. 필드는 `title`인데 컬럼은 `post_title`처럼 어긋나게 두지 마라
  (필드명 규칙은 `common.md`의 "필드명에 클래스명을 반복하지 마라"를 따른다)
- DB 예약어, 타입명과 겹치는 이름을 피하라. `year`, `order`, `rank`, `key`는 MySQL 예약어이거나 타입, 함수명이다
  (`academic_year`, `display_order`처럼 의미를 붙여 피한다)
- enum 매핑은 `@Enumerated(EnumType.STRING)`을 사용하라. ORDINAL을 사용하지 마라
- ID 생성은 `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 사용하라 (MySQL AUTO_INCREMENT)
- 연관관계는 `@ManyToOne(fetch = FetchType.LAZY)` 기본, 컬렉션은 필요 시 `@OneToMany` + `FetchType.LAZY`

## enum

- 값은 `enum class` 주 생성자의 `val` 프로퍼티로 선언하라
- `name` 프로퍼티는 선언할 수 없다 (`Enum.name`과 충돌한다). 표시 이름은 `displayName`으로 선언하라
- `values()` 대신 `entries`를 써라
- 조회 함수는 `companion object`에 둔다. 없을 수 있는 조회는 `Optional` 대신 nullable을 반환하고, 반드시 있어야 하는 조회는 `?: throw`로 작성하라

```kotlin
enum class PostStatus(
    val code: String,
    val displayName: String
) {
    PUBLISHED("P", "게시"),
    HIDDEN("H", "숨김");

    companion object {
        fun tryFromCode(code: String): PostStatus? {
            return entries.firstOrNull { it.code == code }
        }

        fun fromCode(code: String): PostStatus {
            return tryFromCode(code) ?: throw RestApiException(INVALID_REQUEST)
        }
    }
}
```

## 값 객체

- 여러 값을 묶어 전달하는 도메인 값 객체는 `data class`로 선언하라
