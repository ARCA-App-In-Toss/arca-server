-- 회원의 필수 정책 동의 기록. 가입 시 정책마다 한 행을 남기고, 동의 시각은 서버 시각이다
CREATE TABLE consent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    policy_id VARCHAR(100) NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    agreed_at DATETIME(6) NOT NULL,
    INDEX idx_consent_member_id (member_id),
    FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB;
