-- 세션을 익명 키 주체에 묶는다. 가입은 GUEST 세션의 이 값으로 새 회원을 익명 키와 연결한다
ALTER TABLE auth_session ADD COLUMN anonymous_key_hash VARCHAR(64) NOT NULL;
