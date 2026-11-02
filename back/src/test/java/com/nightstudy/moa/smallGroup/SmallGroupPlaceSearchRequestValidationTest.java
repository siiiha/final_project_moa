package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.nightstudy.moa.smallGroup.dto.SmallGroupPlaceSearchRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 외부 호출 없이 검색 조건의 경계값과 좌표·반경·정렬의 조합을 검증한다.
class SmallGroupPlaceSearchRequestValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private SmallGroupPlaceSearchRequest request() {
        SmallGroupPlaceSearchRequest request = new SmallGroupPlaceSearchRequest();
        request.setSmallGroupId(10L);
        request.setQuery("가상 모임 장소");
        return request;
    }

    @Test
    void acceptsDefaultsAndTrimsQueryBeforeLengthValidation() {
        SmallGroupPlaceSearchRequest request = request();
        request.setQuery("  " + "가".repeat(100) + "  ");
        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getQuery()).hasSize(100);
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getSize()).isEqualTo(15);
        assertThat(request.getSort()).isEqualTo("accuracy");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsMissingOrBlankQuery(String query) {
        SmallGroupPlaceSearchRequest request = request();
        request.setQuery(query);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsQueryLongerThanOneHundredCharacters() {
        SmallGroupPlaceSearchRequest request = request();
        request.setQuery("가".repeat(101));
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveGroupId(long id) {
        SmallGroupPlaceSearchRequest request = request();
        request.setSmallGroupId(id);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({"1,1,true", "45,15,true", "0,15,false", "46,15,false", "1,0,false", "1,16,false"})
    void validatesPageBoundaries(int page, int size, boolean valid) {
        SmallGroupPlaceSearchRequest request = request();
        request.setPage(page);
        request.setSize(size);
        assertThat(validator.validate(request).isEmpty()).isEqualTo(valid);
    }

    @Test
    void rejectsExplicitNullRequiredValues() {
        SmallGroupPlaceSearchRequest request = request();
        request.setSmallGroupId(null);
        request.setPage(null);
        request.setSize(null);
        request.setSort(null);
        assertThat(validator.validate(request)).hasSize(4);
    }

    @ParameterizedTest
    @CsvSource({"-90,-180,true", "90,180,true", "0,0,true", "91,0,false", "0,181,false", "-91,0,false", "0,-181,false"})
    void validatesCoordinateBoundaries(double latitude, double longitude, boolean valid) {
        SmallGroupPlaceSearchRequest request = request();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        assertThat(validator.validate(request).isEmpty()).isEqualTo(valid);
    }

    @Test
    void rejectsSingleCoordinateAndNonFiniteCoordinates() {
        SmallGroupPlaceSearchRequest request = request();
        request.setLatitude(0.0);
        assertThat(validator.validate(request)).isNotEmpty();
        request.setLatitude(null);
        request.setLongitude(0.0);
        assertThat(validator.validate(request)).isNotEmpty();
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            request.setLatitude(invalid);
            request.setLongitude(0.0);
            assertThat(validator.validate(request)).isNotEmpty();
            request.setLatitude(0.0);
            request.setLongitude(invalid);
            assertThat(validator.validate(request)).isNotEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource({"0,true", "20000,true", "-1,false", "20001,false"})
    void validatesRadiusBoundaries(int radius, boolean valid) {
        SmallGroupPlaceSearchRequest request = request();
        request.setRadius(radius);
        assertThat(validator.validate(request)).isNotEmpty();
        request.setLatitude(0.0);
        request.setLongitude(0.0);
        assertThat(validator.validate(request).isEmpty()).isEqualTo(valid);
    }

    @Test
    void requiresCoordinatesForDistanceSortAndRejectsUnknownSort() {
        SmallGroupPlaceSearchRequest request = request();
        request.setSort("distance");
        assertThat(validator.validate(request)).isNotEmpty();
        request.setLatitude(0.0);
        request.setLongitude(0.0);
        assertThat(validator.validate(request)).isEmpty();
        request.setSort("unknown");
        assertThat(validator.validate(request)).isNotEmpty();
    }
}
