-- 회원. 익명 키 원문 대신 SHA-256 해시를 저장하고, uk_anonymous_key_hash로 익명 주체당 회원을 1명으로 제한한다
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(10) NOT NULL,
    anonymous_key_hash VARCHAR(64) NOT NULL,
    nickname VARCHAR(255) NULL,
    revision BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE INDEX uk_member_code (member_code),
    UNIQUE INDEX uk_anonymous_key_hash (anonymous_key_hash)
) ENGINE=InnoDB;
