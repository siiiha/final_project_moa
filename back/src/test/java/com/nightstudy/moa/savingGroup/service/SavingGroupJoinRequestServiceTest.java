package com.nightstudy.moa.savingGroup.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 목록 반환 전 계주 권한 확인과 필터·페이지·개인정보 최소화 계약을 검증한다.
class SavingGroupJoinRequestServiceTest {
    private SavingGroupMapper mapper;
    private SavingGroupService service;
    private SavingGroupJoinTargetRow target;
    private SavingGroupMembershipRelationRow owner;

    @BeforeEach
    void setUp() {
        mapper = mock(SavingGroupMapper.class);
        service = new SavingGroupServiceImpl(mapper);
        target = new SavingGroupJoinTargetRow();
        target.setGroupId(30L);
        target.setOwnerMemberId(40L);
        owner = new SavingGroupMembershipRelationRow();
        owner.setMemberRole("OWNER");
        owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupJoinTargetForUpdate(10L)).thenReturn(target);
        when(mapper.selectSavingGroupMembershipForUpdate(30L, 40L)).thenReturn(owner);
    }

    @Test
    void 정상목록은공통페이지와내부모임ID를사용한다() {
        var row = new SavingGroupJoinRequestRow();
        row.setCrewMemberId("9007199254740993");
        when(mapper.selectSavingGroupJoinRequestCount(30L, null)).thenReturn(11);
        when(mapper.selectSavingGroupJoinRequests(30L, null, 10, 10)).thenReturn(List.of(row));
        var result = service.getSavingGroupJoinRequests(10L, 40L, null, 2);
        assertEquals("9007199254740993", result.getItems().get(0).getCrewMemberId());
        assertEquals(11, result.getPageMeta().getTotal());
        assertEquals(2, result.getPageMeta().getTotalPages());
        var order = inOrder(mapper);
        order.verify(mapper).selectSavingGroupJoinTargetForUpdate(10L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(30L, 40L);
        order.verify(mapper).selectSavingGroupJoinRequestCount(30L, null);
        order.verify(mapper).selectSavingGroupJoinRequests(30L, null, 10, 10);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "APPROVED", "REJECTED"})
    void 상태필터를개수와목록에동일하게전달한다(String status) {
        when(mapper.selectSavingGroupJoinRequestCount(30L, status)).thenReturn(1);
        when(mapper.selectSavingGroupJoinRequests(30L, status, 0, 10)).thenReturn(List.of());
        service.getSavingGroupJoinRequests(10L, 40L, status, 1);
        verify(mapper).selectSavingGroupJoinRequests(30L, status, 0, 10);
    }

    @Test
    void 비계주는목록을조회하지못한다() {
        target.setOwnerMemberId(41L);
        fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 40L, null, 1);
        verify(mapper, never()).selectSavingGroupJoinRequestCount(any(), any());
    }

    @Test
    void 계주관계누락차단() {
        when(mapper.selectSavingGroupMembershipForUpdate(30L, 40L)).thenReturn(null);
        fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 40L, null, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEMBER", "MANAGER"})
    void 생성회원이어도OWNER관계필요(String role) {
        owner.setMemberRole(role);
        fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 40L, null, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "REJECTED", "LEFT", "EXPELLED"})
    void 승인되지않은계주차단(String status) {
        owner.setMembershipStatus(status);
        fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 40L, null, 1);
    }

    @Test
    void 삭제또는없는모임은404() {
        when(mapper.selectSavingGroupJoinTargetForUpdate(10L)).thenReturn(null);
        fails("SAVING_GROUP_NOT_FOUND", 10L, 40L, null, 1);
    }

    @Test
    void 빈목록은정상응답() {
        var result = service.getSavingGroupJoinRequests(10L, 40L, null, 1);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getPageMeta().getTotalPages());
        verify(mapper, never()).selectSavingGroupJoinRequests(any(), any(), anyInt(), anyInt());
    }

    @Test
    void 매우큰페이지는offset계산없이빈목록() {
        when(mapper.selectSavingGroupJoinRequestCount(30L, null)).thenReturn(11);
        assertTrue(service.getSavingGroupJoinRequests(10L, 40L, null, Integer.MAX_VALUE).getItems().isEmpty());
        verify(mapper, never()).selectSavingGroupJoinRequests(any(), any(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ALL", "LEFT", "EXPELLED", "pending"})
    void 잘못된상태는조회전차단(String status) {
        fails("BAD_REQUEST", 10L, 40L, status, 1);
        verifyNoInteractions(mapper);
    }

    @Test
    void 잘못된ID페이지및인증을조회전차단() {
        fails("BAD_REQUEST", null, 40L, null, 1);
        fails("BAD_REQUEST", 0L, 40L, null, 1);
        fails("BAD_REQUEST", 10L, 40L, null, 0);
        fails("UNAUTHORIZED", 10L, null, null, 1);
        fails("UNAUTHORIZED", 10L, -1L, null, 1);
        verifyNoInteractions(mapper);
    }

    @Test
    void 실제매퍼의마스킹필터정렬및바인딩() throws Exception {
        var config = new org.apache.ibatis.session.Configuration();
        config.getTypeAliasRegistry().registerAlias("SavingGroupCreationRequest",
                com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest.class);
        try (var input = getClass().getClassLoader().getResourceAsStream("mappers/SavingGroupMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(input, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        var bound = config.getMappedStatement(prefix + "selectSavingGroupJoinRequests")
                .getBoundSql(Map.of("groupId", 30L, "status", "PENDING", "offset", 10, "limit", 10));
        String sql = bound.getSql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("CAST(gm.crew_member_id AS CHAR)"));
        assertTrue(sql.contains("CONCAT(LEFT(m.name, 1), REPEAT('*'"));
        assertTrue(sql.contains("gm.group_id = ?"));
        assertTrue(sql.contains("gm.member_role != 'OWNER'"));
        assertTrue(sql.contains("m.deleted_at IS NULL"));
        assertTrue(sql.contains("gm.membership_status IN ('PENDING', 'APPROVED', 'REJECTED')"));
        assertTrue(sql.contains("ORDER BY gm.requested_at DESC, gm.crew_member_id DESC"));
        assertFalse(sql.contains("m.phone"));
        assertFalse(sql.contains("m.email"));
        assertEquals(List.of("groupId", "status", "limit", "offset"),
                bound.getParameterMappings().stream().map(org.apache.ibatis.mapping.ParameterMapping::getProperty).toList());
        assertTrue(config.getMappedStatement(prefix + "selectSavingGroupJoinRequestCount")
                .getBoundSql(Map.of("groupId", 30L, "status", "PENDING")).getSql().contains("gm.membership_status = ?"));
    }

    private void fails(String code, Long groupId, Long memberId, String status, int page) {
        assertEquals(code, assertThrows(ApiException.class,
                () -> service.getSavingGroupJoinRequests(groupId, memberId, status, page)).getCode());
    }
}
