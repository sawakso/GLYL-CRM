package cn.cordys.crm.clue.schedule;

import cn.cordys.common.schedule.BaseScheduleJob;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.clue.service.CluePoolAssignRuleService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

/**
 * 线索池定时自动分配任务
 * <p>
 * 由 {@link CluePoolAssignScheduler} 按池独立注册 cron 触发器;触发时扫描该池内未分配线索并执行分配规则。
 * 任务非 Spring 托管(Quartz 反射实例化),内部通过 {@link CommonBeanFactory} 获取业务 Bean。
 */
@Slf4j
public class CluePoolAutoAssignJob extends BaseScheduleJob {

    /**
     * 任务分组(与触发器分组一致)
     */
    public static final String JOB_GROUP = "CLUE_POOL_ASSIGN";

    @Override
    protected void businessExecute(JobExecutionContext context) {
        String poolId = this.resourceId;
        String organizationId = context.getJobDetail().getJobDataMap().getString("organizationId");
        String operatorId = this.userId;
        if (poolId == null || organizationId == null) {
            log.warn("线索池定时分配任务参数缺失, poolId={}, orgId={}", poolId, organizationId);
            return;
        }
        try {
            CluePoolAssignRuleService assignRuleService = CommonBeanFactory.getBean(CluePoolAssignRuleService.class);
            int count = assignRuleService.batchMatchAndAssign(poolId, organizationId, operatorId);
            log.info("线索池定时分配任务完成, poolId={}, 分配数量={}", poolId, count);
        } catch (Exception e) {
            log.error("线索池定时分配任务执行失败, poolId={}", poolId, e);
        }
    }
}
