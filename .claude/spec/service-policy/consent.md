---
description: 동의 정책 (필수 정책 목록, 버전)
---

# 동의 (consent)

## 필수 정책 목록

회원으로 가입하려면 아래 정책에 모두 동의해야 한다. 세션 교환 응답은 이 목록을 항상 담는다.

| 정책 ID | 버전 | 제목 | URL | 필수 |
|---|---|---|---|---|
| `terms-of-service` | `1` | 서비스 이용약관 | `https://arca.invalid/policies/terms-of-service/1` | 예 |
| `privacy-policy` | `1` | 개인정보처리방침 | `https://arca.invalid/policies/privacy-policy/1` | 예 |

- 위 ID, 버전, URL은 법무 확정 전 백엔드 가정값이다. URL은 실제 주소로 오인되지 않게 예약 도메인(`.invalid`)을 쓴다
- 선택 동의와 마케팅 동의는 두지 않는다. 모든 정책이 필수다
- 정책 버전을 바꾸면 배포를 거쳐 반영된다
