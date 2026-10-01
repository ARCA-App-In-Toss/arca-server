---
description: 테스트 코드 작성 규칙 (모든 테스트는 통합 테스트, Kotlin)
---

# Test Convention

모든 서비스 테스트는 통합 테스트로 작성한다. 기본은 `@IntegrationTest`(H2)다. Mockito 기반 단위 테스트는 쓰지 않는다.
테스트 코드도 코드 컨벤션(`.claude/rules/code-convention/`)을 따른다. 이 문서는 테스트에서 다르게 쓰는 부분과 테스트에만 있는 규칙을 적는다.

## 네이밍 & 설정

- 위치: `src/test/kotlin/com/arca/{domain}/...`. `src/main/kotlin`의 도메인 구조를 미러링하라
- 파일명: `{Class}Test.kt` (예: `PostServiceTest.kt`)
- 클래스 어노테이션: `@IntegrationTest` (`src/test/kotlin/com/arca/global/infra/IntegrationTest.kt`)
  - 커스텀 어노테이션 = `@SpringBootTest` + `@Transactional` + `@TestConstructor(autowireMode = ALL)`, H2 사용
  - 각 테스트는 `@Transactional` 롤백으로 격리된다. 별도 truncate 스크립트를 쓰지 않는다
- 검증 라이브러리는 AssertJ (`assertThat`, `assertThatThrownBy`)

## 의존성 주입

- 주 생성자의 `private val` 파라미터로 주입하라. `@TestConstructor`가 붙어 있어 `@Autowired`를 쓰지 않는다
- `@Autowired lateinit var` 필드 주입을 쓰지 마라
- 설정 값은 파라미터에 `@param:Value`를 붙여 받는다 (`@param:Value("\${cors.allowed-origin}") private val allowedOrigin: String`)
- 파라미터는 테스트 대상 → 데이터 준비용 Repository → 그 밖의 컴포넌트(`EntityManager`, 설정 값) 순으로 묶고, 그룹 사이를 빈 줄로 나눠라

```kotlin
@IntegrationTest
class PostServiceTest(
    private val postService: PostService,

    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,

    private val entityManager: EntityManager
)
```

## 메서드 작성

- 메서드명은 한글 서술형이고 단어를 `_`로 잇는다 (`fun 작성자가_게시글을_삭제하면_성공한다()`). 백틱과 공백을 쓰지 마라
- `@Nested` 클래스로 시나리오를 그룹화하라 (클래스명도 한글 서술형)
  - `inner class`로 선언하라. `inner`가 없으면 static 중첩 클래스가 되어 JUnit이 실행하지 않는다
- 본문은 `//given` / `//when` / `//then` 주석으로 구간을 구분하라
- 지역 변수는 `val`로 선언하고 타입은 추론에 맡긴다

## 상수와 상태

- 테스트 클래스의 상수는 `companion object`에 `private const val`로 선언하라 (공통 규칙)
- `@Nested` 클래스 안에서만 쓰는 값은 그 클래스의 `private val`로 선언하고 이름은 camelCase로 짓는다
  - `inner class`에는 `companion object`를 둘 수 없다. 바깥으로 올리면 중첩 클래스마다 다른 값을 쓰는 같은 이름이 부딪친다
- `@BeforeEach`에서 채우는 상태는 참조 타입이면 `private lateinit var`, `Long`, `Int`면 `private var id = 0L`로 선언하라 (원시 타입에는 `lateinit`을 쓸 수 없다)

## 검증

- 컬렉션 요소를 뽑아 검증할 때는 `extracting`에 타입 인자와 람다를 쓴다 (`.extracting<String> { it.title }`)
  - `extracting(PostResponse::title)`처럼 참조를 넘기면 튜플을 돌려주는 가변 인자 오버로드로 잡혀 컴파일되지 않는다
