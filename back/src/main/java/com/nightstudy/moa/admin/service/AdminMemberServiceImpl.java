package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.mapper.AdminMemberMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 관리자 회원 조회의 페이징과 대상 없음 예외를 처리한다.
public class AdminMemberServiceImpl implements AdminMemberService {
    private final AdminMemberMapper adminMemberMapper;

    public AdminMemberServiceImpl(AdminMemberMapper adminMemberMapper) {
        this.adminMemberMapper = adminMemberMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMemberListResult getMemberList(AdminMemberSearchCondition condition) {
        int total = adminMemberMapper.selectMemberCount(condition);
        PageMeta pageMeta = PageMeta.of(condition.getPage(), condition.getSize(), total);
        if (total == 0 || pageMeta.getPage() > pageMeta.getTotalPages()) {
            return new AdminMemberListResult(List.of(), pageMeta);
        }

        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());
        return new AdminMemberListResult(adminMemberMapper.selectMemberList(condition), pageMeta);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMemberDetailResponse getMemberDetail(Long memberId) {
        AdminMemberDetailResponse detail = adminMemberMapper.selectMemberDetail(memberId);
        if (detail == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "회원을 찾을 수 없습니다."
            );
        }
        return detail;
    }
}
