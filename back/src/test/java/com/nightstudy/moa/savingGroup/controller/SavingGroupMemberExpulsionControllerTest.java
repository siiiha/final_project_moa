package com.nightstudy.moa.savingGroup.controller;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
// AI작성: 두 경로 별칭·서버 인증 사용자·양수 ID 및 공통 오류 응답을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupMemberExpulsionControllerTest {
 @Mock SavingGroupService service;
 MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.standaloneSetup(new SavingGroupController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();}
 @ParameterizedTest @ValueSource(strings={"/saving-groups","/api/saving-groups"})
 void success(String base)throws Exception{
  mvc.perform(patch(base+"/20/members/50/expel").requestAttr("memberId",30L).param("memberId","999"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
  verify(service).expelApprovedSavingGroupMember(20L,50L,30L);
 }
 @ParameterizedTest @ValueSource(strings={"0/members/50","-1/members/50","abc/members/50","20/members/0","20/members/-1","20/members/abc","9223372036854775808/members/50"})
 void invalidIds(String path)throws Exception{
  mvc.perform(patch("/api/saving-groups/"+path+"/expel")).andExpect(status().isBadRequest());verifyNoInteractions(service);
 }
 @ParameterizedTest @ValueSource(ints={401,403,404,409})
 void serviceErrors(int code)throws Exception{
  doThrow(new ApiException(HttpStatus.valueOf(code),"TEST_ERROR","추방 실패")).when(service).expelApprovedSavingGroupMember(20L,50L,30L);
  mvc.perform(patch("/api/saving-groups/20/members/50/expel").requestAttr("memberId",30L)).andExpect(status().is(code)).andExpect(jsonPath("$.error.code").value("TEST_ERROR"));
 }
 @Test void cannotSupplyActorViaQuery()throws Exception{
  doThrow(new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","로그인이 필요합니다.")).when(service).expelApprovedSavingGroupMember(20L,50L,null);
  mvc.perform(patch("/api/saving-groups/20/members/50/expel").param("memberId","30")).andExpect(status().isUnauthorized());
  verify(service).expelApprovedSavingGroupMember(20L,50L,null);
 }
}
