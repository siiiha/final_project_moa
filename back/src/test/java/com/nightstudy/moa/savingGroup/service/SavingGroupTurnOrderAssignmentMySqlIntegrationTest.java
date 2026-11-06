package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

// AI작성: 명시적으로 허용한 빈 전용 MySQL에서만 최소 테스트 구조를 만들고 동시 저장과 실제 롤백을 검증한다.
@EnabledIfEnvironmentVariable(named="MOA_RUN_MYSQL_JOIN_TESTS", matches="true")
@ResourceLock("moa-join-test-database")
class SavingGroupTurnOrderAssignmentMySqlIntegrationTest {
    SavingGroupTurnOrderService service(SavingGroupMapper mapper, DriverManagerDataSource source) {
        var clock=Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"),ZoneId.of("Asia/Seoul"));
        var factory=new ProxyFactory(new SavingGroupTurnOrderServiceImpl(mapper,clock));
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),new AnnotationTransactionAttributeSource()));
        return (SavingGroupTurnOrderService)factory.getProxy();
    }
    @Test void concurrentWholeAssignmentsAndFailedReplacementPreserveDatabase() throws Exception {
        String url=Objects.requireNonNull(System.getenv("MOA_TEST_DB_URL"));
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(Locale.ROOT)).doesNotContain("user=","password=","createdatabaseifnotexist=true");
        var source=new DriverManagerDataSource(url,Objects.requireNonNull(System.getenv("MOA_TEST_DB_USER")),Objects.requireNonNull(System.getenv("MOA_TEST_DB_PASSWORD")));
        try(var connection=source.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try(var tables=connection.getMetaData().getTables(connection.getCatalog(),null,"%",new String[]{"TABLE","VIEW"})) {
                assertThat(tables.next()).as("전용 테스트 DB는 비어 있어야 합니다.").isFalse();
            }
        }
        var jdbc=new JdbcTemplate(source); var created=new ArrayList<String>();
        var ddl=new LinkedHashMap<String,String>();
        ddl.put("groups","group_id BIGINT PRIMARY KEY, deleted_at DATETIME NULL");
        ddl.put("members","member_id BIGINT PRIMARY KEY, nickname VARCHAR(50), deleted_at DATETIME NULL");
        ddl.put("saving_group","saving_group_id BIGINT PRIMARY KEY, group_id BIGINT, saving_group_type VARCHAR(20), status VARCHAR(2), start_date DATE, end_date DATE");
        ddl.put("group_member","crew_member_id BIGINT PRIMARY KEY, group_id BIGINT, member_id BIGINT, member_role VARCHAR(20), membership_status VARCHAR(20), sequence_number INT NULL");
        ddl.put("meeting_round","round_id BIGINT PRIMARY KEY, group_id BIGINT, round_number INT");
        ddl.put("payout_request","payout_request_id BIGINT PRIMARY KEY, round_id BIGINT, requester_id BIGINT, status VARCHAR(20)");
        ddl.put("saving_group_turn_order_plan","turn_order_plan_id BIGINT AUTO_INCREMENT PRIMARY KEY, saving_group_id BIGINT NOT NULL, effective_month DATE NOT NULL, start_round_number INT NOT NULL, assigned_by BIGINT NOT NULL, assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE(saving_group_id,effective_month), FOREIGN KEY(saving_group_id) REFERENCES saving_group(saving_group_id), FOREIGN KEY(assigned_by) REFERENCES members(member_id), CHECK(DAYOFMONTH(effective_month)=1), CHECK(start_round_number>=1)");
        ddl.put("saving_group_turn_order_plan_member","turn_order_plan_id BIGINT NOT NULL, member_id BIGINT NOT NULL, round_number INT NOT NULL, PRIMARY KEY(turn_order_plan_id,member_id), UNIQUE(turn_order_plan_id,round_number), FOREIGN KEY(turn_order_plan_id) REFERENCES saving_group_turn_order_plan(turn_order_plan_id), FOREIGN KEY(member_id) REFERENCES members(member_id), CHECK(round_number>=1)");
        var executor=Executors.newFixedThreadPool(2);
        try {
            for(var entry:ddl.entrySet()) { jdbc.execute("CREATE TABLE `"+entry.getKey()+"` ("+entry.getValue()+") ENGINE=InnoDB"); created.add(entry.getKey()); }
            jdbc.update("INSERT INTO `groups` VALUES (20,NULL)");
            jdbc.update("INSERT INTO members VALUES (30,'테스트계주',NULL),(40,'테스트계원',NULL)");
            jdbc.update("INSERT INTO saving_group VALUES (10,20,'순번계','01','2026-10-01',NULL)");
            jdbc.update("INSERT INTO group_member VALUES (1,20,30,'OWNER','APPROVED',NULL),(2,20,40,'MEMBER','APPROVED',NULL)");
            var config=new Configuration(new Environment("turn-order-test",new SpringManagedTransactionFactory(),source));
            config.setMapUnderscoreToCamelCase(true); config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
            try(var stream=Resources.getResourceAsStream("mappers/SavingGroupMapper.xml")) {
                new XMLMapperBuilder(stream,config,"mappers/SavingGroupMapper.xml",config.getSqlFragments()).parse();
            }
            var mapper=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config)).getMapper(SavingGroupMapper.class);
            var service=service(mapper,source); var gate=new CountDownLatch(1);
            var first=executor.submit(() -> { gate.await(); return service.update(10L,30L,SavingGroupTurnOrderAssignmentServiceTest.request(30,1,40,2)); });
            var second=executor.submit(() -> { gate.await(); return service.update(10L,30L,SavingGroupTurnOrderAssignmentServiceTest.request(30,2,40,1)); });
            gate.countDown(); first.get(15,TimeUnit.SECONDS); second.get(15,TimeUnit.SECONDS);
            assertThat(jdbc.queryForList("SELECT sequence_number FROM group_member ORDER BY member_id",Integer.class)).containsExactlyInAnyOrder(1,2);
            // AI작성: 두 번째 회원 저장에서 강제 실패시켜 이미 저장한 첫 번째 회원도 DB에서 롤백되는지 확인한다.
            var failing=(SavingGroupMapper)java.lang.reflect.Proxy.newProxyInstance(SavingGroupMapper.class.getClassLoader(),new Class[]{SavingGroupMapper.class},(proxy,method,args)-> {
                if ((method.getName().equals("insertTurnOrderPlanMember") || method.getName().equals("updateTurnOrderSequence")) && args[1].equals(40L)) throw new IllegalStateException("테스트 저장 실패");
                try { return method.invoke(mapper,args); } catch(java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
            });
            var before=jdbc.queryForList("SELECT sequence_number FROM group_member ORDER BY member_id",Integer.class);
            int changedFirst=before.get(0)==1?2:1;
            assertThatThrownBy(() -> service(failing,source).update(10L,30L,SavingGroupTurnOrderAssignmentServiceTest.request(30,changedFirst,40,3-changedFirst))).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForList("SELECT sequence_number FROM group_member ORDER BY member_id",Integer.class)).isEqualTo(before);
            jdbc.update("UPDATE saving_group SET status='02'");
            service.update(10L,30L,SavingGroupTurnOrderAssignmentServiceTest.request(30,3,40,4));
            assertThatThrownBy(() -> service(failing,source).update(10L,30L,SavingGroupTurnOrderAssignmentServiceTest.request(30,4,40,3))).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForList("SELECT round_number FROM saving_group_turn_order_plan_member ORDER BY member_id",Integer.class)).containsExactly(3,4);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM saving_group_turn_order_plan",Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow(); executor.awaitTermination(20,TimeUnit.SECONDS);
            Collections.reverse(created); for(String table:created) jdbc.execute("DROP TABLE `"+table+"`");
        }
    }
}
