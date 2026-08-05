package cn.cordys.crm.clue.constants;

import lombok.Getter;
import org.apache.commons.lang3.Strings;

/**
 * 锁定状态枚举
 * 控制线索是否允许编辑
 */
@Getter
public enum LockStatusEnum {

    UNLOCKED("UNLOCKED", "未锁定"),
    LOCKED("LOCKED", "已锁定");

    private final String key;
    private final String name;

    LockStatusEnum(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public static LockStatusEnum ofKey(String key) {
        for (LockStatusEnum e : values()) {
            if (Strings.CS.equals(key, e.key)) {
                return e;
            }
        }
        return null;
    }
}
