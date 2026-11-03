package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.region.MemberRegionDto;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 구·동·세종 저장과 잘못된 지역의 저장 차단을 DB 없이 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupCreationServiceImplTest {
    @Mock private SmallGroupMapper mapper;
    @InjectMocks private SmallGroupCreationServiceImpl service;

    @ParameterizedTest
    @CsvSource({"서울특별시,강남구,", "서울특별시,강남구,역삼동", "세종특별자치시,'',", "세종특별자치시,'',어진동"})
    void storesVerifiedRegion(String province, String district, String town) {
        SmallGroupCreationRequest request = request();
        MemberRegionDto region = region();
        region.setProvinceName(province);
        region.setDistrictName(district);
        region.setTownName(town);
        region.setRegionName(province + (district.isEmpty() ? "" : " " + district)
                + (town == null ? "" : " " + town));
        request.setRegionName(" " + region.getRegionName() + " ");
        request.setRegionId(999L);
        when(mapper.existsActiveMember(10L)).thenReturn(true);
        when(mapper.selectCreationRegionsByName(region.getRegionName())).thenReturn(List.of(region));
        when(mapper.insertGroup(request)).thenAnswer(invocation -> { request.setGroupId(100L); return 1; });
        when(mapper.insertSmallGroup(request)).thenAnswer(invocation -> { request.setSmallGroupId(200L); return 1; });
        when(mapper.insertOwnerMembership(100L, 10L)).thenReturn(1);

        var result = service.createSmallGroup(request);

        assertThat(request.getRegionId()).isEqualTo(1L);
        assertThat(result.getRegionName()).isEqualTo(region.getRegionName());
        assertThat(result.getSmallGroupId()).isEqualTo(200L);
        verify(mapper).insertGroup(request);
        verify(mapper).insertOwnerMembership(100L, 10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "duplicate", "provinceOnly", "nullProvince", "nullDistrict", "blankDistrict", "nullId"})
    void rejectsInvalidRegionBeforeInserts(String kind) {
        SmallGroupCreationRequest request = request();
        MemberRegionDto region = region();
        if (kind.equals("provinceOnly")) region.setDistrictName("");
        if (kind.equals("nullProvince")) region.setProvinceName(null);
        if (kind.equals("nullDistrict")) region.setDistrictName(null);
        if (kind.equals("blankDistrict")) region.setDistrictName("  ");
        if (kind.equals("nullId")) region.setRegionId(null);
        when(mapper.existsActiveMember(10L)).thenReturn(true);
        when(mapper.selectCreationRegionsByName(request.getRegionName())).thenReturn(
                kind.equals("missing") ? List.of() : kind.equals("duplicate") ? List.of(region, region) : List.of(region));

        assertRegionRejected(request);
    }

    @Test
    void rejectsMissingRegionName() {
        when(mapper.existsActiveMember(10L)).thenReturn(true);
        for (String name : new String[] {null, "", "  "}) {
            SmallGroupCreationRequest request = request();
            request.setRegionName(name);
            assertRegionRejected(request);
        }
        verify(mapper, never()).selectCreationRegionsByName(any());
    }

    // AI작성: 잘못된 지역은 공통 오류를 반환하고 세 저장 작업을 모두 실행하지 않아야 한다.
    private void assertRegionRejected(SmallGroupCreationRequest request) {
        ApiException error = catchThrowableOfType(() -> service.createSmallGroup(request), ApiException.class);
        assertThat(error.getCode()).isEqualTo("REGION_NOT_FOUND");
        verify(mapper, never()).insertGroup(any());
        verify(mapper, never()).insertSmallGroup(any());
        verify(mapper, never()).insertOwnerMembership(any(), any());
    }

    // AI작성: 실제 개인정보 없는 생성 요청과 지역 기준 데이터를 구성한다.
    private SmallGroupCreationRequest request() {
        SmallGroupCreationRequest request = new SmallGroupCreationRequest();
        request.setMemberId(10L);
        request.setRegionName("서울특별시 강남구");
        request.setGroupName("검증 모임");
        request.setLikeCategory("독서");
        request.setDescription("지역 선택 검증");
        request.setMaxMembers(10);
        request.setMeetDate(LocalDate.of(2099, 1, 1));
        request.setLocation("지역 도서관");
        return request;
    }

    private MemberRegionDto region() {
        MemberRegionDto region = new MemberRegionDto();
        region.setRegionId(1L);
        region.setRegionName("서울특별시 강남구");
        region.setProvinceName("서울특별시");
        region.setDistrictName("강남구");
        return region;
    }
}
