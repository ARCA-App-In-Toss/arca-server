-- 회원. 익명 키 원문 대신 SHA-256 해시를 저장하고, uk_anonymous_key_hash로 익명 주체당 회원을 1명으로 제한한다
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(10) NOT NULL,
    anonymous_key_hash VARCHAR(64) NOT NULL,
    nickname VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE INDEX uk_member_code (member_code),
    UNIQUE INDEX uk_anonymous_key_hash (anonymous_key_hash)
) ENGINE=InnoDB;

-- 액세스 토큰 세션. 토큰 원문 대신 SHA-256 해시를 저장하고, 폐기된 토큰과 모르는 토큰을 구분하려고 폐기된 행도 지우지 않는다
-- anonymous_key_hash로 세션을 익명 키 주체에 묶는다. 가입은 GUEST 세션의 이 값으로 새 회원을 익명 키와 연결한다
CREATE TABLE auth_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    session_mode VARCHAR(50) NOT NULL,
    member_id BIGINT NULL,
    anonymous_key_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE INDEX uk_token_hash (token_hash),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB;

-- 회원의 필수 약관 동의 기록. 가입 시 약관마다 한 행을 남기고, 동의 시각은 서버 시각이다
CREATE TABLE terms_agreement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    terms_id VARCHAR(100) NOT NULL,
    terms_version VARCHAR(50) NOT NULL,
    agreed_at DATETIME(6) NOT NULL,
    INDEX idx_terms_agreement_member_id (member_id),
    FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;

-- 질문. 원문은 수정하지 않는다
CREATE TABLE question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(500) NOT NULL
) ENGINE=InnoDB;

-- 세마포어(계약 용어 SEMA) 콘텐츠. 기본 질문과 대체 질문을 하나씩 NOT NULL FK로 가져 "세마포어 1 ── 2 질문"을 스키마가 보장한다
-- date_kst는 편성된 KST 날짜다. 예비 세마포어는 NULL이고, uk_semaphore_date_kst로 날짜당 세마포어를 하나로 제한한다
-- 콘텐츠는 수정하지 않는다. 바꿀 때는 예비 SEMA로 교체하고 콘텐츠 ID(semaId)가 바뀐다
CREATE TABLE semaphore (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    date_kst DATE NULL,
    primary_question BIGINT NOT NULL,
    alternate_question BIGINT NOT NULL,
    UNIQUE INDEX uk_semaphore_date_kst (date_kst),
    FOREIGN KEY (primary_question) REFERENCES question (id),
    FOREIGN KEY (alternate_question) REFERENCES question (id)
) ENGINE=InnoDB;

-- 답변. 삭제는 행을 지우므로 남은 행이 모두 활성 답변이다
-- uk_answer_member_id_created_date_kst로 회원 x 배포 단위(KST 날짜)당 답변을 1개로 제한한다. 선두 컬럼 member_id가 FK 인덱스와 회원별 COUNT를 겸한다
-- semaphore_id, question_id는 답변한 SEMA와 고른 질문이다. 질문과 SEMA 행은 수정하지 않고, 예비 교체는 옛 SEMA 행을 남기므로 참조가 바뀌지 않는다
-- content는 코드 포인트 15,000개 상한이다. H2가 TEXT를 CLOB으로 보고해 스키마 검증과 맞지 않아 VARCHAR로 둔다 (utf8mb4 행 크기 상한 안)
CREATE TABLE answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    semaphore_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    content VARCHAR(15000) NOT NULL,
    created_date_kst DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE INDEX uk_answer_member_id_created_date_kst (member_id, created_date_kst),
    INDEX idx_answer_semaphore_id (semaphore_id),
    INDEX idx_answer_question_id (question_id),
    FOREIGN KEY (member_id) REFERENCES member (id),
    FOREIGN KEY (semaphore_id) REFERENCES semaphore (id),
    FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE=InnoDB;
