import client from './client.js';

// 기존 API 클라이언트로 질문을 보내고, 응답에서 답변 문장만 꺼냅니다.
export async function sendChatbotMessage(message) {
  const { data } = await client.post(
    '/chatbot/messages',
    { message },
    { timeout: 40_000 },
  );

  return data.data.reply;
}