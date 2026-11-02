import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { loadKakaoMap } from '../../api/kakaoMap';

const MapArea = styled.div`
  width: 100%;
  height: 280px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
`;

function RegionMap({ latitude, longitude }) {
  const mapRef = useRef(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const container = mapRef.current;

    setIsLoading(true);
    setError('');

    async function initializeMap() {
      try {
        const maps = await loadKakaoMap();

        if (!active || !container) return;

        const hasPosition =
          Number.isFinite(latitude) && Number.isFinite(longitude);

        // AI작성: 위치 확인 전에는 서울 시청 부근을 기본 지도로 표시한다.
        const center = new maps.LatLng(
          hasPosition ? latitude : 37.5665,
          hasPosition ? longitude : 126.978,
        );

        const map = new maps.Map(container, {
          center,
          level: hasPosition ? 4 : 7,
        });

        // AI작성: 실제로 전달받은 좌표가 있을 때만 위치 마커를 표시한다.
        if (hasPosition) {
          new maps.Marker({
            map,
            position: center,
          });
        }
      } catch (error) {
        if (active) {
          setError(error.message || '지도를 불러오지 못했습니다.');
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    initializeMap();

    return () => {
      active = false;
      container?.replaceChildren();
    };
  }, [latitude, longitude]);

  return (
    <div>
      <MapArea ref={mapRef} aria-label="활동 지역 지도" />
      {isLoading && <p role="status">지도를 불러오고 있습니다.</p>}
      {error && <p role="alert">{error}</p>}
    </div>
  );
}

export default RegionMap;