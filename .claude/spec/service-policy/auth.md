---
description: 인증, 세션 정책 (토큰 발급, 폐기, 만료, 세션 mode별 허용 범위)
---

# 인증 (auth)

## 토큰

- 세션 교환을 제외한 모든 API는 `Authorization: Bearer <토큰>` 헤더로 인증한다. 인증 scheme `Bearer`는 대소문자를 구분하지 않는다
- 토큰은 서버가 발급하는 opaque 문자열이다. refresh token과 cookie는 쓰지 않는다
- 토큰의 유효 기간은 발급 시각부터 24시간이다. 응답의 `expiresAt`이 그 시각이며, 그 시각부터 만료로 본다
- 서버는 토큰 원문을 저장하지 않는다. 원문은 발급 응답에서 한 번만 나간다

## 인증 실패 판정

요청마다 아래 순서로 판정하고, 처음 걸린 조건의 오류로 응답한다.

1. `Authorization` 헤더가 없거나, `Bearer` 형식이 아니거나, 토큰이 비어 있으면 `401 SESSION_INVALID`
2. 서버가 발급한 적 없는 토큰이면 `401 SESSION_INVALID`
3. 서버가 폐기한 토큰이거나 만료된 토큰이면 `401 SESSION_RECOVERY_REQUIRED` (`recovery.kind = REESTABLISH_SESSION`). 클라이언트는 세션을 다시 교환한다
4. 엔드포인트가 허용하지 않는 세션 mode면 `403 SESSION_SCOPE_INSUFFICIENT`

- 폐기된 토큰과 모르는 토큰을 구분하려고 폐기된 세션도 지우지 않고 남긴다

## 세션 mode

| mode | 조건 | 승객 |
|---|---|---|
| `PRE_PASSENGER` | 유효한 익명 키지만 활성 승객이 없음 (처음 온 사용자, 전체 삭제 뒤 재탑승 전) | 없음 |
| `ACTIVE` | 활성 승객과 연결됨 | 있음 |

- 엔드포인트마다 허용하는 mode가 정해져 있다. 허용 mode를 정하지 않은 엔드포인트는 인증 없이 호출된다 (현재는 세션 교환만 해당)
- 응답 유실 복구를 위한 별도 mode(`DELETION_RECOVERY`)와 결과 확인(ack)은 두지 않는다. 실제로 유실 문제가 관측되면 그때 보완한다
