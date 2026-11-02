let mapSdkPromise;

// AI작성: 카카오 지도 SDK를 한 번만 불러와 화면 간에 재사용한다.
export function loadKakaoMap() {
  if (window.kakao?.maps?.Map) {
    return Promise.resolve(window.kakao.maps);
  }

  if (mapSdkPromise) {
    return mapSdkPromise;
  }

  const key = import.meta.env.VITE_KAKAO_MAP_JAVASCRIPT_KEY;

  if (!key) {
    return Promise.reject(
      new Error('카카오 지도 키 설정을 확인해주세요.'),
    );
  }

  mapSdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');

    // AI작성: 로딩 실패 시 다시 시도할 수 있도록 초기화한다.
    const fail = () => {
      clearTimeout(timer);
      script.remove();
      mapSdkPromise = null;
      reject(new Error('지도를 불러오지 못했습니다. 다시 시도해주세요.'));
    };

    const timer = setTimeout(fail, 15000);

    script.async = true;
    script.src =
      `https://dapi.kakao.com/v2/maps/sdk.js` +
      `?appkey=${encodeURIComponent(key)}&autoload=false`;

    script.onload = () => {
      if (!window.kakao?.maps?.load) {
        fail();
        return;
      }

      window.kakao.maps.load(() => {
        clearTimeout(timer);
        resolve(window.kakao.maps);
      });
    };

    script.onerror = fail;
    document.head.appendChild(script);
  });

  return mapSdkPromise;
}