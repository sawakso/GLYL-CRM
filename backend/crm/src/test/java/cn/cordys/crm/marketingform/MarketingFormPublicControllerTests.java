package cn.cordys.crm.marketingform;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.marketingform.domain.MarketingForm;
import cn.cordys.crm.marketingform.domain.MarketingFormSubmission;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 市场表单公开提交 → 回流成线索进池 的端到端测试。
 * 重点覆盖: 提交含「映射到 phone 的引用线索字段」时, 线索必须能正常创建进池
 * (修复前 saveModuleField 唯一自检撞到刚插入的自身导致回滚, 无法进池)。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MarketingFormPublicControllerTests extends BaseTest {

    private static final String PUBLIC_BASE = "/pub/marketing-form";
    private static String testPoolId;
    private static String testToken;
    private static String testFormId;

    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private BaseMapper<MarketingForm> marketingFormMapper;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<MarketingFormSubmission> submissionMapper;

    @Test
    @Order(1)
    void prepareTestData() {
        // 1. 建一个启用中的目标线索池 (补齐 NOT NULL 配置列, 避免 MyBatis 显式插 NULL)
        CluePool pool = new CluePool();
        pool.setId(IDGenerator.nextStr());
        pool.setName("mkt-test-pool");
        pool.setScopeId("[\"admin\"]");
        pool.setOwnerId("[\"admin\"]");
        pool.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        pool.setEnable(true);
        pool.setAuto(false);
        pool.setPickMode("VISIBLE_PICKABLE");
        pool.setNewLeadRemind(false);
        pool.setUnassignedReminderMinutes(1440);
        pool.setUnfollowedReminderMinutes(2880);
        pool.setNotifyPoolAdminOnUnfollowedTimeout(false);
        pool.setAllowTransferAfterPick(false);
        pool.setRestrictTransferInToMembers(false);
        pool.setRestrictReturnToMembers(false);
        pool.setClearTeamOnOwnerChange(false);
        pool.setClearExternalOwnerOnOwnerEmpty(false);
        pool.setClearExternalTeamOnExternalOwnerEmpty(false);
        pool.setClearOwnerOnPoolTransfer(false);
        pool.setClearExternalOwnerOnPoolTransfer(false);
        pool.setAllowViewChangeLogBeforePick(false);
        pool.setAllowEditTeamBeforePick(false);
        pool.setAllowSendSalesRecordBeforePick(false);
        pool.setAllowViewSalesRecordBeforePick(false);
        pool.setAllowViewPoolLog(false);
        pool.setAutoAssignEnabled(false);
        pool.setDedupStrategy("NONE");
        pool.setDedupWindow(5);
        pool.setDedupKey("AUTO");
        pool.setCreateTime(System.currentTimeMillis());
        pool.setCreateUser("admin");
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser("admin");
        cluePoolMapper.insert(pool);
        testPoolId = pool.getId();

        // 2. 建一个 ACTIVE 市场表单, 字段映射: 表单字段 field_phone -> 线索 phone
        testToken = "mkt-token-" + IDGenerator.nextStr();
        testFormId = IDGenerator.nextStr();
        MarketingForm form = new MarketingForm();
        form.setId(testFormId);
        form.setName("test-marketing-form");
        form.setTargetPoolId(testPoolId);
        form.setFieldMapping("{\"field_phone\":\"phone\"}");
        form.setDedupStrategy("NONE");
        form.setDedupWindow(0);
        form.setDedupKey("PHONE");
        form.setQrToken(testToken);
        form.setStatus("ACTIVE");
        form.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        form.setCreateTime(System.currentTimeMillis());
        form.setUpdateTime(System.currentTimeMillis());
        form.setCreateUser("admin");
        form.setUpdateUser("admin");
        marketingFormMapper.insert(form);
    }

    @Test
    @Order(2)
    void submitWithPhoneRefFieldShouldCreateClue() throws Exception {
        // 提交含「映射到 phone 的引用线索字段」的 moduleFields
        Map<String, Object> body = Map.of(
                "moduleFields", List.of(Map.of("fieldId", "field_phone", "fieldValue", "13800000001")),
                "deviceId", "device-abc-123"
        );

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(PUBLIC_BASE + "/" + testToken + "/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON.toJSONString(body)))
                .andExpect(status().isOk())
                .andReturn();

        String clueId = result.getResponse().getContentAsString();
        assert clueId != null && !clueId.isBlank() : "提交后应返回线索 ID, 实际为空";

        // 断言: 线索确实创建并进池
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        assert clue != null : "线索未创建";
        assert testPoolId.equals(clue.getPoolId()) : "线索应进入目标线索池";
        assert Boolean.TRUE.equals(clue.getInSharedPool()) : "线索应在共享池";
        assert "13800000001".equals(clue.getPhone()) : "手机号应映射到线索 phone";
    }

    @Test
    @Order(3)
    void secondSubmitWithDifferentPhoneShouldCreateNewClue() throws Exception {
        // 不同手机号提交, 应创建另一条线索并进池 (验证非唯一字段正常新增)
        Map<String, Object> body = Map.of(
                "moduleFields", List.of(Map.of("fieldId", "field_phone", "fieldValue", "13900000002")),
                "deviceId", "device-abc-123"
        );

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(PUBLIC_BASE + "/" + testToken + "/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSON.toJSONString(body)))
                .andExpect(status().isOk())
                .andReturn();

        String clueId = result.getResponse().getContentAsString();
        assert clueId != null && !clueId.isBlank() : "提交应返回线索 ID";
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        assert clue != null : "第二次提交的线索应创建";
        assert testPoolId.equals(clue.getPoolId()) : "第二次提交的线索应进池";
        assert "13900000002".equals(clue.getPhone()) : "手机号应正确映射";
    }

    @Test
    @Order(4)
    void cleanup() {
        clueMapper.deleteByLambda(new LambdaQueryWrapper<Clue>().eq(Clue::getPoolId, testPoolId));
        submissionMapper.deleteByLambda(new LambdaQueryWrapper<MarketingFormSubmission>()
                .eq(MarketingFormSubmission::getMarketingFormId, testFormId));
        cluePoolMapper.deleteByLambda(new LambdaQueryWrapper<CluePool>().eq(CluePool::getId, testPoolId));
        marketingFormMapper.deleteByLambda(new LambdaQueryWrapper<MarketingForm>()
                .eq(MarketingForm::getId, testFormId));
    }
}
