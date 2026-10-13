-- 모든 데이터 삭제 때 옮겨 두는 동의 증빙. 활성 저장소(member, terms_agreement)와 분리한 테이블이고 회원과 FK가 없다
-- 정책 종류, 버전, 동의 시각, 삭제 시각, 가명 주체값(익명 키 해시)만 둔다. 삭제 시각부터 1년 보존한다
CREATE TABLE terms_agreement_evidence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    anonymous_key_hash VARCHAR(64) NOT NULL,
    terms_id VARCHAR(100) NOT NULL,
    terms_version VARCHAR(50) NOT NULL,
    agreed_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
