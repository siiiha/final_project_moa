// AI작성: stage4가 합성 계좌 10013을 실제 분석해 저장한 9월 결과의 스냅샷이다.
// 원본 파일과 해시를 남겨 숫자 출처를 확인한다. 브라우저에서 실시간 추론하지 않는다.
export const savingGroupReplayInfo = {
  "sourceAccountId": 10013,
  "displayGroupId": 12,
  "periodStart": "2026-09-01",
  "periodEnd": "2026-09-30",
  "sourceFile": "deliverables/moa-anomaly-stage4/predictions/test.jsonl",
  "sourceSha256": "290ccfc169a651e12a8529d7ad3babd1bb4b49d954ef12fa8590a94c02966d92",
  "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
  "transactionCount": 14,
  "alertCount": 4
};

// AI작성: 모임 12는 화면 재생을 위한 임시 위치다. 운영 DB 계좌-모임 관계로 사용하지 않는다.
// 거래 설명은 원천 식별자/시나리오명을 노출하지 않도록 중립 문구로 대체했다.
// 금액·잔액·원 점수·경고 기준·경고 여부·품질 문맥은 저장 결과와 동일하다.
// 실제 분석 시각과 DB revision은 원본에 없으므로 지어내지 않고 null을 유지한다.
// 모든 행은 완료된 저장 결과이며 임의 PENDING/FAILED를 섞지 않는다.
export const savingGroupAnomalyFixtures = {
  12: [
    {
      "id": "mock-10013-000053",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-27",
      "time": "13:08",
      "type": "출금",
      "description": "합성 계좌 출금",
      "amount": -12000,
      "balance": 2899528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.5969525018193491,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-scenario-13-05",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-18",
      "time": "20:32",
      "type": "출금",
      "description": "합성 계좌 출금",
      "amount": -50000,
      "balance": 2911528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.7116934010508431,
        "threshold": 0.6180405340153708,
        "isAnomaly": true,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-scenario-13-04",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-18",
      "time": "20:24",
      "type": "출금",
      "description": "합성 계좌 출금",
      "amount": -50000,
      "balance": 2961528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.7186575241835682,
        "threshold": 0.6180405340153708,
        "isAnomaly": true,
        "requiresReview": true,
        "coldStart": true,
        "qualityFlags": [
          "same_direction_history_under_5"
        ],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-scenario-13-03",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-18",
      "time": "20:16",
      "type": "출금",
      "description": "합성 계좌 출금",
      "amount": -50000,
      "balance": 3011528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.71469214825948,
        "threshold": 0.6180405340153708,
        "isAnomaly": true,
        "requiresReview": true,
        "coldStart": true,
        "qualityFlags": [
          "same_direction_history_under_5"
        ],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-scenario-13-02",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-18",
      "time": "20:08",
      "type": "출금",
      "description": "합성 계좌 출금",
      "amount": -50000,
      "balance": 3061528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.6934931422165204,
        "threshold": 0.6180405340153708,
        "isAnomaly": true,
        "requiresReview": true,
        "coldStart": true,
        "qualityFlags": [
          "same_direction_history_under_5"
        ],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-scenario-13-01",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-18",
      "time": "20:00",
      "type": "출금",
      "description": "합성 계좌 출금",
      "amount": -50000,
      "balance": 3111528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.5160923821055847,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": true,
        "coldStart": true,
        "qualityFlags": [
          "same_direction_history_under_5"
        ],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000046",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-16",
      "time": "12:07",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 55000,
      "balance": 3161528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.40473969504464996,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000045",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-16",
      "time": "07:16",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 50000,
      "balance": 3106528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.40617422663798836,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000048",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-14",
      "time": "19:52",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 55000,
      "balance": 3056528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.40155602105931465,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000049",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-13",
      "time": "08:48",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 55000,
      "balance": 3001528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.38983972896303376,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000050",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-13",
      "time": "07:15",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 55000,
      "balance": 2946528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.46769586462996793,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": true,
        "coldStart": true,
        "qualityFlags": [
          "same_direction_history_under_5"
        ],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000052",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-12",
      "time": "19:23",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 55000,
      "balance": 2891528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.42723734082248666,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000047",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-12",
      "time": "12:03",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 55000,
      "balance": 2836528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.4389156396678943,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": false,
        "coldStart": false,
        "qualityFlags": [],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    },
    {
      "id": "mock-10013-000051",
      "sourceAccountId": 10013,
      "groupId": 12,
      "date": "2026-09-12",
      "time": "12:02",
      "type": "입금",
      "description": "합성 계좌 입금",
      "amount": 45000,
      "balance": 2781528,
      "analysis": {
        "status": "COMPLETED",
        "revisionNo": null,
        "anomalyScore": 0.5277883655417102,
        "threshold": 0.6180405340153708,
        "isAnomaly": false,
        "requiresReview": true,
        "coldStart": true,
        "qualityFlags": [
          "same_direction_history_under_5"
        ],
        "modelVersion": "sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d",
        "featureSchemaVersion": "moa-anomaly-features-v1",
        "analyzedAt": null,
        "errorCode": null
      }
    }
  ],
};
