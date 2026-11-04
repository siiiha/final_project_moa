package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticePinRequest;
import jakarta.validation.Validation;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 기존 Service 테스트 위치에서 실제 MyBatis 매핑·파라미터와 요청 검증을 확인한다. DB SQL 실행은 별도다.
class SavingGroupNoticePinMapperTest {
    private Configuration configuration;

    @BeforeEach
    void parseMapper() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        String resource = "mappers/SavingGroupMapper.xml";
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            new XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"LATEST", "OLDEST"})
    void normalPageExcludesPinnedNoticesAndPreservesSort(String sort) {
        var bound = sql("selectSavingGroupNotices", Map.of("groupId", 20L, "sort", sort, "offset", 4));
        assertThat(normalize(bound)).contains("gn.group_id = ? AND gn.is_pinned = 0", "LIMIT 4 OFFSET ?");
        String direction = "OLDEST".equals(sort) ? "ASC" : "DESC";
        assertThat(normalize(bound)).contains("ORDER BY gn.created_at " + direction + ", gn.notice_id " + direction);
        assertThat(bound.getParameterMappings()).extracting(mapping -> mapping.getProperty()).containsExactly("groupId", "offset");
        assertThat(normalize(sql("selectSavingGroupNoticeCount", Map.of("groupId", 20L))))
                .contains("WHERE group_id = ? AND is_pinned = 0");
    }

    @Test
    void pinnedSummaryUsesLatestOrderAndExistingMappingWithoutPageLimit() {
        var bound = sql("selectSavingGroupPinnedNotices", Map.of("groupId", 20L));
        assertThat(normalize(bound)).contains("gn.is_pinned = 1", "ORDER BY gn.created_at DESC, gn.notice_id DESC")
                .doesNotContain("LIMIT", "OFFSET", "FOR UPDATE");
        assertThat(configuration.getMappedStatement(namespace("selectSavingGroupPinnedNotices")).getResultMaps().get(0).getId())
                .endsWith(".noticeSummary");
    }

    @Test
    void groupLockUsesCommonGroupIdAndExcludesDeletedGroups() {
        var bound = sql("selectSavingGroupNoticeTargetForUpdate", Map.of("groupId", 20L));
        assertThat(normalize(bound)).contains("sg.group_id = ?", "g.deleted_at IS NULL", "FOR UPDATE");
        assertThat(bound.getParameterMappings()).extracting(mapping -> mapping.getProperty()).containsExactly("groupId");
    }

    @Test
    void noticeStateLockRequiresBothIdsAndMapsBooleanState() {
        var bound = sql("selectSavingGroupNoticePinForUpdate", Map.of("groupId", 20L, "noticeId", 40L));
        assertThat(normalize(bound)).contains("WHERE group_id = ? AND notice_id = ?", "FOR UPDATE");
        assertThat(bound.getParameterMappings()).extracting(mapping -> mapping.getProperty()).containsExactly("groupId", "noticeId");
        assertThat(configuration.getResultMap(namespace("noticePin")).getConstructorResultMappings().get(1).getJavaType())
                .isEqualTo(Boolean.class);
    }

    @Test
    void pinnedLimitUsesAllCurrentLockedIdsInsteadOfSnapshotCountOrTruncatedList() {
        var bound = sql("selectSavingGroupPinnedNoticeIdsForUpdate", Map.of("groupId", 20L));
        assertThat(normalize(bound)).contains("SELECT notice_id", "group_id = ? AND is_pinned = 1", "FOR UPDATE")
                .doesNotContain("COUNT(", "LIMIT");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pinUpdateBindsStateAndBothIdsAndSkipsUnchangedState(boolean pinned) {
        var bound = sql("updateSavingGroupNoticePinned", Map.of("groupId", 20L, "noticeId", 40L, "pinned", pinned));
        assertThat(normalize(bound)).contains("SET is_pinned = ?", "WHERE group_id = ? AND notice_id = ?", "AND is_pinned != ?");
        assertThat(bound.getParameterMappings()).extracting(mapping -> mapping.getProperty())
                .containsExactly("pinned", "groupId", "noticeId", "pinned");
    }

    @Test
    void requestRequiresExplicitTrueOrFalse() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new SavingGroupNoticePinRequest();
            assertThat(validator.validate(request)).hasSize(1);
            for (boolean pinned : List.of(true, false)) {
                request.setPinned(pinned);
                assertThat(validator.validate(request)).isEmpty();
            }
        }
    }

    private BoundSql sql(String id, Map<String, ?> parameters) {
        return configuration.getMappedStatement(namespace(id)).getBoundSql(parameters);
    }

    private String namespace(String id) {
        return "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper." + id;
    }

    private String normalize(BoundSql bound) {
        return bound.getSql().replaceAll("\\s+", " ").trim();
    }
}
