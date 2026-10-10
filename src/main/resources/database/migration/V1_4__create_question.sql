-- 질문. 원문을 고치면 content_version을 올린다. 답변은 저장 당시 원문을 스냅샷으로 따로 보존하므로 이 행이 바뀌어도 영향받지 않는다
CREATE TABLE question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(500) NOT NULL,
    content_version BIGINT NOT NULL
) ENGINE=InnoDB;
