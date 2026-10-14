---
description: 분석 정책 (제품 이벤트 수집, 서버 소유 이벤트, 분석 주체, 중복 제거, 보존, 삭제, 비식별 집계)
---

# 분석 (analytics)

## 제품 이벤트 수집

클라이언트가 화면 이벤트를 batch로 보낸다. best-effort 기능이라 실패해도 화면 기능을 막지 않는다.

- 허용 세션 mode는 `GUEST`, `ACTIVE`다
- 한 번에 이벤트 1~20개를 받는다. 성공하면 body 없이 `202`다
- 이벤트 이름과 속성은 닫힌 허용 목록이다

  | 이름 | 속성 |
  |---|---|
  | `onboarding_started` | 없음 |
  | `onboarding_skipped` | 없음 |
  | `today_sema_viewed` | `answerState`: `UNANSWERED`, `ANSWERED` |
  | `alternate_question_viewed` | 없음 |
  | `sema_question_changed` | `from`, `to`: `PRIMARY`, `ALTERNATE` |
  | `answer_started` | `mode`: `CREATE`, `UPDATE` |
  | `answer_save_failed` | `mode`: `CREATE`, `UPDATE` / `certainty`: `REJECTED`, `UNKNOWN` / `reasonCode`: `VALIDATION_REJECTED`, `DATE_CHANGED`, `CONTENT_REPLACED`, `CONFLICT`, `NETWORK_UNCONFIRMED`, `SERVER_UNAVAILABLE`, `PROTOCOL_UNCONFIRMED` |
  | `archive_viewed` | `state`: `EMPTY`, `NON_EMPTY` |

- 이름별 허용 속성은 모두 필수다. 속성이 없는 이벤트는 `properties`를 `{}`로 보낸다
- 아래 중 하나라도 걸리면 batch 전체를 `400 INVALID_REQUEST`로 거절하고 아무것도 저장하지 않는다
  - 이벤트 0개 또는 21개 이상
  - 모르는 이름, 서버 소유 이벤트 이름
  - 속성 누락, 허용 밖 속성, 허용 밖 값
  - `schemaVersion`이 1이 아님
  - `appVersion`이 50자를 넘음, 모르는 `platform`
  - 필드 누락, 형식 오류, 추가 필드
- 오류 응답과 로그에 입력값을 남기지 않는다
- `occurredAt`은 클라이언트 관찰 순서용이다. 지표의 귀속 기준은 서버 수신 시각이다

## 중복 제거

- 같은 `eventId`는 원시 이벤트가 남아 있는 동안 한 번만 적재한다. 이미 받은 `eventId`를 다시 보내도 `202`이고 새 행은 생기지 않는다
- 한 batch 안에 같은 `eventId`가 여럿이면 처음 것만 적재한다 (400 아님)
- 같은 batch가 동시에 두 번 오면 진 요청이 `500 DATABASE_ERROR`일 수 있다. 따로 막지 않는다. 다시 보내면 중복 제거로 `202`다
- 적재 단계는 `eventId` 중복 제거만 한다. "같은 날 여러 번은 1회" 같은 이벤트별 중복 집계 규칙은 분석 쿼리에서 정한다 (요구사항의 "추후 분석 명세")

## 분석 주체

- 분석 주체는 세션의 익명 키 주체(익명 키 해시)다. 클라이언트는 익명 키, 회원 코드, 분석 주체를 보내지 않는다
- 가입 전 `GUEST`로 보낸 이벤트와 가입 뒤 `ACTIVE`로 보낸 이벤트는 같은 주체다

## 서버 소유 이벤트

서버가 아래 처리의 성공 시점에 직접 기록한다. 클라이언트가 이 이름을 보내면 거절한다.

| 이름 | 기록 시점 |
|---|---|
| `onboarding_completed` | 회원 가입 성공 (`member.md`) |
| `answer_saved` | 답변 작성 성공 (`answer.md`) |
| `answer_edited` | 답변 수정 성공. 같은 본문으로 수정해도 기록한다 |
| `answer_deleted` | 답변 삭제 성공 |
| `all_data_deleted` | 모든 데이터 삭제 성공. 원시 이벤트가 아닌 비식별 일별 집계로만 기록한다 |

- 처리와 같은 트랜잭션에서 기록한다. 처리가 실패하면 이벤트도 남지 않는다

## 모든 데이터 삭제와 이벤트

- 모든 데이터 삭제(`member.md`)가 성공하면 그 회원의 익명 키 주체의 원시 이벤트를 모두 지운다. 가입 전 `GUEST`로 보낸 이벤트도 함께 지운다
- 같은 트랜잭션에서 그날(KST)의 `all_data_deleted` 집계를 1 늘린다. 집계는 날짜와 이름별 횟수만 담고 개인과 연결할 값이 없다
- 삭제와 같은 주체의 이벤트 batch가 겹치면 삭제 직후 commit된 batch 행이 남을 수 있다. 따로 막지 않는다. 실제 문제가 보이면 그때 보완한다

## 보존

- 원시 이벤트는 수신 시각부터 90일 보존한다. 90일이 지난 원시 이벤트를 파기하는 작업은 아직 구현하지 않았다 (후속 이슈)
- 비식별 일별 집계는 장기 보존한다
- 이벤트를 조회하는 API는 없다
