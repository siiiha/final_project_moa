import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// AI작성: JWT와 로그인 회원 번호만 브라우저 저장소에 유지해 API 인증과 화면 권한 표시에 사용한다.
const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      memberId: null,
      setAuth: (token, memberId) => set({ token, memberId }),
      clearAuth: () => set({ token: null, memberId: null }),
    }),
    { name: 'moa-auth' }
  )
);

export default useAuthStore;
