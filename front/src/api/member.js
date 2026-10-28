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

// AI작성: DB의 관심사 ID를 유지하고 소모임 대분류 순서로 표시한다.
export const memberInterestOptions = [
  { interestId: 1, interestName: '운동/스포츠' },
  { interestId: 3, interestName: '사교/인맥' },
  { interestId: 2, interestName: '인문학/책/글' },
  { interestId: 4, interestName: '아웃도어/여행' },
  { interestId: 5, interestName: '음악/악기' },
  { interestId: 6, interestName: '업종/직무' },
  { interestId: 7, interestName: '문화/공연/축제' },
  { interestId: 8, interestName: '외국/언어' },
  { interestId: 9, interestName: '게임/오락' },
  { interestId: 10, interestName: '공예/만들기' },
  { interestId: 11, interestName: '댄스/무용' },
  { interestId: 12, interestName: '봉사활동' },
  { interestId: 13, interestName: '사진/영상' },
  { interestId: 14, interestName: '자기계발' },
  { interestId: 15, interestName: '스포츠관람' },
  { interestId: 16, interestName: '반려동물' },
  { interestId: 17, interestName: '요리/제조' },
  { interestId: 18, interestName: '자동차/바이크' },
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

// AI작성: 일반·소셜 회원가입에서 사용할 약관을 서버에서 조회한다.
export async function getMemberTerms() {
  const response = await client.get('/auth/terms');
  return response.data.data;
}

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