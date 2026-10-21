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


