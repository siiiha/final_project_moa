package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionReleaseRequest;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionRequest;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionTarget;
import com.nightstudy.moa.admin.mapper.AdminMemberMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.List;
import java.time.LocalDateTime;
import com.nightstudy.moa.member.service.AuthSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 관리자 회원 조회의 페이징과 대상 없음 예외를 처리한다.
public class AdminMemberServiceImpl implements AdminMemberService {
    private final AdminMemberMapper adminMemberMapper;
    private final MemberRestrictionPolicy memberRestrictionPolicy;
    private final AuthSessionService authSessionService;

    public AdminMemberServiceImpl(
            AdminMemberMapper adminMemberMapper,
            MemberRestrictionPolicy memberRestrictionPolicy,
            AuthSessionService authSessionService
    ) {
        this.adminMemberMapper = adminMemberMapper;
        this.memberRestrictionPolicy = memberRestrictionPolicy;
        this.authSessionService = authSessionService;
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

    @Override
    @Transactional
    public void restrictMember(
            Long administratorId,
            Long memberId,
            AdminMemberRestrictionRequest request
    ) {
        validateNotSelf(administratorId, memberId);
        AdminMemberRestrictionTarget target = getRestrictionTarget(memberId);

        if ("INACTIVE".equals(target.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INACTIVE_MEMBER_RESTRICTION_NOT_ALLOWED",
                    "비활성 회원은 이용 제한할 수 없습니다."
            );
        }

        LocalDateTime restrictedUntil = memberRestrictionPolicy.calculateSuspendedUntil(
                target.getAccountStatus(),
                target.getSuspendedUntil(),
                request.getRestrictionType(),
                LocalDateTime.now()
        );

        if (adminMemberMapper.updateMemberRestriction(memberId, restrictedUntil) != 1) {
            throw restrictionConflict();
        }
        if (adminMemberMapper.insertRestrictionHistory(
                memberId,
                "RESTRICT",
                request.getRestrictionType(),
                request.getReason(),
                restrictedUntil
        ) != 1) {
            throw restrictionConflict();
        }

        // AI작성: 상태 확인으로 기존 JWT를 차단하면서 저장된 로그인 세션도 즉시 모두 폐기한다.
        authSessionService.revokeAllSessions(memberId);
    }

    @Override
    @Transactional
    public void releaseMemberRestriction(
            Long administratorId,
            Long memberId,
            AdminMemberRestrictionReleaseRequest request
    ) {
        validateNotSelf(administratorId, memberId);
        AdminMemberRestrictionTarget target = getRestrictionTarget(memberId);

        if (!"SUSPENDED".equals(target.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "MEMBER_NOT_SUSPENDED",
                    "이용 제한 상태인 회원만 제한을 해제할 수 있습니다."
            );
        }

        if (adminMemberMapper.releaseMemberRestriction(memberId) != 1
                || adminMemberMapper.insertRestrictionHistory(
                        memberId,
                        "RELEASE",
                        null,
                        request.getReason(),
                        null
                ) != 1) {
            throw restrictionConflict();
        }
    }

    private AdminMemberRestrictionTarget getRestrictionTarget(Long memberId) {
        AdminMemberRestrictionTarget target = adminMemberMapper.selectRestrictionTargetForUpdate(memberId);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.");
        }
        return target;
    }

    // AI작성: 인증에서 얻은 관리자 번호와 대상 번호를 비교해 관리자 본인 제한을 막는다.
    private void validateNotSelf(Long administratorId, Long memberId) {
        if (administratorId.equals(memberId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ADMIN_SELF_RESTRICTION_NOT_ALLOWED",
                    "관리자는 본인의 이용을 제한하거나 해제할 수 없습니다."
            );
        }
    }

    private ApiException restrictionConflict() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "MEMBER_RESTRICTION_FAILED",
                "회원 이용 제한 상태가 변경되어 처리하지 못했습니다."
        );
    }
}
