package cn.cordys.crm.clue.schedule;

import cn.cordys.common.schedule.ScheduleManager;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 线索池定时自动分配调度管理器
 * <p>
 * 负责按池独立注册/更新/移除 cron 定时任务,并在应用启动时加载所有已启用定时分配的池。
 * 复用 {@link ScheduleManager}(Quartz) 的能力。
 */
@Slf4j
@Component
public class CluePoolAssignScheduler implements ApplicationRunner {

    @Resource
    private ScheduleManager scheduleManager;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;

    /**
     * 注册或更新某池的定时自动分配任务。
     * 未启用或 cron 为空时不做任何事(调用方负责在关闭时移除)。
     */
    public void registerOrUpdate(CluePool pool) {
        if (pool == null || !Boolean.TRUE.equals(pool.getAutoAssignEnabled())
                || StringUtils.isBlank(pool.getAutoAssignCron())) {
            return;
        }
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("resourceId", pool.getId());
            jobDataMap.put("userId", resolveOperator(pool));
            jobDataMap.put("expression", pool.getAutoAssignCron());
            jobDataMap.put("organizationId", pool.getOrganizationId());
            scheduleManager.addOrUpdateCronJob(
                    new JobKey(pool.getId(), CluePoolAutoAssignJob.JOB_GROUP),
                    new TriggerKey(pool.getId(), CluePoolAutoAssignJob.JOB_GROUP),
                    CluePoolAutoAssignJob.class,
                    pool.getAutoAssignCron(),
                    jobDataMap);
            log.info("注册线索池定时分配任务, poolId={}, cron={}", pool.getId(), pool.getAutoAssignCron());
        } catch (Exception e) {
            log.error("注册线索池定时分配任务失败, poolId={}", pool.getId(), e);
        }
    }

    /**
     * 移除某池的定时自动分配任务
     */
    public void remove(String poolId) {
        if (StringUtils.isBlank(poolId)) {
            return;
        }
        try {
            scheduleManager.removeJob(
                    new JobKey(poolId, CluePoolAutoAssignJob.JOB_GROUP),
                    new TriggerKey(poolId, CluePoolAutoAssignJob.JOB_GROUP));
        } catch (Exception e) {
            log.warn("移除线索池定时分配任务失败, poolId={}", poolId, e);
        }
    }

    /**
     * 应用启动时加载所有已启用定时分配的池,重新注册任务
     */
    public void initEnabledPools() {
        try {
            LambdaQueryWrapper<CluePool> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CluePool::getAutoAssignEnabled, true);
            List<CluePool> pools = cluePoolMapper.selectListByLambda(wrapper);
            if (CollectionUtils.isEmpty(pools)) {
                return;
            }
            for (CluePool pool : pools) {
                registerOrUpdate(pool);
            }
            log.info("初始化线索池定时分配任务完成, 数量={}", pools.size());
        } catch (Exception e) {
            log.error("初始化线索池定时分配任务失败", e);
        }
    }

    private String resolveOperator(CluePool pool) {
        try {
            List<String> owners = JSON.parseArray(pool.getOwnerId(), String.class);
            if (!CollectionUtils.isEmpty(owners)) {
                return owners.get(0);
            }
        } catch (Exception ignored) {
            // 解析失败降级为 SYSTEM
        }
        return "SYSTEM";
    }

    @Override
    public void run(ApplicationArguments args) {
        initEnabledPools();
    }
}
