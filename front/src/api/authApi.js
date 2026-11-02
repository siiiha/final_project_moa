import client from './client';

// AI작성: 로그인 성공 후 JWT를 받고, 같은 JWT로 로그인 회원 번호를 확인한다.
export async function login(request) {
  const response = await client.post('/auth/login', request);
  return response.data;
}

export async function getMyMemberId() {
  const response = await client.get('/auth/me');
  return response.data;
}

export async function logout() {
  const response = await client.post('/auth/logout');
  return response.data;
}
