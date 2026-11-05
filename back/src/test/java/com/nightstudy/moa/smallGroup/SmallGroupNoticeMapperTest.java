package com.nightstudy.moa.smallGroup;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
// AI작성: 공지 SQL의 삭제 제외·고정 분리·작성자 제한·행 잠금을 실제 MyBatis 파서로 확인한다.
class SmallGroupNoticeMapperTest {
 @Test void parsesAndRestrictsNoticeStatements() throws Exception {
  var configuration=new Configuration();
  try(var input=getClass().getResourceAsStream("/mappers/SmallGroupMapper.xml")) {
   new XMLMapperBuilder(input,configuration,"mappers/SmallGroupMapper.xml",configuration.getSqlFragments()).parse();
  }
  var params=Map.of("groupId",20L,"noticeId",9L,"writerId",3L,"offset",0,"sort","LATEST","pinned",true,"title","제목","content","본문");
  String prefix="com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper.";
  for(String id: new String[]{"countSmallGroupNotices","selectSmallGroupNoticePage","selectSmallGroupNoticeDetail","selectSmallGroupNoticeForUpdate","selectSmallGroupPinnedNotices","selectSmallGroupNoticePinForUpdate"}) {
   String sql=configuration.getMappedStatement(prefix+id).getBoundSql(params).getSql();assertThat(sql).contains("deleted_at IS NULL");
  }
  assertThat(configuration.getMappedStatement(prefix+"selectSmallGroupNoticePage").getBoundSql(params).getSql()).contains("is_pinned = 0","DESC");
  assertThat(configuration.getMappedStatement(prefix+"selectSmallGroupNoticeForUpdate").getBoundSql(params).getSql()).contains("FOR UPDATE");
  assertThat(configuration.getMappedStatement(prefix+"deleteSmallGroupNotice").getBoundSql(params).getSql()).contains("writer_id =","deleted_at = CURRENT_TIMESTAMP");
 }
}
