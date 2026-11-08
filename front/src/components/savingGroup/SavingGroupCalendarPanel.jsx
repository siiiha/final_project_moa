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
const Form = styled.form`
  display: grid;
  gap: .65rem;
  margin-top: 1rem;
  label { display: grid; gap: .25rem; }
`;

const week = ['일', '월', '화', '수', '목', '금', '토'];
const seoulDate = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' });
const detailDate = new Intl.DateTimeFormat('ko-KR', {
  month: 'long', day: 'numeric', weekday: 'long', timeZone: 'UTC',
});

function monthDate(month, offset = 0, day = 1) {
  const [year, number] = month.split('-').map(Number);
  return new Date(Date.UTC(year, number - 1 + offset, day));
}

// AI작성: 한국 날짜를 기준으로 월을 조회하고, 같은 응답에서 날짜별 상세를 보여 준다.
export default function SavingGroupCalendarPanel({ groupId, loadMonth, createEvent }) {
  const [selected, setSelected] = useState(() => seoulDate.format(new Date()));
  const month = selected.slice(0, 7);
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [time, setTime] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

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
  const first = monthDate(month).getUTCDay();
  const days = monthDate(month, 1, 0).getUTCDate();
  const cells = Math.ceil((first + days) / 7) * 7;
  const [year, number] = month.split('-').map(Number);

  function selectDate(date) {
    setSelected(date);
    setFormOpen(false);
    setSaveError('');
  }

  function moveMonth(offset) {
    selectDate(monthDate(month, offset).toISOString().slice(0, 10));
  }

  async function save(event) {
    event.preventDefault();
    if (saving) return;
    if (!title.trim()) {
      setSaveError('일정 제목을 입력해 주세요.');
      return;
    }
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
      setTime('');
    } catch (error) {
      setSaveError(error.message || '일정을 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Layout>
      <Card aria-label="월간 달력">
        <Toolbar>
          <button type="button" onClick={() => moveMonth(-1)} disabled={saving || month === '1000-01'}>이전 달</button>
          <h3>{year}년 {number}월</h3>
          <button type="button" onClick={() => moveMonth(1)} disabled={saving || month === '9999-12'}>다음 달</button>
          <button type="button" onClick={() => selectDate(seoulDate.format(new Date()))} disabled={saving}>오늘</button>
          {current?.data?.canCreate && (
            <Primary type="button" onClick={() => setFormOpen(true)} disabled={saving}>+ 일정 추가</Primary>
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
                aria-label={`${number}월 ${day}일, 일정 ${items.length}개`} disabled={saving}
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
        <h4>일정</h4>
        {!current && <p role="status">일정을 불러오는 중입니다.</p>}
        {current?.error && (
          <div role="alert"><p>{current.error}</p><button type="button" onClick={() => setRetry(value => value + 1)}>다시 조회</button></div>
        )}
        {current?.data && (selectedEvents.length === 0 ? <p>등록된 일정이 없습니다.</p> : (
          <ul>
            {selectedEvents.map(item => (
              <Event key={item.id}><strong>{item.title}</strong><time dateTime={item.time}>{item.time.slice(0, 5)}</time></Event>
            ))}
          </ul>
        ))}
        {formOpen && current?.data?.canCreate && (
          <Form onSubmit={save}>
            <h4>선택한 날짜에 일정 등록</h4>
            <label>일정 제목<input autoFocus value={title} onChange={event => setTitle(event.target.value)} maxLength={100} required disabled={saving} /></label>
            <label>시각<input type="time" value={time} onChange={event => setTime(event.target.value)} required disabled={saving} /></label>
            {saveError && <p role="alert">{saveError}</p>}
            <Toolbar>
              <Primary type="submit" disabled={saving}>{saving ? '저장 중' : '일정 저장'}</Primary>
              <button type="button" disabled={saving} onClick={() => { setFormOpen(false); setSaveError(''); }}>취소</button>
            </Toolbar>
          </Form>
        )}
      </Card>
    </Layout>
  );
}
