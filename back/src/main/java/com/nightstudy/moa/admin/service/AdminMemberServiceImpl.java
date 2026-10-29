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
import com.nightstudy.moa.member.service.AuthSessionService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 관리자 회원 조회의 페이징과 대상 없음 예외를 처리한다.
public class AdminMemberServiceImpl implements AdminMemberService {
    private final AdminMemberMapper adminMemberMapper;
    private final MemberRestrictionPolicy memberRestrictionPolicy;
    private final AuthSessionService authSessionService;

    // AI작성: 조회·상태 변경 Mapper와 공통 제한 정책, 기존 세션 폐기 기능을 재사용한다.
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
    // AI작성: 전체 건수로 페이지 정보를 계산하고 유효한 페이지일 때만 회원 목록을 조회한다.
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
    // AI작성: 회원 상세가 없으면 공통 404 예외로 변환해 Controller에 전달한다.
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
    // AI작성: 대상 잠금, 제한 상태 변경, 이력 저장, 세션 폐기를 하나의 트랜잭션으로 처리한다.
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
    // AI작성: 현재 제한 상태를 확인한 뒤 상태 복구와 해제 이력을 하나의 트랜잭션으로 저장한다.
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

    // AI작성: 동시 요청에 사용될 잠긴 회원 정보를 조회하고 존재하지 않으면 404로 처리한다.
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

    // AI작성: 상태 변경 또는 이력 저장 건수가 예상과 다를 때 사용할 공통 충돌 예외를 생성한다.
    private ApiException restrictionConflict() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "MEMBER_RESTRICTION_FAILED",
                "회원 이용 제한 상태가 변경되어 처리하지 못했습니다."
        );
    }
}
