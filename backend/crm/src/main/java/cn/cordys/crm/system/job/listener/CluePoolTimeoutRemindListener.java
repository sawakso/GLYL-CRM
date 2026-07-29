package cn.cordys.crm.system.job.listener;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 线索池超时提醒监听器。
 * <p>
 * 分别处理线索入池后未分配、领取后未跟进两类超时提醒。
 */
@Component
@Slf4j
public class CluePoolTimeoutRemindListener implements ApplicationListener<ExecuteEvent> {

    private static final long MINUTE_MILLIS = 60_000L;

    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private UserExtendService userExtendService;

    @Override
    public void onApplicationEvent(ExecuteEvent event) {
        try {
            remindTimeout();
        } catch (Exception e) {
            log.error("线索池超时提醒任务异常: {}", e.getMessage());
        }
    }

    /**
     * 执行线索池超时提醒。
     */
    public void remindTimeout() {
        LambdaQueryWrapper<CluePool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CluePool::getEnable, true);
        List<CluePool> pools = cluePoolMapper.selectListByLambda(poolWrapper);
        if (CollectionUtils.isEmpty(pools)) {
            return;
        }

        long now = System.currentTimeMillis();
        for (CluePool pool : pools) {
            List<String> poolAdminIds = resolvePoolAdminIds(pool);
            remindUnassigned(pool, poolAdminIds, now);
            remindUnfollowed(pool, poolAdminIds, now);
        }
    }

    /**
     * 线索入池后超过配置时间仍未分配时，提醒池管理员和协同管理员。
     */
    private void remindUnassigned(CluePool pool, List<String> poolAdminIds, long now) {
        Integer reminderMinutes = pool.getUnassignedReminderMinutes();
        if (reminderMinutes == null || reminderMinutes <= 0 || CollectionUtils.isEmpty(poolAdminIds)) {
            return;
        }

        long threshold = now - reminderMinutes.longValue() * MINUTE_MILLIS;
        LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
        clueWrapper.eq(Clue::getPoolId, pool.getId())
                .eq(Clue::getInSharedPool, true);
        List<Clue> clues = clueMapper.selectListByLambda(clueWrapper);
        if (CollectionUtils.isEmpty(clues)) {
            return;
        }

        for (Clue clue : clues) {
            if (clue.getPoolEntryTime() == null
                    || clue.getPoolEntryTime() > threshold
                    || clue.getUnassignedReminderSentAt() != null) {
                continue;
            }
            try {
                commonNoticeSendService.sendNotice(
                        NotificationConstants.Module.CLUE,
                        NotificationConstants.Event.CLUE_POOL_UNASSIGNED_TIMEOUT_REMIND,
                        clue.getName(),
                        InternalUser.ADMIN.getValue(),
                        pool.getOrganizationId(),
                        poolAdminIds,
                        true
                );
                markUnassignedReminderSent(clue.getId(), now);
            } catch (Exception e) {
                log.error("线索池未分配超时提醒失败, poolId={}, clueId={}: {}",
                        pool.getId(), clue.getId(), e.getMessage());
            }
        }
    }

    /**
     * 线索领取后超过配置时间仍未跟进时，提醒负责人，并按配置同时提醒池管理员。
     */
    private void remindUnfollowed(CluePool pool, List<String> poolAdminIds, long now) {
        Integer reminderMinutes = pool.getUnfollowedReminderMinutes();
        if (reminderMinutes == null || reminderMinutes <= 0) {
            return;
        }

        long threshold = now - reminderMinutes.longValue() * MINUTE_MILLIS;
        LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
        clueWrapper.eq(Clue::getSourcePoolId, pool.getId())
                .eq(Clue::getInSharedPool, false);
        List<Clue> clues = clueMapper.selectListByLambda(clueWrapper);
        if (CollectionUtils.isEmpty(clues)) {
            return;
        }

        for (Clue clue : clues) {
            Long collectionTime = clue.getCollectionTime();
            Long followTime = clue.getFollowTime();
            if (collectionTime == null
                    || collectionTime > threshold
                    || clue.getUnfollowedReminderSentAt() != null
                    || StringUtils.isBlank(clue.getOwner())
                    || (followTime != null && followTime >= collectionTime)) {
                continue;
            }

            Set<String> receiverIds = new LinkedHashSet<>();
            receiverIds.add(clue.getOwner());
            if (Boolean.TRUE.equals(pool.getNotifyPoolAdminOnUnfollowedTimeout())) {
                receiverIds.addAll(poolAdminIds);
            }

            try {
                commonNoticeSendService.sendNotice(
                        NotificationConstants.Module.CLUE,
                        NotificationConstants.Event.CLUE_POOL_UNFOLLOWED_TIMEOUT_REMIND,
                        clue.getName(),
                        InternalUser.ADMIN.getValue(),
                        pool.getOrganizationId(),
                        new ArrayList<>(receiverIds),
                        true
                );
                markUnfollowedReminderSent(clue.getId(), now);
            } catch (Exception e) {
                log.error("线索池未跟进超时提醒失败, poolId={}, clueId={}: {}",
                        pool.getId(), clue.getId(), e.getMessage());
            }
        }
    }

    private List<String> resolvePoolAdminIds(CluePool pool) {
        Set<String> scopeIds = new LinkedHashSet<>();
        scopeIds.addAll(parseScopeIds(pool.getOwnerId()));
        scopeIds.addAll(parseScopeIds(pool.getCollaboratorId()));
        if (scopeIds.isEmpty()) {
            return List.of();
        }
        return userExtendService.getScopeOwnerIds(new ArrayList<>(scopeIds), pool.getOrganizationId());
    }

    private List<String> parseScopeIds(String scopeIdsJson) {
        if (StringUtils.isBlank(scopeIdsJson)) {
            return List.of();
        }
        try {
            List<String> scopeIds = JSON.parseArray(scopeIdsJson, String.class);
            return scopeIds == null ? List.of() : scopeIds;
        } catch (Exception e) {
            log.warn("解析线索池管理员范围失败: {}", e.getMessage());
            return List.of();
        }
    }

    private void markUnassignedReminderSent(String clueId, long sentAt) {
        Clue update = new Clue();
        update.setId(clueId);
        update.setUnassignedReminderSentAt(sentAt);
        clueMapper.update(update);
    }

    private void markUnfollowedReminderSent(String clueId, long sentAt) {
        Clue update = new Clue();
        update.setId(clueId);
        update.setUnfollowedReminderSentAt(sentAt);
        clueMapper.update(update);
    }
}
