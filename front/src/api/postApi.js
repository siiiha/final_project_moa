import client from './client';

// AI작성: 게시글 목록 조회 조건을 Spring Boot API에 전달합니다.
export async function getPosts(params) {
  const response = await client.get('/community/list', { params });
  return response.data;
}
