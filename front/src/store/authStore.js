import { create } from "zustand";
import { createJSONStorage, persist } from 
"zustand/middleware";

// 로그인 JWT를 화면 간 공유하고 현재 탭에 보관
const useAuthStore = create(
persist(
(set) => ({
    token: null,
    setToken: (token) => set({ token }),

    clearToken: () => set({ token: null }),
}),
{
    name: 'moa-auth',
    storage: createJSONStorage(() => sessionStorage),
    partialize: (state) => ({ token: state.token }),
        },
    ),
);

export default useAuthStore;