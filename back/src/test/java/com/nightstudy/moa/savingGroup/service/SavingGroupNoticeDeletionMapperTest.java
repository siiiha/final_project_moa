package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 실제 XML 바인딩에서 삭제 범위와 모든 조회·수정·고정 경로의 미삭제 조건을 확인한다. MySQL 실행 검증은 아니다.
class SavingGroupNoticeDeletionMapperTest {
    Configuration configuration;
    final String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
    final Map<String, Object> parameters = Map.of("groupId", 20L, "noticeId", 40L, "memberId", 30L,
            "sort", "LATEST", "offset", 0, "pinned", true, "title", "제목", "content", "내용");
    @BeforeEach void parse() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, configuration, "mappers/SavingGroupMapper.xml", configuration.getSqlFragments()).parse();
        }
    }
    @Test void deleteRequiresWriterAndScopeAndOnlySetsDeletionTime() {
        var bound = configuration.getMappedStatement(prefix + "deleteSavingGroupNotice").getBoundSql(parameters);
        assertThat(bound.getSql().replaceAll("\\s+", " "))
                .contains("SET deleted_at = CURRENT_TIMESTAMP", "group_id = ? AND notice_id = ?", "writer_id = ? AND deleted_at IS NULL")
                .doesNotContain("DELETE FROM", "SET writer_id", "SET content");
        assertThat(bound.getParameterMappings()).extracting(p -> p.getProperty()).containsExactly("groupId", "noticeId", "memberId");
    }
    @ParameterizedTest @ValueSource(strings = {"selectRecentNotices", "selectSavingGroupNoticeCount", "selectSavingGroupNotices",
            "selectSavingGroupPinnedNotices", "selectSavingGroupNoticePinForUpdate", "selectSavingGroupPinnedNoticeIdsForUpdate",
            "updateSavingGroupNoticePinned", "selectSavingGroupNoticeDetail", "selectSavingGroupNoticeForUpdate", "updateSavingGroupNotice"})
    void deletedNoticesAreExcludedFromEverySavingGroupPath(String statement) {
        assertThat(configuration.getMappedStatement(prefix + statement).getBoundSql(parameters).getSql())
                .contains("deleted_at IS NULL");
    }
}
