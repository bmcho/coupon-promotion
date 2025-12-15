CREATE TABLE daily_point_reports
(
    id            BIGINT AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    report_date   DATE   NOT NULL,
    earn_amount   BIGINT NOT NULL DEFAULT 0,
    use_amount    BIGINT NOT NULL DEFAULT 0,
    cancel_amount BIGINT NOT NULL DEFAULT 0,
    net_amount    BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_daily_point_report UNIQUE (user_id, report_date)
);