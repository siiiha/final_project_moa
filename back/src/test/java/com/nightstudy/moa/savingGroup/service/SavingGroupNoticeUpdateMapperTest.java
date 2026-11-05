package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

// AI작성: 실제 XML을 파싱해 수정 소속·미삭제 조건 및 감사 필드 보존을 검증한다. DB 실행 검증은 아니다.
class SavingGroupNoticeUpdateMapperTest {
    @Test void updateAndLockKeepScopeAndDeletedFilter() throws Exception {
        var configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, configuration, "mappers/SavingGroupMapper.xml", configuration.getSqlFragments()).parse();
        }
        String namespace = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        var parameters = Map.of("groupId", 20L, "noticeId", 40L, "title", "제목", "content", "내용");
        var update = configuration.getMappedStatement(namespace + "updateSavingGroupNotice").getBoundSql(parameters);
        assertThat(update.getSql().replaceAll("\\s+", " "))
                .contains("updated_at = CURRENT_TIMESTAMP", "group_id = ? AND notice_id = ? AND deleted_at IS NULL")
                .doesNotContain("writer_id =", "created_at =", "is_pinned =");
        assertThat(update.getParameterMappings()).extracting(p -> p.getProperty())
                .containsExactly("title", "content", "groupId", "noticeId");
        assertThat(configuration.getMappedStatement(namespace + "selectSavingGroupNoticeForUpdate")
                .getBoundSql(parameters).getSql()).contains("deleted_at IS NULL", "FOR UPDATE");
    }
}
