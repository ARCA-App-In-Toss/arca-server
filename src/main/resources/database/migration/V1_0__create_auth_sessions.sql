-- 액세스 토큰 세션. 토큰 원문 대신 SHA-256 해시를 저장하고, 폐기된 토큰과 모르는 토큰을 구분하려고 폐기된 행도 지우지 않는다
CREATE TABLE auth_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    session_mode VARCHAR(50) NOT NULL,
    member_id BIGINT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE INDEX uk_token_hash (token_hash),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB;
