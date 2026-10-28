package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

// AI작성: 설정 조회용 MyBatis statement와 constructor resultMap이 애플리케이션 기동 전 파싱되는지 확인한다.
class SmallGroupMapperXmlTest {

    private static final String NAMESPACE = "com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper.";

    @Test
    void parsesSmallGroupSettingsQueryStatements() {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mappers/SmallGroupMapper.xml")) {
            assertThat(input).isNotNull();

            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    input, configuration, "mappers/SmallGroupMapper.xml", configuration.getSqlFragments());
            mapperBuilder.parse();
        } catch (Exception exception) {
            throw new AssertionError("소모임 Mapper XML을 파싱할 수 없습니다.", exception);
        }

        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupSettingsAuthority")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupCurrentSettings")).isTrue();
        assertThat(configuration.getResultMapNames()).contains(NAMESPACE + "smallGroupSettings");
    }
}
