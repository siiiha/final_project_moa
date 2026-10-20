import client from './client';

// AI작성: 게시글 목록 조회 조건을 Spring Boot API에 전달합니다.
export async function getPosts(params) {
  const response = await client.get('/community/list', { params });
  return response.data;
}

// AI작성: 게시글 ID로 Spring Boot 상세 조회 API를 호출합니다.
export async function getPostDetail(postId) {
  const response = await client.get(`/community/detail/${postId}`);
  return response.data;
}

// AI작성: 게시글 작성 데이터를 Spring Boot API에 전달합니다.
export async function createPost(postData) {
  const response = await client.post('/community/write', postData);
  return response.data;
}

// AI작성: 게시글 ID와 수정 데이터를 Spring Boot API에 전달합니다.
export async function updatePost(postId, postData) {
  const response = await client.put(`/community/edit/${postId}`, postData);
  return response.data;
}

// AI작성: 게시글 ID를 Spring Boot 삭제 API에 전달합니다.
export async function deletePost(postId) {
  const response = await client.delete(`/community/${postId}`);
  return response.data;
}

// AI작성: 게시글 ID와 페이징 조건으로 댓글 목록을 조회합니다.
export async function getComments(postId, params) {
  const response = await client.get(`/post/${postId}/comments`, { params });
  return response.data;
}

// AI작성: 게시글 ID와 댓글 데이터를 Spring Boot API에 전달합니다.
export async function createComment(postId, commentData) {
  const response = await client.post(`/post/${postId}/comments`, commentData);
  return response.data;
}

// AI작성: 게시글·댓글 ID와 수정 내용을 Spring Boot API에 전달합니다.
export async function updateComment(postId, commentId, commentData) {
  const response = await client.put(`/post/${postId}/comments/${commentId}`, commentData);
  return response.data;
}
