import { useEffect, useState } from 'react';
import styled from 'styled-components';

const Layout = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 1rem;
  color: #17352e;
  font-size: 1rem;
  @media (max-width: 760px) { grid-template-columns: minmax(0, 1fr); }
`;
const Card = styled.section`
  min-width: 0;
  padding: clamp(.5rem, 2vw, 1rem);
  border: 1px solid #cbdad6;
  border-radius: .8rem;
  background: white;
  h3 { margin: 0; }
  button, input { min-height: 44px; font: inherit; }
  button { padding: .35rem .65rem; cursor: pointer; }
  button:disabled { cursor: default; }
`;
const Toolbar = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: .5rem;
  margin-bottom: .75rem;
`;
const Primary = styled.button`
  background: #116858;
  color: white;
  border: 1px solid #116858;
  border-radius: .5rem;
`;
const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  border: 1px solid #cbdad6;
`;
const Weekday = styled.div`
  text-align: center;
  padding: .5rem 0;
  background: #def2eb;
  font-weight: bold;
`;
const Day = styled.button`
  && {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    text-align: left;
    min-height: 5.5rem;
    min-width: 0;
    padding: .25rem;
    border: 1px solid #dce6e3;
    background: ${({ $selected }) => $selected ? '#d5f6e9' : 'white'};
    color: inherit;
  }
`;
const Badge = styled.span`
  display: block;
  border-radius: .3rem;
  background: #bfe9dc;
  font-size: .85rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;
const DetailTitle = styled.h3`
  padding: .65rem;
  border-radius: .5rem;
  background: #def2eb;
`;
const Event = styled.li`
  display: flex;
  justify-content: space-between;
  gap: .75rem;
  padding: .65rem 0;
  border-bottom: 1px solid #dce6e3;
  overflow-wrap: anywhere;
  time { white-space: nowrap; }
`;
const EventActions = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: .5rem;
`;
const Form = styled.form`
  display: grid;
  gap: .65rem;
  margin-top: 1rem;
  label { display: grid; gap: .25rem; }
