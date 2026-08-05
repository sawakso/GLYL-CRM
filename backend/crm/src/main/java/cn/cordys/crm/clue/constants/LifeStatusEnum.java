package cn.cordys.crm.clue.constants;

import lombok.Getter;
import org.apache.commons.lang3.Strings;

/**
 * 生命状态枚举 (纷享销客维度)
 * 数据生命周期管理：活跃/沉睡/作废
 */
@Getter
public enum LifeStatusEnum {

    ACTIVE("ACTIVE", "活跃"),
    DORMANT("DORMANT", "沉睡"),
    INVALID("INVALID", "作废");

    private final String key;
    private final String name;

    LifeStatusEnum(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public static LifeStatusEnum ofKey(String key) {
        for (LifeStatusEnum e : values()) {
            if (Strings.CS.equals(key, e.key)) {
                return e;
            }
        }
        return null;
    }

    public static String getNameByKey(String key) {
        LifeStatusEnum e = ofKey(key);
        return e != null ? e.name : null;
    }
}
