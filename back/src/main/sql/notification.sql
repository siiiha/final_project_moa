USE final_project;

CREATE TABLE IF NOT EXISTS notification (
                                            notification_id  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                            member_id        BIGINT        NOT NULL,           -- 받는 사람
                                            type             VARCHAR(30)   NOT NULL,           -- PAYMENT_DUE, PREDICTION_DONE ...
    message          VARCHAR(300)  NOT NULL,           -- 화면에 보여 줄 문장
    link             VARCHAR(300),                     -- 누르면 이동할 React 주소 (예: /groups/3)
    read_at          DATETIME      NULL,               -- 읽은 시각. NULL 이면 안 읽음
    created_at       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_member_notification (member_id, notification_id),   -- "내 최근 알림" 을 빠르게
    FOREIGN KEY (member_id) REFERENCES member (member_id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;