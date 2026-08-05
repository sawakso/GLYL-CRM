package cn.cordys.crm.clue.constants;

import org.apache.commons.lang3.Strings;

/**
 * @Author: jianxing
 * @CreateTime: 2025-03-11  15:53
 */
public enum ClueStatus {

    /**
     * 新建
     */
    NEW("NEW", "新建"),
    /**
     * 跟进中
     */
    FOLLOWING("FOLLOWING", "跟进中"),
    /**
     * 感兴趣
     */
    INTERESTED("INTERESTED", "感兴趣"),
    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),
    /**
     * 失败
     */
    FAIL("FAIL", "失败"),
    /**
     * MQL（市场认可线索）
     */
    MQL("MQL", "MQL"),
    /**
     * 已转化
     */
    CONVERTED("CONVERTED", "已转化"),
    /**
     * 无效关闭
     */
    CLOSED("CLOSED", "无效关闭");

    private final String key;
    private final String name;

    ClueStatus(String key, String name) {
        this.key = key;
        this.name = name;
    }

    /**
     * 获取当前状态允许流转到的下一个状态集合
     * 基于纷享销客线索生命周期：新建 → 跟进中 → 感兴趣/MQL → 已转化 / 无效关闭
     */
    public java.util.Set<ClueStatus> allowedTransitions() {
        return switch (this) {
            case NEW -> java.util.Set.of(FOLLOWING, CLOSED);
            case FOLLOWING -> java.util.Set.of(INTERESTED, MQL, CLOSED);
            case INTERESTED -> java.util.Set.of(MQL, CONVERTED, CLOSED);
            case MQL -> java.util.Set.of(CONVERTED, CLOSED);
            case SUCCESS, CONVERTED -> java.util.Set.of();
            case FAIL, CLOSED -> java.util.Set.of(NEW);
        };
    }

    /**
     * 判断是否可以流转到目标状态
     */
    public boolean canTransitionTo(ClueStatus target) {
        return allowedTransitions().contains(target);
    }

    /**
     * 是否为终态（不允许再流转到其他状态，但可以从终态回到新建）
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == CONVERTED || this == FAIL || this == CLOSED;
    }

    public static ClueStatus ofKey(String key) {
        for (ClueStatus status : ClueStatus.values()) {
            if (Strings.CS.equals(key, status.key)) {
                return status;
            }
        }
        return null;
    }

    public static String getByKey(String key) {
        ClueStatus status = ofKey(key);
        return status != null ? status.name : null;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}
