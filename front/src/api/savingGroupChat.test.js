import assert from 'node:assert/strict';
import test from 'node:test';
import { Client } from '@stomp/stompjs';
import client from './client.js';
import { connectSavingGroupChat, findChatRoom } from './savingGroupChat.js';

// AI작성: 네트워크 없이 실제 STOMP 구독 콜백의 오류 코드·문구 전달과 재연결을 검증한다.
test('채팅 도배 오류 코드와 일반 오류 문구를 전달하고 재연결마다 개인 오류 큐를 구독한다', async () => {
  const originalWindow = globalThis.window;
  const originalActivate = Client.prototype.activate;
  const originalDeactivate = Client.prototype.deactivate;
  const originalSubscribe = Client.prototype.subscribe;
  const originalGet = client.get;
  let connection;
  const subscriptions = [];
  const errors = [];
  let connects = 0;
  try {
    globalThis.window = { location: { href: 'https://moa.example/' } };
    Client.prototype.activate = function () { connection = this; };
    Client.prototype.deactivate = async function () {};
    Client.prototype.subscribe = function (destination, callback) {
      subscriptions.push({ destination, callback });
    };
    const chat = connectSavingGroupChat(10, 1, {
      onMessage() {}, onDisconnect() {}, onConnect() { connects++; },
      onError(...args) { errors.push(args); },
    });
    connection.onConnect();
    const errorQueue = subscriptions.find(item => item.destination === '/user/queue/errors');
    errorQueue.callback({ body: JSON.stringify({ error: { code: 'CHAT_RATE_LIMITED', message: '3분 제한' } }) });
    errorQueue.callback({ body: JSON.stringify({ error: { code: 'FEATURE_RESTRICTED', message: '관리자 제재' } }) });
    errorQueue.callback({ body: '{}' });
    assert.deepEqual(errors, [
      ['3분 제한', 'CHAT_RATE_LIMITED'], ['관리자 제재', 'FEATURE_RESTRICTED'],
      ['메시지를 보내지 못했습니다.', undefined],
    ]);
    connection.onConnect();
    assert.equal(connects, 2);
    assert.equal(subscriptions.filter(item => item.destination === '/user/queue/errors').length, 2);

    const signal = new AbortController().signal;
    const room = { roomId: 10, chatRestricted: false, spamRestricted: true,
      restrictedUntil: '2026-11-11T00:03:00Z', serverTime: '2026-11-11T00:00:00Z' };
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/20/chat-room');
      assert.equal(options.signal, signal);
      return { data: { data: room } };
    };
    assert.deepEqual(await findChatRoom(20, signal), room);
    assert.equal(chat.send('연결 전'), false);
    await chat.close();
  } finally {
    globalThis.window = originalWindow;
    Client.prototype.activate = originalActivate;
    Client.prototype.deactivate = originalDeactivate;
    Client.prototype.subscribe = originalSubscribe;
    client.get = originalGet;
  }
});