- 여러 값을 튜플로 뽑을 때는 프로퍼티 참조를 넘긴다 (`.extracting(PostResponse::title, PostResponse::status)`). 위의 가변 인자 오버로드가 그대로 쓰인다
- `doesNotContain`, `allSatisfy`처럼 대상이 비면 아무것도 검사하지 않고 통과하는 단언은 앞에 `isNotEmpty()`를 둔다 (`.extracting<String> { it.title }.isNotEmpty().doesNotContain("X")`)
  - 앞에 `hasSize`, `contains`, `containsExactly`가 이미 있으면 빈 목록에서 실패하므로 따로 넣지 않는다
- 목록에서 요소 하나를 찾을 때는 `first { it.title == "첫 번째 글" }`를 쓴다
- 반드시 있어야 하는 저장 결과를 다시 읽을 때는 `findById(id).orElseThrow()`를, nullable을 돌려주는 조회는 `checkNotNull(...)`을 쓴다. `!!`를 쓰지 마라
  - `require()`/`check()` 금지는 응답 코드가 500으로 떨어지는 비즈니스 검증에 대한 규칙이라 테스트의 전제 확인에는 해당하지 않는다

## 예외 검증

`RestApiException`을 던지는 예외 케이스는 반드시 `exceptionCode`까지 검증하라. 타입만 검증하면 다른 코드로 회귀해도 통과해 회귀를 못 잡는다.

- 타입 + 코드: `.isInstanceOf(RestApiException::class.java).hasFieldOrPropertyWithValue("exceptionCode", {CODE})`
- `ExceptionCode` 항목은 개별 import로 식별자만 노출하라 (`ExceptionCode.X` 표기 금지, 와일드카드 import 금지)

```kotlin
import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND

assertThatThrownBy { postService.getMyPosts(invalidMemberId) }
    .isInstanceOf(RestApiException::class.java)
    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
```

## MySQL 전용 쿼리 테스트

FULLTEXT 등 H2가 실행하지 못하는 네이티브 쿼리는 `@MySqlIntegrationTest`(Testcontainers MySQL)로 검증한다.
(Testcontainers 의존성과 `@MySqlIntegrationTest`는 이런 쿼리가 처음 생길 때 추가한다)

- 별도 클래스로 분리하고 이름은 `{Class}{기능}Test`로 짓는다 (예: `PostServiceSearchTest`)
- 트랜잭션 롤백 격리가 없다고 가정하라. 커밋된 행만 보는 쿼리(InnoDB FULLTEXT 등)는 저장이 그대로 커밋된다.
  `@AfterEach`에서 `deleteAllInBatch()`로 직접 지워라
- Docker가 없는 환경에서는 skip된다. CI에서는 항상 실행된다

## Fixture

| 항목 | 규칙 |
|---|---|
| 위치 | `src/test/kotlin/com/arca/{domain}/fixture/` |
| 선언 | `object {Domain}Fixture` (예: `PostFixture`, `MemberFixture`) |
| 생성 방식 | 엔티티의 팩토리(`{Entity}.create(...)`)로 만든다. 팩토리가 받지 않는 값(조회수, 생성 시각처럼 도메인 흐름이 바꾸는 값)만 `ReflectionTestUtils.setField(entity, "필드명", 값)`로 채운다 |
| 오버로드 | 기본값 세트 + 세부 필드를 받는 팩토리 함수를 함께 제공 (`createPost()`, `createPostWithDetails(...)`) |
| 호출 | 위치 인자로 호출한다. 공통 규칙의 이름 붙인 인자 규칙은 픽스처 호출에 적용하지 않는다. 여러 줄이어도 마지막 인자 뒤에 쉼표를 붙이지 않는다 |

- `kotlin-jpa`가 만드는 기본 생성자는 소스에서 부를 수 없다. `BeanUtils.instantiateClass`로 빈 엔티티를 만들지 마라. 필드 초기화가 실행되지 않아 컬렉션과 기본값이 비어 있다
