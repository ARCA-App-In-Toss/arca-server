-- 제품 분석 원시 이벤트. 클라이언트 batch와 서버 소유 이벤트를 함께 둔다. 수신 시각부터 90일 보존한다
-- anonymous_key_hash는 세션에서 파생한 분석 주체다. 모든 데이터 삭제 때 이 값으로 그 주체의 행을 지운다
-- uk_product_event_event_id로 같은 eventId를 한 번만 적재한다
CREATE TABLE product_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    anonymous_key_hash VARCHAR(64) NOT NULL,
    product_event_name VARCHAR(50) NOT NULL,
    schema_version INT NOT NULL,
    properties VARCHAR(500) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    app_version VARCHAR(50) NULL,
    product_event_platform VARCHAR(50) NULL,
    UNIQUE INDEX uk_product_event_event_id (event_id),
    INDEX idx_product_event_anonymous_key_hash (anonymous_key_hash)
) ENGINE=InnoDB;

-- 비식별 일별 집계. 개인과 연결할 값 없이 KST 날짜, 이벤트 이름별 횟수만 둔다. 장기 보존한다
CREATE TABLE product_event_daily_count (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_kst DATE NOT NULL,
    product_event_name VARCHAR(50) NOT NULL,
    total_count BIGINT NOT NULL,
    UNIQUE INDEX uk_product_event_daily_count_date_kst_name (date_kst, product_event_name)
) ENGINE=InnoDB;
