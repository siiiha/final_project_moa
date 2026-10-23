<!-- AI작성: 계모임 가입 승인에 필요한 DB 변경 제안과 관리자 확인 절차를 정리한다. -->
# 계모임 가입 승인 DB 선행 변경 요청

## 상태와 범위

- 작성일: 2026-10-22
- 상태: 관리자 검토·공용 DB 반영 확인 대기. 아래 컬럼명과 정책은 제안이며 반영 완료를 의미하지 않는다.
- 기준: 저장소 `back/src/main/resources/schema.sql`과 가입 신청 설계. 실제 공용 DB 구조는 관리자가 확인한다.
- 적용 담당: DB 관리자. 개발 측에서 공용 DB나 schema.sql을 직접 수정하지 않는다.
- 가입 승인은 기존 group_member의 PENDING 행을 UPDATE한다. 새 계원 행을 INSERT하거나 meeting_approval을 사용하지 않는다.

## 변경 요청과 컬럼 명세

| 테이블 | 기존 컬럼·구조 | 변경 제안 | 이유 |
| --- | --- | --- | --- |
| group_member | 승인 처리자 컬럼 없음 | approved_by BIGINT NULL 추가, members.member_id 참조 FK | 승인한 계주의 회원 ID 기록 |
| group_member | 별도 승인 시각 컬럼 없음 | approved_at DATETIME NULL 추가 | 가입 승인 처리 시각 기록 |
| group_member | joined_at DATETIME NULL | 구조 유지, 신규 승인 시 approved_at과 동일한 시각 저장 | 승인 완료를 가입 시점으로 관리 |
| group_member | member_id, group_id의 복합 UNIQUE 없음 | UNIQUE(member_id, group_id) 추가 | 회원·모임별 하나의 관계 행 보장 및 기존 행 재신청 정책 지원 |
| group_member | membership_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'LEFT') NOT NULL DEFAULT 'PENDING' | 기존 정의를 유지하며 EXPELLED 추가 | 추방 상태 구분과 재신청 차단 |
| group_member | 공용 DB 인덱스 확인 필요 | (group_id, membership_status) 인덱스 검토 | 모임별 승인 인원 집계와 상태 조회 지원 |
| group_member | 공용 DB 인덱스 확인 필요 | (member_id, membership_status, group_id) 인덱스 검토 | 회원별 승인 계모임 집계 지원 |

승인 시각 저장 대안은 joined_at만 사용하는 것이다. 컬럼 수는 줄지만 승인 시각과 가입 시각을 구분할 수 없으므로 별도 approved_at을 권장한다. 관리자가 실제 이름·타입·제약을 확정한 뒤 애플리케이션에 반영한다.

approved_by FK는 연쇄 삭제를 사용하지 않는 방향을 제안한다. 기존 승인 행의 승인 처리자·시각을 추정해 채우지 않으며 새 컬럼은 NULL로 유지한다. 기존 joined_at도 보정하지 않는다.

## 기존 데이터와 정원 확인

- UNIQUE 적용 전 member_id와 group_id가 같은 중복 행이 있는지 확인한다. 중복이 있으면 제약 추가를 보류하고 개발자에게 알린다. 임의 삭제·병합·상태 변경은 하지 않는다.
- 공용 DB의 기존 컬럼·ENUM·제약조건·동등 인덱스를 확인해 중복 변경을 피한다.
- groups.max_members는 현재 INT NULL이다. 계모임 연결 행의 NULL·0 이하 여부를 확인하되 값을 임의로 채우지 않는다.
- groups는 소모임도 사용하므로 NOT NULL 또는 CHECK 추가는 소모임 정책 확인 후 별도 결정한다. 이번 요청에서는 구조 변경 대상에서 제외한다.
- 계주는 groups.member_id와 일치하고 group_member에 OWNER + APPROVED 관계가 있어야 한다. 누락·불일치는 확인만 하고 자동 생성·보정하지 않는다.
- saving_group 상태는 00(심사중), 01(모집중), 02(진행중), 03(정지), 04(종료) 기준이다. 다른 값이 있으면 공유하고 임의 변환하지 않는다.

## 애플리케이션 후속 반영

- 승인 시 PENDING 행을 APPROVED, MEMBER로 변경하고 joined_at·approved_at에 동일한 시각, approved_by에 계주 회원 ID를 저장한다.
- LEFT 재신청 시 approved_by·approved_at도 NULL로 초기화한다. 기존 joined_at·left_at·application_message 초기화와 requested_at 갱신을 유지한다.
- 승인 정보 컬럼을 참조하는 Mapper는 DB 반영 확인 후 변경한다. 현재 Mapper는 아직 변경하지 않았다.
- 승인 가능 상태는 01만 허용한다. 정원에는 APPROVED 계주와 계원을 포함하고 NULL·0 이하 정원은 차단한다.
- 최대 5개는 APPROVED 관계 중 삭제되지 않은 00·01·02·03 계모임만 집계한다. PENDING 및 종료·삭제 모임은 제외한다.
- 승인 전체는 신청 회원 → 계모임 순서로 잠그고 조회·검증·UPDATE를 같은 트랜잭션에서 수행한다.

## 관리자 확인 및 전달 요청

1. 변경 전 테이블 정의·인덱스·중복 관계·정원 이상·계주 관계를 확인한다. 결과에 개인정보나 계좌정보를 포함하지 않는다.
2. 실제 변경 SQL과 적용 시점은 관리자가 결정한다. 새 마이그레이션 도구는 도입하지 않는다.
3. 변경 후 approved_by·approved_at의 타입·NULL 허용·기본값, FK와 UNIQUE, ENUM 값, 최종 인덱스를 확인한다.
4. 공용 DB 반영 내역과 갱신한 schema.sql을 개발자에게 공유한다.
5. 개발 측은 전달된 구조를 확인하고 재신청 초기화 변경·테스트를 별도 반영한 뒤 승인 구현을 진행한다.

## 현재 검증 범위

이 문서는 변경 요청 및 명세이며 SQL 실행 결과가 아니다. 공용 DB 접속, DDL 실행, 기존 데이터 변경은 하지 않았다. 실제 DB 제약·실행 계획·잠금·동시성 검증은 아직 수행하지 않았다.