`;

const week = ['일', '월', '화', '수', '목', '금', '토'];
// AI작성: 서버의 선택일 등록 상한과 같은 값으로 버튼 상태를 안내한다.
const dailyEventLimit = 10;
const seoulDate = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' });
// AI작성: 달력 날짜와 동일한 서울 시간대를 사용한다.
const seoulTime = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', hourCycle: 'h23', hour: '2-digit', minute: '2-digit',
});
const detailDate = new Intl.DateTimeFormat('ko-KR', {
  month: 'long', day: 'numeric', weekday: 'long', timeZone: 'UTC',
});

function monthDate(month, offset = 0, day = 1) {
  const [year, number] = month.split('-').map(Number);
  return new Date(Date.UTC(year, number - 1 + offset, day));
}

// AI작성: 한국 날짜를 기준으로 월을 조회하고, 같은 응답에서 날짜별 상세를 보여 준다.
export default function SavingGroupCalendarPanel({ groupId, loadMonth, createEvent, deleteEvent }) {
  const [selected, setSelected] = useState(() => seoulDate.format(new Date()));
  const month = selected.slice(0, 7);
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [title, setTitle] = useState('');
  // AI작성: 오전·오후는 하나의 값으로 관리하고 시와 분은 따로 입력받는다.
  const [period, setPeriod] = useState('AM');
  const [hour, setHour] = useState('');
  const [minute, setMinute] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [deletingId, setDeletingId] = useState(null);
  const [deleteError, setDeleteError] = useState('');
  const busy = saving || deletingId !== null;

  useEffect(() => {
    const controller = new AbortController();
    loadMonth(groupId, { month, signal: controller.signal })
      .then(data => {
        if (!controller.signal.aborted) setResult({ month, retry, data });
      })
      .catch(error => {
        if (!controller.signal.aborted) {
          setResult({ month, retry, error: error.message || '일정을 불러오지 못했습니다.' });
        }
      });
    return () => controller.abort();
  }, [groupId, loadMonth, month, retry]);

  const current = result?.month === month && result.retry === retry ? result : null;
  // AI작성: 달력의 배지와 선택일 목록이 같은 일정 배열을 사용한다.
  const eventsByDate = new Map();
  for (const event of current?.data?.events ?? []) {
    if (!eventsByDate.has(event.date)) eventsByDate.set(event.date, []);
    eventsByDate.get(event.date).push(event);
  }
  const selectedEvents = eventsByDate.get(selected) ?? [];
  const limitReached = selectedEvents.length >= dailyEventLimit;
  const first = monthDate(month).getUTCDay();
  const days = monthDate(month, 1, 0).getUTCDate();
  const cells = Math.ceil((first + days) / 7) * 7;
  const [year, number] = month.split('-').map(Number);

  function selectDate(date) {
    setSelected(date);
    setFormOpen(false);
    setSaveError('');
    setDeleteError('');
  }

  function moveMonth(offset) {
    selectDate(monthDate(month, offset).toISOString().slice(0, 10));
  }

  function openForm() {
    const [currentHour, currentMinute] = seoulTime.format(new Date()).split(':');
    const hour24 = Number(currentHour);
    setPeriod(hour24 < 12 ? 'AM' : 'PM');
    setHour(String(hour24 % 12 || 12));
    setMinute(currentMinute);
    setFormOpen(true);
  }

  async function save(event) {
    event.preventDefault();
    if (busy || !current?.data?.canCreate || limitReached) return;
    if (!title.trim()) {
      setSaveError('일정 제목을 입력해 주세요.');
      return;
    }
    if (!/^\d{1,2}$/.test(hour) || Number(hour) < 1 || Number(hour) > 12 ||
        !/^\d{1,2}$/.test(minute) || Number(minute) > 59) {
      setSaveError('시는 1~12, 분은 0~59 사이의 정수로 입력해 주세요.');
      return;
    }
    // AI작성: 자정은 00시, 정오는 12시로 변환해 기존 API 형식으로 전송한다.
    const hour24 = Number(hour) % 12 + (period === 'PM' ? 12 : 0);
    const time = `${String(hour24).padStart(2, '0')}:${minute.padStart(2, '0')}`;
    setSaving(true);
    setSaveError('');
    try {
      const created = await createEvent(groupId, { date: selected, title: title.trim(), time });
      setResult(previous => ({
        ...previous,
        data: {
          ...previous.data,
          events: [...previous.data.events, created].sort((a, b) =>
            a.date.localeCompare(b.date) || a.time.localeCompare(b.time)),
        },
      }));
      setFormOpen(false);
      setTitle('');
    } catch (error) {
      setSaveError(error.message || '일정을 저장하지 못했습니다.');
      // AI작성: 다른 회원이 마지막 자리를 등록했으면 서버의 최신 월간 일정을 조회한다.
      if (error.code === 'SAVING_GROUP_DAILY_EVENT_LIMIT') setRetry(value => value + 1);
    } finally {
      setSaving(false);
    }
  }

  // AI작성: 서버 삭제가 성공한 뒤 공유 배열에서 제거해 배지와 등록 가능 여부를 함께 갱신한다.
  async function remove(item) {
    if (busy || !current?.data?.canCreate) return;
    if (!window.confirm(`${item.title} (${item.time.slice(0, 5)}) 일정을 삭제할까요?`)) return;
    setDeletingId(item.id);
    setDeleteError('');
    try {
      await deleteEvent(groupId, item.id);
      setResult(previous => ({
        ...previous,
        data: { ...previous.data, events: previous.data.events.filter(event => event.id !== item.id) },
      }));
      setSaveError('');
    } catch (error) {
      setDeleteError(error.message || '일정을 삭제하지 못했습니다.');
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <Layout>
      <Card aria-label="월간 달력">
        <Toolbar>
          <button type="button" onClick={() => moveMonth(-1)} disabled={busy || month === '1000-01'}>이전 달</button>
          <h3>{year}년 {number}월</h3>
          <button type="button" onClick={() => moveMonth(1)} disabled={busy || month === '9999-12'}>다음 달</button>
          <button type="button" onClick={() => selectDate(seoulDate.format(new Date()))} disabled={busy}>오늘</button>
          {current?.data?.canCreate && (
            <Primary type="button" onClick={openForm} disabled={busy || limitReached || formOpen}>+ 일정 추가</Primary>
          )}
        </Toolbar>
        <Grid>
          {week.map(label => <Weekday key={label}>{label}</Weekday>)}
          {Array.from({ length: cells }, (_, index) => {
            const day = index - first + 1;
            if (day < 1 || day > days) return <div key={index} aria-hidden="true" />;
            const date = `${month}-${String(day).padStart(2, '0')}`;
            const items = eventsByDate.get(date) ?? [];
            return (
              <Day key={date} type="button" $selected={date === selected} aria-pressed={date === selected}
                aria-label={`${number}월 ${day}일, 일정 ${items.length}개`} disabled={busy}
                onClick={() => selectDate(date)}>
                <strong>{day}</strong>
                {items.slice(0, 2).map(item => <Badge key={item.id}>{item.title}</Badge>)}
                {items.length > 2 && <Badge>+{items.length - 2}개</Badge>}
              </Day>
            );
          })}
        </Grid>
      </Card>
      <Card aria-label="선택한 날짜의 일정">
        <DetailTitle>{detailDate.format(new Date(selected))}</DetailTitle>
        {limitReached && current?.data?.canCreate && !saveError && (
          <p>하루에 최대 10개까지 등록할 수 있습니다.</p>
        )}
        {/* AI작성: 등록 폼은 선택일 제목 아래에 열리고 목록은 그 아래에 표시한다. */}
        {formOpen && (
          <Form onSubmit={save}>
            <h4>선택한 날짜에 일정 등록</h4>
            <label>일정 제목<input autoFocus value={title} onChange={event => setTitle(event.target.value)} maxLength={100} required disabled={saving} /></label>
            <fieldset disabled={saving}>
              <legend>시각</legend>
              <Toolbar>
                <label><input type="radio" name="schedule-period" checked={period === 'AM'} onChange={() => setPeriod('AM')} /> 오전</label>
                <label><input type="radio" name="schedule-period" checked={period === 'PM'} onChange={() => setPeriod('PM')} /> 오후</label>
                <label>시<input inputMode="numeric" maxLength={2} value={hour} onChange={event => setHour(event.target.value)} /></label>
                <label>분<input inputMode="numeric" maxLength={2} value={minute} onChange={event => setMinute(event.target.value)} /></label>
              </Toolbar>
            </fieldset>
            {saveError && <p role="alert">{saveError}</p>}
            <Toolbar>
              <Primary type="submit" disabled={busy || !current?.data?.canCreate || limitReached}>{saving ? '저장 중' : '일정 저장'}</Primary>
              <button type="button" disabled={saving} onClick={() => { setFormOpen(false); setSaveError(''); }}>취소</button>
            </Toolbar>
          </Form>
        )}
        <h4>일정</h4>
        {deleteError && <p role="alert">{deleteError}</p>}
        {!current && <p role="status">일정을 불러오는 중입니다.</p>}
        {current?.error && (
          <div role="alert"><p>{current.error}</p><button type="button" onClick={() => setRetry(value => value + 1)}>다시 조회</button></div>
        )}
        {current?.data && (selectedEvents.length === 0 ? <p>등록된 일정이 없습니다.</p> : (
          <ul>
            {selectedEvents.map(item => (
              <Event key={item.id}>
                <strong>{item.title}</strong>
                <EventActions>
                  <time dateTime={item.time}>{item.time.slice(0, 5)}</time>
                  {current.data.canCreate && (
                    <button type="button" disabled={busy} aria-label={`${item.title} 일정 삭제`} onClick={() => remove(item)}>
                      {deletingId === item.id ? '삭제 중' : '삭제'}
                    </button>
                  )}
                </EventActions>
              </Event>
            ))}
          </ul>
        ))}
      </Card>
    </Layout>
  );
}
