package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.savingGroup.service.SavingGroupJoinConcurrencyIntegrationTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

// AI작성: 준비·정리와 7개 경쟁 조건을 재사용하고 실제 소모임 Service·Mapper로 실행한다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_DB_ENABLED", matches = "true")
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("moa-join-test-database")
class SmallGroupJoinConcurrencyIntegrationTest extends SavingGroupJoinConcurrencyIntegrationTest {
    @Override
    protected boolean isSavingGroup() {
        return false;
    }
}
