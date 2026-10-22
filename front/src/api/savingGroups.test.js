import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { requestSavingGroupJoin } from './savingGroups.js';

test('가입 신청 성공 응답을 그대로 반환한다', async () => {
  const originalAdapter = client.defaults.adapter;
  client.defaults.adapter = async (config) => ({
    data: { success: true, data: null, meta: null, error: null },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  });

  try {
    const result = await requestSavingGroupJoin(12);

    assert.equal(result.success, true);
  } finally {
    client.defaults.adapter = originalAdapter;
  }
});

test('본인 인증 미완료 응답의 코드와 메시지를 보존한다', async () => {
  const originalAdapter = client.defaults.adapter;
  client.defaults.adapter = async () => {
    const error = new Error('Forbidden');
    error.response = {
      data: {
        success: false,
        error: {
          code: 'IDENTITY_VERIFICATION_REQUIRED',
          message: '본인 인증 완료 후 가입 신청할 수 있습니다.',
        },
      },
      status: 403,
    };
    throw error;
  };

  try {
    await assert.rejects(
      requestSavingGroupJoin(12),
      (error) => error.code === 'IDENTITY_VERIFICATION_REQUIRED'
        && error.message === '본인 인증 완료 후 가입 신청할 수 있습니다.',
    );
  } finally {
    client.defaults.adapter = originalAdapter;
  }
});
