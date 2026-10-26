-- Moa schema submission draft, 2026-10-08.
-- Select an empty MySQL database before executing this file.
-- 28 tables, 40 foreign keys, 6 UNIQUE constraints, 3 CHECK constraint.
-- Single-column numeric primary keys use AUTO_INCREMENT.
-- ERDCloud export supplemented in SQL; not applied to ERDCloud.
-- Structural validation only; execution in MySQL is still pending.

CREATE TABLE IF NOT EXISTS `member_interest` (
	`interest_id`	BIGINT	NOT NULL,
	`member_id`	BIGINT	NOT NULL,
	PRIMARY KEY (`interest_id`,
	`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment` (
	`comment_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`post_id`	BIGINT	NOT NULL,
	`writer_id`	BIGINT	NOT NULL,
	`parent_id`	BIGINT	NULL,
	`content`	TEXT	NOT NULL,
	`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
		ON UPDATE CURRENT_TIMESTAMP,
	`deleted_at` DATETIME NULL,
	PRIMARY KEY (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_event` (
	`schedule_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`group_id`	BIGINT	NOT NULL,
	`writer_id`	BIGINT	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`location`	VARCHAR(200)	NULL,
	`event_date`	DATETIME	NOT NULL,
	PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_category` (
	`category_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`category_name`	VARCHAR(50)	NOT NULL,
	PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_account` (
	`account_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`group_id`	BIGINT	NOT NULL,
	`bank_code`	VARCHAR(20)	NOT NULL,
	`account_number`	VARCHAR(50)	NOT NULL,
	`account_holder_name`	VARCHAR(100)	NOT NULL,
	`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `term_agreement` (
	`agreement_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`term_id`	BIGINT	NOT NULL,
	`member_id`	BIGINT	NOT NULL,
	`agreed_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`agreement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fee_payment` (
	`dues_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`group_id`	BIGINT	NOT NULL,
	`member_id`	BIGINT	NOT NULL,
	`round_id`	BIGINT	NULL,
	`amount`	DECIMAL(15,2)	NOT NULL,
	`due_date`	DATE	NOT NULL,
	`is_paid`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`paid_at`	DATETIME	NULL,
	PRIMARY KEY (`dues_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `members` (
	`member_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`region_id`	BIGINT	NOT NULL,
	`email`	VARCHAR(40)	NOT NULL,
	`password_hash`	VARCHAR(255)	NULL,
	`name`	VARCHAR(50)	NOT NULL,
	`nickname`	VARCHAR(50)	NOT NULL,
	`phone`	VARCHAR(20)	NOT NULL,
	`birth_date`	DATE	NOT NULL,
	`gender`	ENUM('MALE', 'FEMALE')	NOT NULL,
	`profile_img_url`	VARCHAR(500)	NULL,
	`user_level`	VARCHAR(20)	NULL,
	`system_role`	ENUM('USER',  'ADMIN')	NOT NULL	DEFAULT 'USER',
	`identity_verified_at`	DATETIME	NULL,
	`region_verified_at`	DATETIME	NULL,
	`account_status`	ENUM('ACTIVE', 'SUSPENDED',  'INACTIVE')	NOT NULL	DEFAULT 'ACTIVE',
    `suspended_until` DATETIME NULL DEFAULT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deleted_at`	DATETIME	NULL,
	PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_auth_session (
    session_id CHAR(36)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    member_id BIGINT NOT NULL,
    expires_at_ms BIGINT NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (session_id),
    KEY ix_member_auth_session_member (member_id),
    KEY ix_member_auth_session_expires (expires_at_ms),

    CONSTRAINT fk_member_auth_session_member
        FOREIGN KEY (member_id)
        REFERENCES members (member_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE  TABLE IF NOT EXISTS member_refresh_token (
    token_hash CHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    session_id CHAR(36)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT
CURRENT_TIMESTAMP(6),

    PRIMARY KEY (token_hash),
    KEY ix_member_refresh_token_session (session_id),

    CONSTRAINT fk_member_refresh_token_session
        FOREIGN KEY (session_id)
        REFERENCES member_auth_session (session_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_social_account (
                                                     social_account_id BIGINT NOT NULL AUTO_INCREMENT,
                                                     member_id BIGINT NOT NULL,
                                                     provider VARCHAR(20)
    CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provider_user_id VARCHAR(255)
    CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (social_account_id),

    UNIQUE KEY uq_member_social_account_provider_user
(provider, provider_user_id),

    CONSTRAINT chk_member_social_account_provider
    CHECK (provider IN ('KAKAO', 'NAVER', 'GOOGLE')),

    CONSTRAINT fk_member_social_account_member
    FOREIGN KEY (member_id)
    REFERENCES members (member_id)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `meeting_approval` (
	`approval_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`group_id`	BIGINT	NOT NULL,
	`type`	ENUM('CREATE', 'DELETE')	NOT NULL,
	`bank_book_url`	VARCHAR(500)	NULL,
	`status`	ENUM('PENDING','APPROVED','REJECTED')	NOT NULL	DEFAULT 'PENDING',
	`reject_reason`	VARCHAR(255)	NULL,
	`processed_at`	DATETIME	NULL,
	PRIMARY KEY (`approval_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_notice` (
	`notice_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`group_id`	BIGINT	NOT NULL,
	`writer_id`	BIGINT	NOT NULL,
	`title`	VARCHAR(200)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification` (
	`notification_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`receiver_id`	BIGINT	NOT NULL,
	`type`	VARCHAR(50)	NOT NULL,
	`message`	TEXT	NOT NULL,
	`target_url`	VARCHAR(255)	NULL,
	`is_read`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_member` (
	`crew_member_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`member_id`	BIGINT	NOT NULL,
	`group_id`	BIGINT	NOT NULL,
	`member_role`	ENUM('OWNER', 'MANAGER', 'MEMBER')	NOT NULL	DEFAULT 'MEMBER',
	`sequence_number`	INT	NULL,
	`membership_status`	ENUM('PENDING', 'APPROVED', 'REJECTED', 'LEFT', 'EXPELLED')	NOT NULL	DEFAULT 'PENDING',
	`approved_by` BIGINT NULL,
    `approved_at` DATETIME NULL,
    `joined_at`	DATETIME	NULL,
	`left_at`	DATETIME	NULL,
	`application_message`	VARCHAR(500)	NULL,
	`requested_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`crew_member_id`),
    UNIQUE KEY `uq_group_member_member_group` (`member_id`, `group_id`),
    KEY `fk_group_member_approved_by` (`approved_by`),
    KEY `ix_group_member_group_status` (`group_id`, `membership_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `groups` (
    `group_id` BIGINT NOT NULL AUTO_INCREMENT,
    `region_id` BIGINT NOT NULL,
    `member_id` BIGINT NOT NULL,
    `group_name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(50) NULL,
    `max_members` INT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saving_group` (
	`saving_group_id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `saving_group_type` VARCHAR(50) NULL, 
    `dues_amount` DECIMAL(12,2) NULL, 
    `dues_day` TINYINT NULL, 
    `start_date` DATE NULL,
    `end_date` DATE NULL, 
    `status` VARCHAR(20) NOT NULL, 
    `group_image` VARCHAR(255) NULL,
    PRIMARY KEY (`saving_group_id`),
    CONSTRAINT `chk_saving_group_dues_day`
		CHECK(`dues_day` BETWEEN 1 AND 28)
    ) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4
    COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saving_group_setting_change` (
    `setting_change_id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `dues_amount` DECIMAL(12,2) NULL,
    `dues_day` TINYINT NULL,
    `effective_at` DATETIME NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `applied_at` DATETIME NULL,
    `canceled_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`setting_change_id`),
    UNIQUE KEY `uq_saving_group_setting_change_group_effective_at`
(`group_id`, `effective_at`),
    CONSTRAINT `chk_saving_group_setting_change_dues_day`
    CHECK (`dues_day` IS NULL OR `dues_day` BETWEEN 1 AND 28),
    CONSTRAINT `chk_saving_group_setting_change_status`
    CHECK (`status` IN ('PENDING', 'HELD', 'CANCELED', 'APPLIED')),
    CONSTRAINT `fk_saving_group_setting_change_group`
    FOREIGN KEY (`group_id`) REFERENCES `groups` (`group_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `small_group` (
	`small_group_id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `like_category` VARCHAR(50) NULL,
    `meet_date` DATE NULL, 
    `location` VARCHAR(40) NULL,
    PRIMARY KEY (`small_group_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;
    
CREATE TABLE IF NOT EXISTS `term` (
	`term_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`title`	VARCHAR(200)	NOT NULL,
	`version`	VARCHAR(20)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`is_required`	BOOLEAN	NOT NULL,
	PRIMARY KEY (`term_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `report_case` (
    `case_id` BIGINT NOT NULL AUTO_INCREMENT,
    `report_type` ENUM(
        'BOARD_POST',
        'BOARD_COMMENT',
        'BOARD_MEMBER',
        'MEETING',
        'MEETING_MEMBER'
) NOT NULL,
`target_id` BIGINT NOT NULL,
`reported_member_id` BIGINT NOT NULL,
`status` ENUM(
    'PENDING',
    'CONFIRMED',
    'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',
    `process_reason` VARCHAR(1000) NULL DEFAULT NULL,
    `processed_at` DATETIME NULL,
    `restricted_until` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`case_id`),
    KEY `ix_report_case_target` (
        `report_type`, `target_id`, `status`
        ),
    KEY `ix_report_case_violation` (
        `reported_member_id`, `report_type`, `status`, `processed_at`
        ),
    KEY `ix_report_case_restriction` (
        `reported_member_id`, `report_type`, `restricted_until`
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `member_report` (
    `report_id` BIGINT NOT NULL AUTO_INCREMENT,
    `case_id` BIGINT NOT NULL,
    `reporter_id` BIGINT NOT NULL,
    `reason` TEXT NOT NULL,
    `process_reason` VARCHAR(500) NULL DEFAULT NULL,
    `reported_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `evidence_files` JSON NULL,
    `evidence_deleted_at` DATETIME NULL,
    PRIMARY KEY (`report_id`),
    UNIQUE KEY `uq_member_report_case_reporter` (
        `case_id`, `reporter_id`
        ),
    KEY `FK_members_TO_member_report_1` (`reporter_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `email_verification` (
	`verification_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`email`	VARCHAR(40)	NOT NULL,
	`code_hash`	VARCHAR(255)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`expires_at`	DATETIME	NOT NULL,
	`verified_at`	DATETIME	NULL,
	PRIMARY KEY (`verification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `region` (
	`region_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`region_name`	VARCHAR(100)	NOT NULL,
	PRIMARY KEY (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `meeting_round` (
	`round_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`group_id`	BIGINT	NOT NULL,
	`round_number`	INT	NOT NULL,
	`due_date`	DATE	NOT NULL,
	`scheduled_payout_date`	DATE	NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payout_request` (
	`payout_request_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`round_id`	BIGINT	NOT NULL,
	`requester_id`	BIGINT	NOT NULL,
	`reviewer_id`	BIGINT	NULL,
	`transaction_id`	BIGINT	NULL,
	`amount`	DECIMAL(15,2)	NOT NULL,
	`status`	ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')	NOT NULL	DEFAULT 'PENDING',
	`requested_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`reviewed_at`	DATETIME	NULL,
	`completed_at`	DATETIME	NULL,
	`reject_reason`	VARCHAR(500)	NULL,
	PRIMARY KEY (`payout_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `expenses` (
	`expense_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`group_id`	BIGINT	NOT NULL,
	`applicant_id`	BIGINT	NOT NULL,
	`amount`	DECIMAL(15,2)	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`receipt_img_url`	VARCHAR(500)	NULL,
	`status`	ENUM('PENDING', 'APPROVED', 'REJECTED')	NOT NULL	DEFAULT 'PENDING',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`expense_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `interest` (
	`interest_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`interest_name`	VARCHAR(50)	NOT NULL,
	PRIMARY KEY (`interest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_transaction` (
    `transaction_id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT NOT NULL,
    `transaction_type` ENUM('DEPOSIT', 'WITHDRAWAL') NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `balance_after` DECIMAL(15,2) NULL,
    `source_provider` VARCHAR(50) NULL,
    `source_transaction_id` VARCHAR(128) NULL,
    `transacted_at` DATETIME NOT NULL,
    `description` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`transaction_id`),
    UNIQUE KEY `uq_group_transaction_source`
(`account_id`, `source_provider`, `source_transaction_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `transaction_anomaly_analysis` (
    `transaction_id` BIGINT NOT NULL,
    `revision_no` INT NOT NULL,
    `input_fingerprint` CHAR(64) NOT NULL,
    `status` ENUM('PENDING', 'COMPLETED', 'FAILED')
    NOT NULL DEFAULT 'PENDING',
    `anomaly_score` DOUBLE NULL,
    `threshold` DOUBLE NULL,
    `is_anomaly` BOOLEAN NULL,
    `cold_start` BOOLEAN NULL,
    `quality_flags` JSON NOT NULL,
    `model_version` VARCHAR(128) NULL,
    `feature_schema_version` VARCHAR(64) NULL,
    `analyzed_at` DATETIME(6) NULL,
    `error_code` VARCHAR(64) NULL,
    `created_at` DATETIME(6) NOT NULL
    DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`transaction_id`, `revision_no`),
    KEY `ix_transaction_anomaly_analysis_status_created`
(`status`, `created_at`),
    CONSTRAINT `fk_transaction_anomaly_analysis_transaction`
    FOREIGN KEY (`transaction_id`)
    REFERENCES `group_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post` (
	`post_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`writer_id`	BIGINT	NOT NULL,
	`category_id`	BIGINT	NOT NULL,
	`title`	VARCHAR(200)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`view_count`	INT	NOT NULL	DEFAULT 0,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deleted_at`	DATETIME	NULL,
    `updated_at` DATETIME NULL,
	PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `member_interest` ADD CONSTRAINT `FK_interest_TO_member_interest_1` FOREIGN KEY (
	`interest_id`
)
REFERENCES `interest` (
	`interest_id`
);

ALTER TABLE `member_interest` ADD CONSTRAINT `FK_members_TO_member_interest_1` FOREIGN KEY (
	`member_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `comment` ADD CONSTRAINT `FK_post_TO_comment_1` FOREIGN KEY (
	`post_id`
)
REFERENCES `post` (
	`post_id`
);

ALTER TABLE `comment` ADD CONSTRAINT `FK_members_TO_comment_1` FOREIGN KEY (
	`writer_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `comment` ADD CONSTRAINT `FK_comment_TO_comment_1` FOREIGN KEY (
	`parent_id`
)
REFERENCES `comment` (
	`comment_id`
);

ALTER TABLE `group_event`
ADD CONSTRAINT `FK_groups_TO_group_event_1`
FOREIGN KEY (`group_id`)
REFERENCES `groups` (`group_id`);

ALTER TABLE `group_event` ADD CONSTRAINT `FK_members_TO_group_event_1` FOREIGN KEY (
	`writer_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `group_account` 
ADD CONSTRAINT `FK_groups_TO_group_account_1`
FOREIGN KEY (`group_id`)
REFERENCES `groups` (`group_id`);

ALTER TABLE `term_agreement` ADD CONSTRAINT `FK_term_TO_term_agreement_1` FOREIGN KEY (
	`term_id`
)
REFERENCES `term` (
	`term_id`
);

ALTER TABLE `term_agreement` ADD CONSTRAINT `FK_members_TO_term_agreement_1` FOREIGN KEY (
	`member_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `fee_payment` ADD CONSTRAINT `FK_groups_TO_fee_payment_1` FOREIGN KEY (
	`group_id`
)
REFERENCES `groups` (
	`group_id`
);

ALTER TABLE `fee_payment` ADD CONSTRAINT `FK_members_TO_fee_payment_1` FOREIGN KEY (
	`member_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `fee_payment` ADD CONSTRAINT `FK_meeting_round_TO_fee_payment_1` FOREIGN KEY (
	`round_id`
)
REFERENCES `meeting_round` (
	`round_id`
);

ALTER TABLE `members` ADD CONSTRAINT `FK_region_TO_members_1` FOREIGN KEY (
	`region_id`
)
REFERENCES `region` (
	`region_id`
);

ALTER TABLE `meeting_approval` ADD CONSTRAINT `FK_meeting_TO_meeting_approval_1` FOREIGN KEY (
	`group_id`
)
REFERENCES `groups` (
	`group_id`
);

ALTER TABLE `group_notice` ADD CONSTRAINT `FK_groups_TO_group_notice_1` FOREIGN KEY (
	`group_id`
)
REFERENCES `groups` (
	`group_id`
);

ALTER TABLE `group_notice` ADD CONSTRAINT `FK_members_TO_group_notice_1` FOREIGN KEY (
	`writer_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `notification` ADD CONSTRAINT `FK_members_TO_notification_1` FOREIGN KEY (
	`receiver_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `group_member` ADD CONSTRAINT `FK_groups_TO_group_member_1` FOREIGN KEY (
	`group_id`
)
REFERENCES `groups` (
	`group_id`
);

ALTER TABLE `group_member` ADD CONSTRAINT `FK_members_TO_group_member_1` FOREIGN KEY (
	`member_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `group_member`
ADD CONSTRAINT `fk_group_member_approved_by`
FOREIGN KEY (`approved_by`)
REFERENCES `members` (`member_id`);

ALTER TABLE `groups`
ADD CONSTRAINT `FK_region_TO_groups_1`
FOREIGN KEY (`region_id`)
REFERENCES `region` (`region_id`);

ALTER TABLE `groups`
ADD CONSTRAINT `FK_members_TO_groups_1`
FOREIGN KEY (`member_id`)
REFERENCES `members` (`member_id`);

ALTER TABLE `saving_group`
ADD CONSTRAINT `FK_groups_TO_saving_group_1`
FOREIGN KEY (`group_id`)
REFERENCES `groups` (`group_id`);

ALTER TABLE `small_group`
ADD CONSTRAINT `FK_groups_TO_small_group_1`
FOREIGN KEY (`group_id`)
REFERENCES `groups` (`group_id`);

ALTER TABLE `report_case` ADD CONSTRAINT `fk_report_case_reported_member` FOREIGN KEY (
    `reported_member_id`
    )
REFERENCES `members` (
    `member_id`
);

ALTER TABLE `member_report` ADD CONSTRAINT `fk_member_report_case` FOREIGN KEY (
    `case_id`
)
REFERENCES `report_case` (
    `case_id`
);

ALTER TABLE `member_report` ADD CONSTRAINT `FK_members_TO_member_report_1` FOREIGN KEY (
	`reporter_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `meeting_round` ADD CONSTRAINT `FK_groups_TO_meeting_round_1` FOREIGN KEY (
	`group_id`
)
REFERENCES `groups` (
	`group_id`
);

ALTER TABLE `payout_request` ADD CONSTRAINT `FK_meeting_round_TO_payout_request_1` FOREIGN KEY (
	`round_id`
)
REFERENCES `meeting_round` (
	`round_id`
);

ALTER TABLE `payout_request` ADD CONSTRAINT `FK_members_TO_payout_request_1` FOREIGN KEY (
	`requester_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `payout_request` ADD CONSTRAINT `FK_members_TO_payout_request_2` FOREIGN KEY (
	`reviewer_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `payout_request` ADD CONSTRAINT `FK_group_transaction_TO_payout_request_1` FOREIGN KEY (
	`transaction_id`
)
REFERENCES `group_transaction` (
	`transaction_id`
);

ALTER TABLE `expenses` ADD CONSTRAINT `FK_groups_TO_expenses_1` FOREIGN KEY (
	`group_id`
)
REFERENCES `groups` (
	`group_id`
);

ALTER TABLE `expenses` ADD CONSTRAINT `FK_members_TO_expenses_1` FOREIGN KEY (
	`applicant_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `group_transaction` ADD CONSTRAINT `FK_group_account_TO_group_transaction_1` FOREIGN KEY (
	`account_id`
)
REFERENCES `group_account` (
	`account_id`
);

ALTER TABLE `post` ADD CONSTRAINT `FK_members_TO_post_1` FOREIGN KEY (
	`writer_id`
)
REFERENCES `members` (
	`member_id`
);

ALTER TABLE `post` ADD CONSTRAINT `FK_post_category_TO_post_1` FOREIGN KEY (
	`category_id`
)
REFERENCES `post_category` (
	`category_id`
);

-- Additional UNIQUE constraints prepared for execution in MySQL.
ALTER TABLE `members`
ADD CONSTRAINT `uq_members_email`
UNIQUE (`email`);

ALTER TABLE `meeting_round`
ADD CONSTRAINT `uq_meeting_round`
UNIQUE (`group_id`, `round_number`);
