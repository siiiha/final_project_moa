package com.nightstudy.moa.member.mapper;

import com.nightstudy.moa.member.dto.MemberInsertDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nightstudy.moa.member.dto.MemberLoginDto;

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
}
