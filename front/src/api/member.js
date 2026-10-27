import client from './client';

// 이메일과 비밀번호로 로그인하고 JWT를 반환
export async function loginMember(email, password) {
    const response = await client.post('/auth/login', { 
        email, 
        password,
    });
    
    return response.data.data;
}

export async function getMyProfile(token){
    const response = await client.get('/members/me', { 
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    return response.data.data;
}

// 현재 비밀번호 확인 후 회원정보를 수정
export async function updateMyProfile(token, request) {
    const response = await client.put('/members/me', request, {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    return response.data.data;
}

// 현재 DB에서 확인한 관심사 번호, 이름
// 관심사 목록 조회 API 추가 되면 서보 조회로 교체
export const memberInterestOptions = [
    { interestId: 1, interestName: '산책' },
    { interestId: 2, interestName: '독서' },
];

// 현재 비밀번호 확인하고 새 비밀번호로 수정
export async function updateMyPassword(token, request) {
    const response = await client.put(
        '/members/me/password',
        request,
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        }
    );

    return response.data.data;
}

export async function logoutMember(token) {
    const response = await client.post('/auth/logout', null, {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    return response.data;
}

// 이메일의 사용 가능 여부 확인
export async function checkEmailAvailable(email) {
    const response = await client.post('/auth/check-email', {
email });
    return response.data.data;
}

// 닉네임 사용 가능 여부 확인
export async function checkNicknameAvailable(nickname) {
    const response = await client.post('/auth/check-nickname',
        { nickname });
    return response.data.data;
}
// AI작성: 프로필 사진을 등록하거나 변경합니다.
export async function uploadMyProfileImage(token, file) {
    const form = new FormData();
    form.append('file', file);

    const response = await client.put(
        '/members/me/profile-image',
        form,
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        },
    );

    return response.data.data;
}

// AI작성: 인증 목적을 지정하고, 기존 회원가입은 SIGNUP을 기본값으로 사용한다.
export async function sendEmailVerification(email, purpose = 'SIGNUP') {
  const response = await client.post('/auth/email-verifications', {
    email,
    purpose,
  });

  return response.data.data;
}

// 인증번호 확인 후 회원가입용 인증 토큰 반환
export async function verifyEmailCode(verificationRequestId, code) {
    const response = await client.post('/auth/email-verifications/verify', {
        verificationRequestId,
        code,
    });
    return response.data.data;
}

// 인증 완료 토큰 회원정보 전달 후 회원가입
export async function signupMember(request) {
    const response = await client.post('/auth/signup',
request);
    return response.data.data;
}

// AI작성: 로그인 인증을 포함하여 사진 파일을 가져옵니다.
export async function getMyProfileImage(token, imageUrl) {
    const response = await client.get(
        imageUrl.replace(/^\/api\//, '/'),
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
            responseType: 'blob',
        },
    );

    return response.data;
}

// AI작성: 프로필 사진을 삭제합니다.
export async function deleteMyProfileImage(token) {
    const response = await client.delete(
        '/members/me/profile-image',
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        },
    );

    return response.data;
}

export const memberRegionOptions = [
    { regionId: 6, regionName: '서울특별시 강남구' },
    { regionId: 7, regionName: '서울특별시 송파구' },
    { regionId: 8, regionName: '서울특별시 마포구' },
];

// 현재 DB의 테스트용 약관과 연결
export const memberTermOptions = [
    {
        termId: 1,
        title: '[테스트] 서비스 이용약관',
        required: true,
    },
    {
        termId: 2,
        title: '[테스트] 개인정보 수집·이용 동의',
        required: true,
    },
];

export async function withdrawMyAccount(token, request) {
    const response = await client.delete('/members/me', {
        headers: {
            Authorization: `Bearer ${token}`,
        },
        data: request,
    });
}

// AI작성: 인증된 소셜 계정과 추가 회원정보로 가입을 완료한다.
export async function signupSocialMember(request) {
  const response = await client.post('/auth/social/signup', request);
  return response.data.data;
}

// AI작성: 이메일 인증 완료 후 가입 계정의 존재 여부를 확인한다.
export async function findMemberAccount(email, emailVerificationToken) {
  const response = await client.post(
    '/auth/email-verifications/find-account',
    {
      email,
      emailVerificationToken,
    },
  );

  return response.data.data;
}

// AI작성: 이메일 인증 완료 토큰과 새 비밀번호로 재설정을 요청한다.
export async function resetMemberPassword(
  email,
  emailVerificationToken,
  newPassword,
  newPasswordConfirm,
) {
  const response = await client.post('/auth/password/reset', {
    email,
    emailVerificationToken,
    newPassword,
    newPasswordConfirm,
  });

  return response.data;
}