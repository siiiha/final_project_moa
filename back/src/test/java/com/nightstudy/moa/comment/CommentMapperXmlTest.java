package com.nightstudy.moa.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Locale;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

// AI작성: 댓글 Mapper XML 파싱과 삭제 댓글 제외 집계 조건을 DB 없이 검증합니다.
class CommentMapperXmlTest {

    private static final String RESOURCE_PATH = "/mappers/CommentMapper.xml";
    private static final String TOTAL_COUNT_STATEMENT =
            "com.nightstudy.moa.comment.mapper.CommentMapper.selectTotalCommentCountByPostId";

    @Test
    void totalCommentCountExcludesDeletedComments() {
        Configuration configuration = new Configuration();

        try (InputStream input = getClass().getResourceAsStream(RESOURCE_PATH)) {
            assertThat(input).isNotNull();

            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    input,
                    configuration,
                    "mappers/CommentMapper.xml",
                    configuration.getSqlFragments()
            );
            mapperBuilder.parse();
        } catch (Exception exception) {
            throw new AssertionError("댓글 Mapper XML을 파싱할 수 없습니다.", exception);
        }

        MappedStatement statement = configuration.getMappedStatement(TOTAL_COUNT_STATEMENT);
        String sql = statement.getBoundSql(1L)
                .getSql()
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);

        assertThat(sql)
                .contains("where post_id = ?")
                .contains("and deleted_at is null");
    }
}
