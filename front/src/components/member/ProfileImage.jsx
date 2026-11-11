import { useEffect, useState } from 'react';
import { FiUser } from 'react-icons/fi';
import styled from 'styled-components';
import { getMyProfileImage } from '../../api/member';
import useAuthStore from '../../store/authStore';

const Image = styled.img`
  display: block;
  width: 100%;
  height: 100%;
  border-radius: inherit;
  object-fit: cover;
  visibility: ${({ $loading }) => ($loading ? 'hidden' : 'visible')};
`;

// AI작성: 사진 조회 진행 상태와 실패 안내를 표시합니다.
const ImageStatus = styled.span`
  padding: 6px;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
  word-break: keep-all;
`;

function ProfileImage({ imageUrl, size = 38 }) {
  const token = useAuthStore((state) => state.token);
  const [image, setImage] = useState(null);

  useEffect(() => {
    if (!token || !imageUrl) return;

    let cancelled = false;
    let objectUrl = null;

    async function loadImage() {
      try {
        const blob = await getMyProfileImage(token, imageUrl);

        if (cancelled) return;

        objectUrl = URL.createObjectURL(blob);
        setImage({
          token,
          imageUrl,
          src: objectUrl,
          status: 'loading',
        });
      } catch {
        if (!cancelled) {
          setImage({
            token,
            imageUrl,
            src: null,
            status: 'error',
          });
        }
      }
    }

    loadImage();

    // AI작성: 사진 변경 또는 화면 종료 시 임시 주소를 해제합니다.
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [token, imageUrl]);

  if (!token || !imageUrl) {
    return (
      <FiUser size={size} role="img" aria-label="기본 프로필" />
    );
  }

  const currentImage =
    image?.token === token && image?.imageUrl === imageUrl
      ? image
      : null;

  if (currentImage?.status === 'error') {
    return (
      <ImageStatus role="alert">사진 불러오기 실패</ImageStatus>
    );
  }

  const isLoading = currentImage?.status !== 'loaded';

  // AI작성: 실제 이미지 표시가 완료될 때까지 로딩 안내를 유지합니다.
  const updateImageStatus = (status) => {
    setImage((current) =>
      current === currentImage ? { ...current, status } : current,
    );
  };

  return (
    <>
      {isLoading && (
        <ImageStatus role="status">사진을 준비하고 있어요</ImageStatus>
      )}

      {currentImage?.src && (
        <Image
          src={currentImage.src}
          alt="내 프로필 사진"
          $loading={isLoading}
          onLoad={() => updateImageStatus('loaded')}
          onError={() => updateImageStatus('error')}
        />
      )}
    </>
  );
}

export default ProfileImage;