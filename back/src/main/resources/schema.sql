-- Moa schema submission draft, 2026-10-08.
-- Select an empty MySQL database before executing this file.
-- 28 tables, 40 foreign keys, 6 UNIQUE constraints, 3 CHECK constraint.
-- Single-column numeric primary keys use AUTO_INCREMENT.
-- ERDCloud export supplemented in SQL; not applied to ERDCloud.
-- Structural validation only; execution in MySQL is still pending.

CREATE TABLE IF NOT EXISTS `member_interest` (
	`interest_id`	BIGINT	NOT NULL COMMENT '관심사 ID',
	`member_id`	BIGINT	NOT NULL COMMENT '회원 ID',
	PRIMARY KEY (`interest_id`,
	`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 관심사';

CREATE TABLE IF NOT EXISTS `comment` (
	`comment_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '댓글 ID',
	`post_id`	BIGINT	NOT NULL COMMENT '게시글 ID',
	`writer_id`	BIGINT	NOT NULL COMMENT '작성 회원 ID',
	`parent_id`	BIGINT	NULL COMMENT '부모 댓글 ID',
	`content`	TEXT	NOT NULL COMMENT '댓글 내용',
	`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
		ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
	`deleted_at` DATETIME NULL COMMENT '삭제 일시',
	PRIMARY KEY (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='댓글';

CREATE TABLE IF NOT EXISTS `group_event` (
	`schedule_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '모임 일정 ID',
	`group_id`	BIGINT	NOT NULL COMMENT '모임 ID',
	`writer_id`	BIGINT	NOT NULL COMMENT '작성 회원 ID',
	`title`	VARCHAR(100)	NOT NULL COMMENT '제목',
	`location`	VARCHAR(200)	NULL COMMENT '모임 장소',
	`event_date`	DATETIME	NOT NULL COMMENT '일정 일시',
	PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 일정';

CREATE TABLE IF NOT EXISTS `post_category` (
	`category_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
	`category_name`	VARCHAR(50)	NOT NULL COMMENT '카테고리 이름',
	PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글 카테고리';

CREATE TABLE IF NOT EXISTS `group_account` (
	`account_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '모임 계좌 ID',
	`group_id`	BIGINT	NOT NULL COMMENT '모임 ID',
	`bank_code`	VARCHAR(20)	NOT NULL COMMENT '은행 코드',
	`account_number`	VARCHAR(255)	NOT NULL COMMENT '계좌번호',
	`account_holder_name`	VARCHAR(100)	NOT NULL COMMENT '예금주 이름',
	`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    PRIMARY KEY (`account_id`),
    UNIQUE KEY `uq_group_account_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 계좌';


CREATE TABLE IF NOT EXISTS `saving_group_account_issue` (
    `issue_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '계좌 발급 이력 ID',
    `approval_id` BIGINT NOT NULL COMMENT '모임 심사 ID',
    `group_id` BIGINT NOT NULL COMMENT '모임 ID',
    `order_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '계좌 발급 주문 ID',
    `status` ENUM('REQUESTED', 'COMPLETED', 'FAILED', 'UNKNOWN') NOT NULL COMMENT '처리 상태',
    `account_id` BIGINT NULL DEFAULT NULL COMMENT '모임 계좌 ID',
    `error_code` VARCHAR(100) NULL DEFAULT NULL COMMENT '오류 코드',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    PRIMARY KEY (`issue_id`),
    UNIQUE KEY `uq_account_issue_approval` (`approval_id`),
    UNIQUE KEY `uq_account_issue_group` (`group_id`),
    UNIQUE KEY `uq_account_issue_order` (`order_id`),
    UNIQUE KEY `uq_account_issue_account` (`account_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계모임 계좌 발급 이력';

CREATE TABLE IF NOT EXISTS `term_agreement` (
	`agreement_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '약관 동의 ID',
	`term_id`	BIGINT	NOT NULL COMMENT '약관 ID',
	`member_id`	BIGINT	NOT NULL COMMENT '회원 ID',
	`agreed_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP COMMENT '동의 일시',
	PRIMARY KEY (`agreement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 약관 동의';

CREATE TABLE IF NOT EXISTS `fee_payment` (
	`dues_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '회비 납부 내역 ID',
	`group_id`	BIGINT	NOT NULL COMMENT '모임 ID',
	`member_id`	BIGINT	NOT NULL COMMENT '회원 ID',
	`round_id`	BIGINT	NULL COMMENT '모임 회차 ID',
	`amount`	DECIMAL(15,2)	NOT NULL COMMENT '금액',
	`due_date`	DATE	NOT NULL COMMENT '납부 기한',
	`is_paid`	BOOLEAN	NOT NULL	DEFAULT FALSE COMMENT '납부 여부',
	`paid_at`	DATETIME	NULL COMMENT '납부 일시',
	PRIMARY KEY (`dues_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회비 납부 내역';

CREATE TABLE IF NOT EXISTS `members` (
	`member_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
	`region_id`	BIGINT NULL COMMENT '활동 지역 ID',
	`email`	VARCHAR(40)	NULL COMMENT '이메일',
	`password_hash`	VARCHAR(255)	NULL COMMENT '비밀번호 해시',
	`name`	VARCHAR(50)	NOT NULL COMMENT '이름',
	`nickname`	VARCHAR(50)	NOT NULL COMMENT '닉네임',
	`phone`	VARCHAR(20) NULL COMMENT '전화번호',
	`birth_date` DATE NULL COMMENT '생년월일',
	`gender` ENUM('MALE', 'FEMALE')	 NULL COMMENT '성별',
	`profile_img_url`	VARCHAR(500)	NULL COMMENT '프로필 이미지 주소',
	`user_level`	VARCHAR(20)	NULL COMMENT '회원 등급',
	`system_role`	ENUM('USER',  'ADMIN')	NOT NULL	DEFAULT 'USER' COMMENT '시스템 권한',
	`identity_verified_at`	DATETIME	NULL COMMENT '본인인증 완료 일시',
	`region_verified_at`	DATETIME	NULL COMMENT '지역 인증 완료 일시',
	`account_status`	ENUM('ACTIVE', 'SUSPENDED',  'INACTIVE')	NOT NULL	DEFAULT 'ACTIVE' COMMENT '계정 상태',
    `suspended_until` DATETIME NULL DEFAULT NULL COMMENT '계정 정지 종료 일시',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	`deleted_at`	DATETIME	NULL COMMENT '삭제 일시',
	PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원';

CREATE TABLE IF NOT EXISTS `member_feature_restriction` (
    `member_id` BIGINT NOT NULL COMMENT '회원 ID',
    `restriction_scope` VARCHAR(30) NOT NULL COMMENT '이용 제한 범위',
    `restricted_until` DATETIME NULL DEFAULT NULL COMMENT '이용 제한 종료 일시',

    PRIMARY KEY (`member_id`, `restriction_scope`),

    CONSTRAINT `fk_member_feature_restriction_member`
        FOREIGN KEY (`member_id`)
        REFERENCES `members` (`member_id`),

    CONSTRAINT `chk_member_feature_restriction_scope`
        CHECK (`restriction_scope` IN ('CONTENT', 'CHAT', 'REPORT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci COMMENT='회원 기능별 이용 제한';

CREATE TABLE IF NOT EXISTS member_auth_session (
    session_id CHAR(36)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '로그인 세션 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    expires_at_ms BIGINT NOT NULL COMMENT '세션 만료 시각 밀리초',
    revoked_at DATETIME(6) NULL COMMENT '세션 폐기 일시',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',

    PRIMARY KEY (session_id),
    KEY ix_member_auth_session_member (member_id),
    KEY ix_member_auth_session_expires (expires_at_ms),

    CONSTRAINT fk_member_auth_session_member
        FOREIGN KEY (member_id)
        REFERENCES members (member_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci COMMENT='회원 로그인 세션';

CREATE  TABLE IF NOT EXISTS member_refresh_token (
    token_hash CHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '리프레시 토큰 해시',
    session_id CHAR(36)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '로그인 세션 ID',
    used_at DATETIME(6) NULL COMMENT '토큰 사용 일시',
    created_at DATETIME(6) NOT NULL DEFAULT
CURRENT_TIMESTAMP(6) COMMENT '생성 일시',

    PRIMARY KEY (token_hash),
    KEY ix_member_refresh_token_session (session_id),

    CONSTRAINT fk_member_refresh_token_session
        FOREIGN KEY (session_id)
        REFERENCES member_auth_session (session_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci COMMENT='회원 리프레시 토큰';

CREATE TABLE IF NOT EXISTS
member_identity_verification_request (
    identity_verification_id VARCHAR(128)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '본인인증 요청 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    created_at DATETIME(6) NOT NULL DEFAULT
CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    expires_at DATETIME(6) NOT NULL COMMENT '만료 일시',
    consumed_at DATETIME(6) NULL COMMENT '인증 결과 사용 완료 일시',

    PRIMARY KEY (identity_verification_id),
    KEY ix_member_identity_verification_request_member(member_id),

    CONSTRAINT
fk_member_identity_verification_request_member
        FOREIGN KEY (member_id)
        REFERENCES members (member_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci COMMENT='회원 본인인증 요청';

CREATE TABLE IF NOT EXISTS member_social_account (
    social_account_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '소셜 계정 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    provider VARCHAR(20)
    CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '소셜 로그인 제공자',
    provider_user_id VARCHAR(255)
    CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '소셜 로그인 제공자의 회원 식별자',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

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
    COLLATE=utf8mb4_unicode_ci COMMENT='회원 소셜 계정';

CREATE TABLE IF NOT EXISTS `meeting_approval` (
	`approval_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '모임 심사 ID',
	`group_id`	BIGINT	NOT NULL COMMENT '모임 ID',
	`type`	ENUM('CREATE', 'DELETE')	NOT NULL COMMENT '모임 개설 또는 삭제 요청 구분',
	`bank_book_url`	VARCHAR(500)	NULL COMMENT '통장 사본 주소',
	`status`	ENUM('PENDING','APPROVED','REJECTED')	NOT NULL	DEFAULT 'PENDING' COMMENT '처리 상태',
    `request_reason` VARCHAR(500) NULL COMMENT '요청 사유',
    `reject_reason`	VARCHAR(255)	NULL COMMENT '반려 사유',
	`processed_at`	DATETIME	NULL COMMENT '처리 일시',
	PRIMARY KEY (`approval_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 개설 및 삭제 심사';

CREATE TABLE IF NOT EXISTS `group_notice` (
	`notice_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '공지사항 ID',
	`group_id`	BIGINT	NOT NULL COMMENT '모임 ID',
	`writer_id`	BIGINT	NOT NULL COMMENT '작성 회원 ID',
	`title`	VARCHAR(200)	NOT NULL COMMENT '제목',
	`content`	TEXT	NOT NULL COMMENT '공지사항 내용',
    `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '상단 고정 여부',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `updated_at` DATETIME NULL DEFAULT NULL COMMENT '수정 일시',
    `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '삭제 일시',
	PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 공지사항';

CREATE TABLE IF NOT EXISTS `notification` (
	`notification_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
	`receiver_id`	BIGINT	NOT NULL COMMENT '수신 회원 ID',
	`type`	VARCHAR(50)	NOT NULL COMMENT '유형',
	`message`	TEXT	NOT NULL COMMENT '알림 내용',
	`target_url`	VARCHAR(255)	NULL COMMENT '이동할 화면 주소',
	`is_read`	BOOLEAN	NOT NULL	DEFAULT FALSE COMMENT '읽음 여부',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	PRIMARY KEY (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림';

CREATE TABLE IF NOT EXISTS `group_member` (
	`crew_member_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '모임 회원 연결 ID',
	`member_id`	BIGINT	NOT NULL COMMENT '회원 ID',
	`group_id`	BIGINT	NOT NULL COMMENT '모임 ID',
	`member_role`	ENUM('OWNER', 'MANAGER', 'MEMBER')	NOT NULL	DEFAULT 'MEMBER' COMMENT '모임 내 역할',
	`sequence_number`	INT	NULL COMMENT '회원 순번',
	`membership_status`	ENUM('PENDING', 'APPROVED', 'REJECTED', 'LEFT', 'EXPELLED')	NOT NULL	DEFAULT 'PENDING' COMMENT '가입 상태',
	`approved_by` BIGINT NULL COMMENT '승인 회원 ID',
    `approved_at` DATETIME NULL COMMENT '승인 일시',
    `joined_at`	DATETIME	NULL COMMENT '가입 일시',
	`left_at`	DATETIME	NULL COMMENT '탈퇴 일시',
	`application_message`	VARCHAR(500)	NULL COMMENT '가입 신청 메시지',
	`requested_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP COMMENT '요청 일시',
	PRIMARY KEY (`crew_member_id`),
    UNIQUE KEY `uq_group_member_member_group` (`member_id`, `group_id`),
    KEY `fk_group_member_approved_by` (`approved_by`),
    KEY `ix_group_member_group_status` (`group_id`, `membership_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 회원';

CREATE TABLE IF NOT EXISTS `groups` (
    `group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '모임 ID',
    `region_id` BIGINT NOT NULL COMMENT '활동 지역 ID',
    `member_id` BIGINT NOT NULL COMMENT '모임 생성 회원 ID',
    `group_name` VARCHAR(100) NOT NULL COMMENT '모임 이름',
    `description` VARCHAR(50) NULL COMMENT '설명',
    `max_members` INT NULL COMMENT '최대 정원',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `deleted_at` DATETIME NULL COMMENT '삭제 일시',
    PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임';

CREATE TABLE IF NOT EXISTS `saving_group` (
	`saving_group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '계모임 ID',
    `group_id` BIGINT NOT NULL COMMENT '모임 ID',
    `interest_id` BIGINT NULL COMMENT '관심사 ID',
    `saving_group_type` VARCHAR(50) NULL COMMENT '계모임 유형',
    `dues_amount` DECIMAL(12,2) NULL COMMENT '회비 금액',
    `dues_day` TINYINT NULL COMMENT '회비 납부일',
    `start_date` DATE NULL COMMENT '운영 시작일',
    `end_date` DATE NULL COMMENT '운영 종료일',
    `status` VARCHAR(20) NOT NULL COMMENT '계모임 운영 상태',
    `group_image` VARCHAR(255) NULL COMMENT '모임 이미지 주소',
    PRIMARY KEY (`saving_group_id`),
    CONSTRAINT `chk_saving_group_dues_day`
		CHECK(`dues_day` BETWEEN 1 AND 28)
    ) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4
    COLLATE=utf8mb4_unicode_ci COMMENT='계모임';

-- 계모임의 적용월별 예약 순번 목록을 저장
CREATE TABLE IF NOT EXISTS `saving_group_turn_order_plan` (
    `turn_order_plan_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '지급 순번 예약 ID',
    `saving_group_id` BIGINT NOT NULL COMMENT '계모임 ID',
    `effective_month` DATE NOT NULL COMMENT '적용 월',
    `start_round_number` INT NOT NULL COMMENT '적용 시작 회차 번호',
    `assigned_by` BIGINT NOT NULL COMMENT '순번 지정 회원 ID',
    `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '순번 지정 일시',

    PRIMARY KEY (`turn_order_plan_id`),
    UNIQUE KEY `uq_turn_order_plan_group_month`
        (`saving_group_id`, `effective_month`),
    KEY `ix_turn_order_plan_assigned_by` (`assigned_by`),

    CONSTRAINT `fk_turn_order_plan_saving_group`
        FOREIGN KEY (`saving_group_id`)
        REFERENCES `saving_group` (`saving_group_id`),
    CONSTRAINT `fk_turn_order_plan_assigned_by`
        FOREIGN KEY (`assigned_by`)
     REFERENCES `members` (`member_id`),

    CONSTRAINT `chk_turn_order_plan_month`
        CHECK (DAYOFMONTH(`effective_month`) = 1),
    CONSTRAINT `chk_turn_order_plan_start`
        CHECK (`start_round_number` >= 1)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계모임 지급 순번 예약';

-- 예약 목록별 회원의 실제 지급 회차를 저장
CREATE TABLE IF NOT EXISTS `saving_group_turn_order_plan_member` (
    `turn_order_plan_id` BIGINT NOT NULL COMMENT '지급 순번 예약 ID',
    `member_id` BIGINT NOT NULL COMMENT '회원 ID',
    `round_number` INT NOT NULL COMMENT '회차 번호',

    PRIMARY KEY (`turn_order_plan_id`, `member_id`),
    UNIQUE KEY `uq_turn_order_plan_round`
        (`turn_order_plan_id`, `round_number`),
    KEY `ix_turn_order_plan_member` (`member_id`),

    CONSTRAINT `fk_turn_order_plan_member_plan`
        FOREIGN KEY (`turn_order_plan_id`)
        REFERENCES `saving_group_turn_order_plan` (`turn_order_plan_id`),
    CONSTRAINT `fk_turn_order_plan_member_member`
        FOREIGN KEY (`member_id`)
        REFERENCES `members` (`member_id`),

    CONSTRAINT `chk_turn_order_plan_member_round`
        CHECK (`round_number` >= 1)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계모임 지급 순번 예약 회원';

CREATE TABLE IF NOT EXISTS `restriction_history` (
    `restriction_history_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '이용 제한 이력 ID',
    `member_id` BIGINT NULL COMMENT '회원 ID',
    `saving_group_id` BIGINT NULL COMMENT '계모임 ID',
    `small_group_id` BIGINT NULL COMMENT '소모임 ID',
    `action` ENUM('RESTRICT', 'RELEASE') NOT NULL COMMENT '제한 또는 해제 구분',
    `restriction_scope` VARCHAR(30) NULL DEFAULT NULL COMMENT '이용 제한 범위',
    `restriction_type` ENUM('7_DAYS', '30_DAYS', 'PERMANENT') NULL COMMENT '이용 제한 기간 유형',
    `reason` VARCHAR(500) NOT NULL COMMENT '사유',
    `restricted_until` DATETIME NULL COMMENT '이용 제한 종료 일시',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

    PRIMARY KEY (`restriction_history_id`),

    CONSTRAINT `chk_restriction_history_target`
    CHECK (
(`member_id` IS NOT NULL)
    + (`saving_group_id` IS NOT NULL)
    + (`small_group_id` IS NOT NULL) = 1
    ),

    CONSTRAINT `fk_restriction_history_member`
    FOREIGN KEY (`member_id`)
    REFERENCES `members` (`member_id`),

    CONSTRAINT `fk_restriction_history_saving_group`
    FOREIGN KEY (`saving_group_id`)
    REFERENCES `saving_group` (`saving_group_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이용 제한 및 해제 이력';

CREATE TABLE IF NOT EXISTS `saving_group_setting_change` (
    `setting_change_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '회비 설정 변경 예약 ID',
    `group_id` BIGINT NOT NULL COMMENT '모임 ID',
    `dues_amount` DECIMAL(12,2) NULL COMMENT '회비 금액',
    `dues_day` TINYINT NULL COMMENT '회비 납부일',
    `effective_at` DATETIME NOT NULL COMMENT '설정 적용 예정 일시',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태',
    `applied_at` DATETIME NULL COMMENT '설정 적용 일시',
    `canceled_at` DATETIME NULL COMMENT '취소 일시',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    PRIMARY KEY (`setting_change_id`),
    UNIQUE KEY `uq_saving_group_setting_change_group_effective_at`
(`group_id`, `effective_at`),
    CONSTRAINT `chk_saving_group_setting_change_dues_day`
    CHECK (`dues_day` IS NULL OR `dues_day` BETWEEN 1 AND 28),
    CONSTRAINT `chk_saving_group_setting_change_status`
    CHECK (`status` IN ('PENDING', 'HELD', 'CANCELED', 'APPLIED')),
    CONSTRAINT `fk_saving_group_setting_change_group`
    FOREIGN KEY (`group_id`) REFERENCES `groups` (`group_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계모임 회비 설정 변경 예약';

CREATE TABLE IF NOT EXISTS `small_group` (
	`small_group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '소모임 ID',
    `group_id` BIGINT NOT NULL COMMENT '모임 ID',
    `like_category` VARCHAR(50) NULL COMMENT '관심 카테고리',
    `meet_date` DATE NULL COMMENT '모임 날짜',
    `location` VARCHAR(40) NULL COMMENT '모임 장소',
    `status` ENUM('ACTIVE', 'RESTRICTED') NOT NULL DEFAULT 'ACTIVE' COMMENT '소모임 운영 상태',
    PRIMARY KEY (`small_group_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci COMMENT='소모임';

ALTER TABLE `restriction_history`
    ADD CONSTRAINT `fk_restriction_history_small_group`
        FOREIGN KEY (`small_group_id`)
            REFERENCES `small_group` (`small_group_id`);

-- 소모임별 회원당 후기 한 건과 별점·내용을 검증
CREATE TABLE IF NOT EXISTS small_group_review (
    small_group_review_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '소모임 후기 ID',
    small_group_id BIGINT NOT NULL COMMENT '소모임 ID',
    writer_id BIGINT NOT NULL COMMENT '작성 회원 ID',
    rating DECIMAL(2,1) NOT NULL COMMENT '별점',
    content VARCHAR(30) NOT NULL COMMENT '후기 내용',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NULL COMMENT '수정 일시',

    PRIMARY KEY (small_group_review_id),

    UNIQUE KEY uq_small_group_review_group_writer
        (small_group_id, writer_id),

    CONSTRAINT fk_small_group_review_small_group
        FOREIGN KEY (small_group_id)
        REFERENCES small_group (small_group_id),

    CONSTRAINT fK_small_group_review_writer
        FOREIGN key (writer_id)
        REFERENCES members (member_id),

    CONSTRAINT chk_small_group_review_rating
        CHECK (
            rating BETWEEN 0.0 AND 5.0
            AND MOD(rating * 10, 5) = 0
            ),

    CONSTRAINT chk_small_group_review_content
        CHECK (
            CHAR_LENGTH(content) BETWEEN 1 AND 30
            AND CHAR_LENGTH(TRIM(content)) > 0
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci COMMENT='소모임 후기';

-- 소모임 투표의 기본 정보와 종료 이력을 저장
CREATE TABLE IF NOT EXISTS small_group_vote (
    small_group_vote_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '소모임 투표 ID',
    small_group_id BIGINT NOT NULL COMMENT '소모임 ID',
    creator_member_id BIGINT NOT NULL COMMENT '투표 생성 회원 ID',
    title VARCHAR(12) NOT NULL COMMENT '제목',
    is_multiple_choice BOOLEAN NOT NULL DEFAULT FALSE COMMENT '복수 선택 허용 여부',
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE COMMENT '익명 투표 여부',
    ends_at DATETIME NULL COMMENT '투표 종료 예정 일시',
    status ENUM('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN' COMMENT '투표 진행 상태',
    closed_at DATETIME NULL COMMENT '투표 종료 일시',
    closed_by_member_id BIGINT NULL COMMENT '투표 종료 처리 회원 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted_at DATETIME NULL DEFAULT NULL COMMENT '삭제 일시',

    PRIMARY KEY (small_group_vote_id),

    KEY ix_small_group_vote_group_status_ends
        (small_group_id, status, ends_at),

    CONSTRAINT fk_small_group_vote_small_group
        FOREIGN KEY (small_group_id)
        REFERENCES small_group (small_group_id),

    CONSTRAINT fk_small_group_vote_creator
        FOREIGN KEY (creator_member_id)
        REFERENCES members (member_id),

    CONSTRAINT fk_small_group_vote_closed_by
        FOREIGN KEY (closed_by_member_id)
        REFERENCES members (member_id),

    CONSTRAINT chk_small_group_vote_title
        CHECK (CHAR_LENGTH(TRIM(title)) BETWEEN 2 AND 12)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci COMMENT='소모임 투표';

-- 투표 보기와 표시 순서를 저장
CREATE TABLE IF NOT EXISTS small_group_vote_option (
    small_group_vote_option_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '투표 보기 ID',
    small_group_vote_id BIGINT NOT NULL COMMENT '소모임 투표 ID',
    option_content VARCHAR(12) NOT NULL COMMENT '투표 보기 내용',
    display_order TINYINT NOT NULL COMMENT '표시 순서',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

    PRIMARY KEY (small_group_vote_option_id),

    UNIQUE KEY uq_small_group_vote_option_order
        (small_group_vote_id, display_order),

    CONSTRAINT fk_small_group_vote_option_vote
        FOREIGN KEY (small_group_vote_id)
        REFERENCES small_group_vote (small_group_vote_id),

    CONSTRAINT chk_small_group_vote_option_content
        CHECK (CHAR_LENGTH(TRIM(option_content)) BETWEEN 2 AND 12)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci COMMENT='소모임 투표 보기';

-- 회원별 투표 참여 이력을 저장하고 중복 참여를 방지
CREATE TABLE IF NOT EXISTS small_group_vote_participation (
    small_group_vote_participation_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '투표 참여 ID',
    small_group_vote_id BIGINT NOT NULL COMMENT '소모임 투표 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    participated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '투표 참여 일시',

    PRIMARY KEY (small_group_vote_participation_id),

    UNIQUE KEY uq_small_group_vote_participation_member
        (small_group_vote_id, member_id),

    CONSTRAINT fk_small_group_vote_participation_vote
        FOREIGN KEY (small_group_vote_id)
        REFERENCES small_group_vote (small_group_vote_id),

    CONSTRAINT fk_small_group_vote_participation_member
        FOREIGN KEY (member_id)
        REFERENCES members (member_id)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci COMMENT='소모임 투표 참여';

-- 투표 참여 이력과 선택한 보기 연결
CREATE TABLE IF NOT EXISTS small_group_vote_selection (
    small_group_vote_participation_id BIGINT NOT NULL COMMENT '투표 참여 ID',
    small_group_vote_option_id BIGINT NOT NULL COMMENT '투표 보기 ID',

    PRIMARY KEY (
        small_group_vote_participation_id,
    small_group_vote_option_id
),

    CONSTRAINT fk_small_group_vote_selection_participation
        FOREIGN KEY (small_group_vote_participation_id)
        REFERENCES small_group_vote_participation (
            small_group_vote_participation_id
        ),

    CONSTRAINT fk_small_group_vote_selection_option
        FOREIGN KEY (small_group_vote_option_id)
        REFERENCES small_group_vote_option (
            small_group_vote_option_id
        )
) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci COMMENT='소모임 투표 선택';

CREATE TABLE IF NOT EXISTS `term` (
	`term_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '약관 ID',
	`title`	VARCHAR(200)	NOT NULL COMMENT '약관 제목',
	`version`	VARCHAR(20)	NOT NULL COMMENT '버전',
	`content`	TEXT	NOT NULL COMMENT '약관 본문',
	`is_required`	BOOLEAN	NOT NULL COMMENT '필수 동의 여부',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '사용 여부',
	PRIMARY KEY (`term_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='약관';

CREATE TABLE IF NOT EXISTS `report_case` (
    `case_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '신고 사건 ID',
    `report_type` ENUM(
        'POST',
        'COMMENT',
        'MEMBER',
        'GROUP',
        'GROUP_MEMBER',
        'CHAT_MESSAGE'
) NOT NULL COMMENT '신고 대상 유형',
`target_id` BIGINT NOT NULL COMMENT '신고 대상 ID',
`reported_member_id` BIGINT NOT NULL COMMENT '신고 대상 회원 ID',
`status` ENUM(
    'PENDING',
    'CONFIRMED',
    'REJECTED'
    ) NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태',
    `process_reason` VARCHAR(1000) NULL DEFAULT NULL COMMENT '처리 사유',
    `processed_at` DATETIME NULL COMMENT '처리 일시',
    `restricted_until` DATETIME NULL COMMENT '이용 제한 종료 일시',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='신고 사건';

CREATE TABLE IF NOT EXISTS `member_report` (
    `report_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원 신고 ID',
    `case_id` BIGINT NOT NULL COMMENT '신고 사건 ID',
    `reporter_id` BIGINT NOT NULL COMMENT '신고 회원 ID',
    `reason` TEXT NOT NULL COMMENT '사유',
    `process_reason` VARCHAR(500) NULL DEFAULT NULL COMMENT '처리 사유',
    `reported_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신고 일시',
    `evidence_files` JSON NULL COMMENT '증빙 파일 정보',
    `evidence_deleted_at` DATETIME NULL COMMENT '증빙 삭제 일시',
    PRIMARY KEY (`report_id`),
    UNIQUE KEY `uq_member_report_case_reporter` (
        `case_id`, `reporter_id`
        ),
    KEY `FK_members_TO_member_report_1` (`reporter_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 신고 내역';

CREATE TABLE IF NOT EXISTS `email_verification` (
    `verification_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '이메일 인증 ID',
    `email` VARCHAR(40) NOT NULL COMMENT '이메일',
    `purpose` VARCHAR(20) NOT NULL DEFAULT 'SIGNUP' COMMENT '인증 목적',
    `code_hash` VARCHAR(255) NOT NULL COMMENT '인증번호 해시',
    `failed_attempts` INT NOT NULL DEFAULT 0 COMMENT '인증 실패 횟수',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `expires_at` DATETIME NOT NULL COMMENT '만료 일시',
    `verified_at` DATETIME NULL COMMENT '인증 완료 일시',
    `verification_token_hash` VARCHAR(64) NULL COMMENT '인증 토큰 해시',
    `token_expires_at` DATETIME NULL COMMENT '인증 토큰 만료 일시',
    `consumed_at` DATETIME NULL COMMENT '인증 토큰 사용 완료 일시',
    PRIMARY KEY (`verification_id`),
    CONSTRAINT `chk_email_verification_purpose`
    CHECK (`purpose` IN (
           'SIGNUP', 'FIND_ACCOUNT', 'RESET_PASSWORD'
            ))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이메일 인증';

CREATE TABLE IF NOT EXISTS `region` (
    `region_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '활동 지역 ID',
    `region_name` VARCHAR(100) NOT NULL COMMENT '전체 지역 이름',
    `province_name` VARCHAR(30) NULL COMMENT '시도 이름',
    `district_name` VARCHAR(50) NULL COMMENT '시군구 이름',
    `legal_code` VARCHAR(10) NULL COMMENT '법정동 코드',
    `town_name` VARCHAR(50) NULL COMMENT '읍면동 이름',
    PRIMARY KEY (`region_id`),
    UNIQUE KEY `uq_region_legal_code` (`legal_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci COMMENT='활동 지역';

CREATE TABLE IF NOT EXISTS `meeting_round` (
	`round_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '모임 회차 ID',
	`group_id` BIGINT NOT NULL COMMENT '모임 ID',
	`round_number` INT NOT NULL COMMENT '회차 번호',
	`due_date` DATE NOT NULL COMMENT '납부 기한',
	`scheduled_payout_date`	DATE NULL COMMENT '지급 예정일',
    `base_dues_amount` DECIMAL(15,2) NULL COMMENT '기본 회비 금액',
    `approved_member_count` INT NULL COMMENT '승인 회원 수',
    `previous_paid_member_count` INT NULL COMMENT '이전 납부 회원 수',
    `additional_dues_amount` DECIMAL(15,2) NULL COMMENT '추가 회비 금액',
    `payout_amount` DECIMAL(15,2) NULL COMMENT '지급 금액',
	`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	PRIMARY KEY (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 회차';

CREATE TABLE IF NOT EXISTS `payout_request` (
	`payout_request_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '계금 지급 요청 ID',
	`round_id`	BIGINT	NOT NULL COMMENT '모임 회차 ID',
	`requester_id`	BIGINT	NOT NULL COMMENT '요청 회원 ID',
	`reviewer_id`	BIGINT	NULL COMMENT '검토 회원 ID',
	`transaction_id`	BIGINT	NULL COMMENT '거래 ID',
	`amount`	DECIMAL(15,2)	NOT NULL COMMENT '금액',
	`status`	ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')	NOT NULL	DEFAULT 'PENDING' COMMENT '처리 상태',
	`requested_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP COMMENT '요청 일시',
	`reviewed_at`	DATETIME	NULL COMMENT '검토 일시',
	`completed_at`	DATETIME	NULL COMMENT '완료 일시',
	`reject_reason`	VARCHAR(500)	NULL COMMENT '반려 사유',
	PRIMARY KEY (`payout_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계금 지급 요청';

CREATE TABLE IF NOT EXISTS `expenses` (
	`expense_id` BIGINT	NOT NULL AUTO_INCREMENT COMMENT '지출 ID',
	`group_id`	BIGINT	NOT NULL COMMENT '모임 ID',
	`applicant_id` BIGINT NOT NULL COMMENT '지출 신청 회원 ID',
	`amount` DECIMAL(15,2) NOT NULL COMMENT '금액',
	`title`	VARCHAR(100) NOT NULL COMMENT '제목',
	`receipt_img_url` VARCHAR(500) NULL COMMENT '영수증 이미지 주소',
    `receipt_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '영수증 중복 확인용 해시',
	`status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태',
    `reviewer_id` BIGINT NULL DEFAULT NULL COMMENT '검토 회원 ID',
    `reviewed_at` DATETIME NULL DEFAULT NULL COMMENT '검토 일시',
    `reject_reason` VARCHAR(500) NULL DEFAULT NULL COMMENT '반려 사유',
	`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	PRIMARY KEY (`expense_id`),
    KEY `ix_expenses_group_receipt_status` (`group_id`, `receipt_sha256`,`status`),
    CONSTRAINT `fk_expenses_reviewer`
        FOREIGN KEY (`reviewer_id`) REFERENCES `members` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 지출';

CREATE TABLE IF NOT EXISTS `interest` (
	`interest_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '관심사 ID',
	`interest_name`	VARCHAR(50)	NOT NULL COMMENT '관심사 이름',
	PRIMARY KEY (`interest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='관심사';

CREATE TABLE IF NOT EXISTS `group_transaction` (
    `transaction_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '거래 ID',
    `account_id` BIGINT NOT NULL COMMENT '모임 계좌 ID',
    `transaction_type` ENUM('DEPOSIT', 'WITHDRAWAL') NOT NULL COMMENT '입금 또는 출금 구분',
    `amount` DECIMAL(15,2) NOT NULL COMMENT '금액',
    `balance_after` DECIMAL(15,2) NULL COMMENT '거래 후 잔액',
    `source_provider` VARCHAR(50) NULL COMMENT '거래 제공자',
    `source_transaction_id` VARCHAR(128) NULL COMMENT '거래 제공자의 거래 식별자',
    `transacted_at` DATETIME NOT NULL COMMENT '거래 일시',
    `description` VARCHAR(255) NULL COMMENT '설명',
    `final_category` VARCHAR(30) NULL COMMENT '사용자가 선택한 최종 지출 분류'
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

    PRIMARY KEY (`transaction_id`),
    UNIQUE KEY `uq_group_transaction_source`
(`account_id`, `source_provider`, `source_transaction_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='모임 계좌 거래 내역';

-- 테스트 입금 요청과 중복 확인 이력을 저장
CREATE TABLE IF NOT EXISTS `saving_group_mock_deposit_request` (
    `deposit_request_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '테스트 입금 요청 ID',
    `account_id` BIGINT NOT NULL COMMENT '모임 계좌 ID',
    `member_id` BIGINT NOT NULL COMMENT '회원 ID',
    `amount` DECIMAL(15,2) NOT NULL COMMENT '금액',
    `request_key` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '중복 요청 구분 키',
    `status` VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '처리 상태',
    `window_anchor_request_id` BIGINT NULL DEFAULT NULL COMMENT '중복 확인 기준 입금 요청 ID',
    `confirmation_token_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '중복 입금 확인 토큰 해시',
    `confirmation_expires_at` DATETIME(6) NULL DEFAULT NULL COMMENT '중복 입금 확인 만료 일시',
    `transaction_id` BIGINT NULL DEFAULT NULL COMMENT '거래 ID',
    `requested_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '요청 일시',
    `completed_at` DATETIME(6) NULL DEFAULT NULL COMMENT '완료 일시',

    PRIMARY KEY (`deposit_request_id`),
    UNIQUE KEY `uq_mock_dep_member_key` (`member_id`, `request_key`),
    UNIQUE KEY `uq_mock_dep_transaction` (`transaction_id`),
    UNIQUE KEY `uq_mock_dep_confirm_hash` (`confirmation_token_hash`),
    KEY `ix_mock_dep_recent`
(`account_id`, `member_id`, `amount`, `status`, `completed_at`),
    KEY `ix_mock_dep_anchor` (`window_anchor_request_id`),

    CONSTRAINT `fk_mock_dep_account`
        FOREIGN KEY (`account_id`)
        REFERENCES `group_account` (`account_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_mock_dep_member`
        FOREIGN KEY (`member_id`)
        REFERENCES `members` (`member_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_mock_dep_transaction`
        FOREIGN KEY (`transaction_id`)
        REFERENCES `group_transaction` (`transaction_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_mock_dep_anchor`
        FOREIGN KEY (`window_anchor_request_id`)
        REFERENCES `saving_group_mock_deposit_request` (`deposit_request_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,

    CONSTRAINT `chk_mock_dep_amount`
        CHECK (`amount` >= 1 AND `amount` = FLOOR(`amount`)),
    CONSTRAINT `chk_mock_dep_key`
        CHECK (CHAR_LENGTH(TRIM(`request_key`)) > 0),
    CONSTRAINT `chk_mock_dep_status`
        CHECK (`status` IN ('PENDING_CONFIRMATION', 'COMPLETED')),
    CONSTRAINT `chk_mock_dep_state`
        CHECK (
                (`status` = 'PENDING_CONFIRMATION'
                AND `transaction_id` IS NULL
                AND `completed_at` IS NULL
                AND `window_anchor_request_id` IS NOT NULL
                AND `confirmation_token_hash` IS NOT NULL
                AND `confirmation_expires_at` IS NOT NULL)
            OR
                (`status` = 'COMPLETED'
                AND `transaction_id` IS NOT NULL
                AND `completed_at` IS NOT NULL
                AND `confirmation_token_hash` IS NULL)
        ),
    CONSTRAINT `chk_mock_dep_completion_time`
        CHECK (`completed_at` IS NULL OR `completed_at` >= `requested_at`),
    CONSTRAINT `chk_mock_dep_expiry_time`
        CHECK (
              `confirmation_expires_at` IS NULL
              OR `confirmation_expires_at` > `requested_at`
          )
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계모임 테스트 입금 요청';

CREATE TABLE IF NOT EXISTS `post` (
	`post_id`	BIGINT	NOT NULL AUTO_INCREMENT COMMENT '게시글 ID',
	`writer_id`	BIGINT	NOT NULL COMMENT '작성 회원 ID',
	`category_id`	BIGINT	NOT NULL COMMENT '카테고리 ID',
	`title`	VARCHAR(200)	NOT NULL COMMENT '제목',
	`content`	TEXT	NOT NULL COMMENT '게시글 내용',
	`view_count`	INT	NOT NULL	DEFAULT 0 COMMENT '조회 수',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP COMMENT '작성 일시',
	`deleted_at`	DATETIME	NULL COMMENT '삭제 일시',
    `updated_at` DATETIME NULL COMMENT '수정 일시',
	PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글';

CREATE TABLE IF NOT EXISTS `post_image` (
    `image_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '게시글 이미지 ID',
    `post_id` BIGINT NOT NULL COMMENT '게시글 ID',
    `image_url` VARCHAR(500) NOT NULL COMMENT '이미지 주소',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '이미지 정렬 순서',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

    PRIMARY KEY (`image_id`),
    KEY `ix_post_image_post_order` (`post_id`, `sort_order`),

    CONSTRAINT `fk_post_image_post`
    FOREIGN KEY (`post_id`)
    REFERENCES `post` (`post_id`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci COMMENT='게시글 이미지';

CREATE  TABLE  IF NOT EXISTS `chat_room` (
    `room_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '채팅방 ID',
    `saving_group_id` BIGINT NOT NULL COMMENT '계모임 ID',

    PRIMARY KEY (`room_id`),
    UNIQUE KEY `uq_chat_room_saving_group` (`saving_group_id`),

    CONSTRAINT `fk_chat_room_saving_group`
        FOREIGN KEY (`saving_group_id`)
        REFERENCES `saving_group` (`saving_group_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci COMMENT='계모임 채팅방';

CREATE TABLE IF NOT EXISTS `chat_room_member` (
    `room_id` BIGINT NOT NULL COMMENT '채팅방 ID',
    `member_id` BIGINT NOT NULL COMMENT '회원 ID',

    PRIMARY KEY (`room_id`, `member_id`),
    KEY `ix_chat_room_member_member` (`member_id`),

    CONSTRAINT `fk_chat_room_member_room`
        FOREIGN KEY (`room_id`)
        REFERENCES `chat_room` (`room_id`)
        ON DELETE CASCADE,

    CONSTRAINT `fk_chat_room_member_member`
        FOREIGN KEY (`member_id`)
        REFERENCES `members` (`member_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci COMMENT='채팅방 참여 회원';

CREATE TABLE IF NOT EXISTS `chat_message`(
    `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '채팅 메시지 ID',
    `room_id` BIGINT NOT NULL COMMENT '채팅방 ID',
    `sender_id` BIGINT NULL DEFAULT NULL COMMENT '발신 회원 ID',
    `content` VARCHAR(1000) NOT NULL COMMENT '메시지 내용',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '메시지 작성 일시',

    PRIMARY KEY (`message_id`),
    KEY `idx_room_message` (`room_id`, `message_id`),
    KEY `ix_chat_message_sender` (`sender_id`),

    CONSTRAINT `fk_chat_message_room`
        FOREIGN KEY (`room_id`)
        REFERENCES `chat_room` (`room_id`)
        ON DELETE CASCADE,

    CONSTRAINT `fk_chat_message_sender`
        FOREIGN KEY (`sender_id`)
        REFERENCES `members` (`member_id`)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci COMMENT='채팅 메시지';

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

ALTER TABLE `saving_group`
ADD CONSTRAINT `FK_interest_TO_saving_group_1`
FOREIGN KEY (`interest_id`)
REFERENCES `interest` (`interest_id`);

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
