package com.nightstudy.moa.savingGroup.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SavingGroupJoinMapperContractTest {

    @Test
    void 가입신청에필요한Mapper구문이존재한다() throws IOException {
        String mapperXml = readMapperXml();

        assertTrue(mapperXml.contains("id=\"selectSavingGroupJoinMemberForUpdate\""));
        assertTrue(mapperXml.contains("id=\"selectSavingGroupJoinTargetForUpdate\""));
        assertTrue(mapperXml.contains("id=\"selectSavingGroupMembershipForUpdate\""));
        assertTrue(mapperXml.contains("id=\"insertSavingGroupJoinRequest\""));
        assertTrue(mapperXml.contains("id=\"reactivateLeftSavingGroupMembership\""));
    }

    @Test
    void 가입수집계와재신청SQL은확정정책을유지한다() throws IOException {
        String mapperXml = readMapperXml();

        assertTrue(mapperXml.contains("sg.status IN ('00', '01', '02', '03')"));
        assertTrue(mapperXml.contains("AND membership_status = 'LEFT'"));
        assertTrue(mapperXml.contains("membership_status = 'PENDING'"));
    }

    private String readMapperXml() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("mappers/SavingGroupMapper.xml")) {
            if (stream == null) {
                throw new IOException("SavingGroupMapper.xml을 찾을 수 없습니다.");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
