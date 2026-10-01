---
description: Repository 레이어 작성 패턴
paths:
  - "src/main/kotlin/**/repository/**/*.kt"
---

# Repository Convention

- JPA Repository 인터페이스는 `{domain}/repository/` 패키지에 두고 `interface XxxRepository : JpaRepository<Xxx, Long>`으로 선언하라
- 쿼리는 Repository 인터페이스에 `@Query`로 직접 작성하라. JPQL을 기본으로 하고, FULLTEXT 등 DB 종속 쿼리만 `nativeQuery = true`로 작성하라
- 쿼리는 raw string(`"""`)으로 작성하고 `trimIndent()`를 붙이지 마라 (어노테이션 인자는 컴파일 타임 상수여야 한다)
- 파라미터는 `@Param("...")`으로 바인딩하라
- 단건 조회는 `Optional<T>` 대신 nullable(`T?`)을 반환하라
- 기본 `findById`는 확장 함수 `findByIdOrNull`(`org.springframework.data.repository.findByIdOrNull`)로 호출하라
- 반환값이 없는 수정 쿼리는 반환 타입을 생략하라
- 기본 조회는 Entity를 반환하고 DTO 조립은 Service에서 한다. 집계, 부분 데이터는 projection으로 직접 반환하라
  - projection으로 받는 DTO는 `{domain}/dto/internal/`에 `~Dto` 이름으로 둔다 (`dto.md`)

```kotlin
interface PostRepository : JpaRepository<Post, Long> {
    @Query("""
        SELECT p
        FROM Post p
        WHERE p.member.id = :memberId
          AND p.id = :postId
    """)
    fun findByMemberIdAndPostId(
        @Param("memberId") memberId: Long,
        @Param("postId") postId: Long
    ): Post?
}
```
