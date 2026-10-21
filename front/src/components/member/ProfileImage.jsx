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
`;

// AI작성: 인증이 필요한 프로필 사진을 가져와 표시합니다.
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
        });
      } catch {
        if (!cancelled) {
          setImage(null);
        }
      }
    }

    loadImage();

    // AI작성: 사진 변경 또는 화면 종료 시 임시 주소를 해제합니다.
    return () => {
      cancelled = true;

      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [token, imageUrl]);

  const imageSrc =
    image?.token === token && image?.imageUrl === imageUrl
      ? image.src
      : null;

  if (!imageSrc) {
    return (
      <FiUser
        size={size}
        role="img"
        aria-label="기본 프로필"
      />
    );
  }

  return (
    <Image
      src={imageSrc}
      alt="내 프로필 사진"
      onError={() => setImage(null)}
    />
  );
}

export default ProfileImage;