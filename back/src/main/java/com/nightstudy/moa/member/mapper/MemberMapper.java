package com.nightstudy.moa.member.mapper;

import com.nightstudy.moa.member.dto.MemberInsertDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nightstudy.moa.member.dto.MemberLoginDto;
import com.nightstudy.moa.member.dto.MemberProfileResponse;
import java.util.List;

@Mapper
public interface MemberMapper {

    int countByEmail(@Param("email") String email);

    int countRegionById(@Param("regionId") Long regionId);

    int insertMember(MemberInsertDto member);

    int countVerifiedEmail(@Param("email") String email);

    int countRequiredTerms();

    int countRequiredAgreedTerms(
            @Param("termIds") java.util.List<Long> termIds
    );

    int insertTermAgreement(
            @Param("memberId") Long memberId,
            @Param("termId") Long termId
    );

    int countByNickname(@Param("nickname") String nickname);

    int countInterestById(@Param("interestId") Long interestId);

    int insertMemberInterest(
            @Param("memberId") Long memberId,
            @Param("interestId") Long interestId
    );

    int countTermById(@Param("termId") Long termId);

    MemberLoginDto findByEmail(@Param("email") String email);

    MemberProfileResponse findProfileByMemberId(
            @Param("memberId") Long memberId
    );

    List<MemberProfileResponse.Interest> findInterestsByMemberId(
            @Param("memberId") Long memberId
    );

    // 수정 대상 회원을 조회하고 동시에 변경되지 않도록 잠금
    MemberLoginDto findByMemberIdForUpdate(
            @Param("memberId") Long memberId
    );

    // 본인 제외 동일 닉네임 사용하는 회원 수 조회
    int countByNicknameExcludingMember(
            @Param("nickname") String nickname,
            @Param("memberId")  Long memberId
    );

    // 로그인한 회원의 닉네임 변경
    int updateNickname(
            @Param("memberId") Long memberId,
            @Param("nickname") String nickname
    );

    // 관심사 목록 교체 위해 회원의 기존 선택 네역 삭제
    int deleteMemberInterests(
            @Param("memberId") Long memberId
    );

    // 암호화된 새 비밀번호 저장
    int updatePassword(
            @Param("memberId") Long memberId,
            @Param("passwordHash") String passwordHash
    );
}
