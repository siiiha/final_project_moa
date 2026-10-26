import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

// AI작성: access token만 저장하고 refresh token은 HttpOnly 쿠키로 관리한다.
const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      authVersion: 0,

      setToken: (token) =>
        set((state) => ({
          token,
          authVersion: state.authVersion + 1,
        })),

      // AI작성: 재발급은 현재 로그인 상태를 유지하며 토큰만 교체한다.
      replaceToken: (token) => set({ token }),

      clearToken: () =>
        set((state) => ({
          token: null,
          authVersion: state.authVersion + 1,
        })),
    }),
    {
      name: 'moa-auth',
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({ token: state.token }),
    },
  ),
);

export default useAuthStore;