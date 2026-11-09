import { useEffect, useRef, useState } from 'react';
import Button from '../common/Button';
import ConfirmModal from '../common/ConfirmModal';
import { deleteSmallGroupNotice } from '../../api/smallGroups.js';

// AI작성: 기존 확인창을 재사용하고 삭제 중 중복 요청과 계정·URL 이탈 후 콜백을 차단한다.
export default function SmallGroupNoticeDeleteButton({ groupId, noticeId, onDeleted, onBlocked }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const lock = useRef(false);
  const active = useRef(false);
  useEffect(() => {
    active.current = true;
    return () => { active.current = false; };
  }, []);
  const remove = async () => {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError('');
    try {
      await deleteSmallGroupNotice(groupId, noticeId);
      if (active.current) onDeleted();
    } catch (failure) {
      if (!active.current) return;
      setError(failure.message);
      if ([401, 403, 404, 409].includes(failure.status)) onBlocked();
    } finally {
      lock.current = false;
      if (active.current) setBusy(false);
    }
  };
  return (
    <>
      <Button type="button" $variant="danger" onClick={() => { setOpen(true); setError(''); }}>공지 삭제</Button>
      {open && <ConfirmModal title="공지를 삭제하시겠어요?" description="삭제한 공지는 목록에서 사라집니다."
        confirmLabel={busy ? '삭제 중…' : '공지 삭제'} confirmVariant="danger" isBusy={busy}
        onConfirm={remove} onCancel={() => { if (!lock.current) { setOpen(false); setError(''); } }}>
        {error && <p role="alert">{error}</p>}
      </ConfirmModal>}
    </>
  );
}
