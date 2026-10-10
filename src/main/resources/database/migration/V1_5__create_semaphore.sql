-- 세마포어(계약 용어 SEMA) 콘텐츠. 기본 질문과 대체 질문을 하나씩 NOT NULL FK로 가져 "세마포어 1 ── 2 질문"을 스키마가 보장한다
-- date_kst는 편성된 KST 날짜다. 예비 세마포어는 NULL이고, uk_semaphore_date_kst로 날짜당 세마포어를 하나로 제한한다
CREATE TABLE semaphore (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL,
    date_kst DATE NULL,
    primary_question BIGINT NOT NULL,
    alternate_question BIGINT NOT NULL,
    UNIQUE INDEX uk_semaphore_date_kst (date_kst),
    FOREIGN KEY (primary_question) REFERENCES question (id),
    FOREIGN KEY (alternate_question) REFERENCES question (id)
) ENGINE=InnoDB;
