---
description: Service 레이어 작성 패턴
paths:
  - "src/main/kotlin/**/service/**/*.kt"
---

# Service Convention

- `@Service` 클래스의 주 생성자로 의존성을 주입하라
- Service는 단일 도메인 로직만 담당하라. 다른 도메인 Service를 직접 호출하지 마라
  - 다른 도메인 데이터가 필요하면 해당 도메인 **Repository**를 주입해 접근하라 (예: `PostService`가 `MemberRepository`를 주입). Service 간 의존은 만들지 않는다
- Query/Command를 분리할 때는 `{Domain}QueryService`, `{Domain}CommandService`로 네이밍하라
- 조회 함수에는 `@Transactional(readOnly = true)`를, 변경 함수에는 `@Transactional`을 붙여라
- 반환값이 없는 함수는 반환 타입을 생략하라
- 조회 결과가 없을 때의 예외는 `?: throw RestApiException(XXX)`로 처리하라

```kotlin
@Service
class PostService(
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun deletePost(
        memberId: Long,
        postId: Long,
    ) {
        val post = postRepository.findByMemberIdAndPostId(memberId, postId)
            ?: throw RestApiException(POST_NOT_FOUND)

        postRepository.delete(post)
    }
}
```
