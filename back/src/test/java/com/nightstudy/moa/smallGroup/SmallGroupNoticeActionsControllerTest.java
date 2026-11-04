package com.nightstudy.moa.smallGroup;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.common.exception.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
// AI작성: 수정·삭제·고정 경로의 입력 검증과 인증 회원 전달·서비스 오류를 확인한다.
class SmallGroupNoticeActionsControllerTest {
 SmallGroupService service; MockMvc mvc;
 @BeforeEach void setup() {service=mock(SmallGroupService.class);mvc=MockMvcBuilders.standaloneSetup(new SmallGroupController(null,service)).setControllerAdvice(new GlobalExceptionHandler()).build();}
 @Test void editsWithAuthenticatedMember() throws Exception {
  mvc.perform(put("/api/small-groups/10/notices/9").requestAttr("memberId",3L).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제목\",\"content\":\"본문\"}"))
   .andExpect(status().isOk());verify(service).updateSmallGroupNotice(10L,9L,3L,"제목","본문");
 }
 @Test void deletesWithAuthenticatedMember() throws Exception {
  mvc.perform(delete("/api/small-groups/10/notices/9").requestAttr("memberId",3L)).andExpect(status().isOk());verify(service).deleteSmallGroupNotice(10L,9L,3L);
 }
 @ParameterizedTest @ValueSource(strings={"true","false"}) void pinsWithExplicitBoolean(String value) throws Exception {
  mvc.perform(patch("/api/small-groups/10/notices/9/pin").requestAttr("memberId",3L).contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":"+value+"}"))
   .andExpect(status().isOk());verify(service).updateSmallGroupNoticePinned(10L,9L,3L,Boolean.valueOf(value));
 }
 @Test void rejectsMissingPinValue() throws Exception {
  mvc.perform(patch("/api/small-groups/10/notices/9/pin").requestAttr("memberId",3L).contentType(MediaType.APPLICATION_JSON).content("{}"))
   .andExpect(status().isBadRequest());verifyNoInteractions(service);
 }
 @Test void preservesPinLimitError() throws Exception {
  doThrow(new ApiException(HttpStatus.CONFLICT,"SMALL_GROUP_NOTICE_PIN_LIMIT_EXCEEDED","최대 2개")).when(service).updateSmallGroupNoticePinned(10L,9L,3L,true);
  mvc.perform(patch("/api/small-groups/10/notices/9/pin").requestAttr("memberId",3L).contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("SMALL_GROUP_NOTICE_PIN_LIMIT_EXCEEDED"));
 }
 @Test void rejectsNegativeNoticeId() throws Exception {
  mvc.perform(delete("/api/small-groups/10/notices/0").requestAttr("memberId",3L)).andExpect(status().isBadRequest());verifyNoInteractions(service);
 }
}
