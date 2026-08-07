package cn.cordys.crm.clue.constants;

import lombok.Getter;
import org.apache.commons.lang3.Strings;

/**
 * 线索阶段枚举 (纷享销客维度)
 * 线索在前端展示的主要进度标识
 */
@Getter
public enum LeadsStageEnum {

    NEW("NEW", "新线索"),
    CONTACTED("CONTACTED", "已联系"),
    CONVERTED("CONVERTED", "已转化"),
    INVALID("INVALID", "无效线索");

    private final String key;
    private final String name;

    LeadsStageEnum(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public static LeadsStageEnum ofKey(String key) {
        for (LeadsStageEnum e : values()) {
            if (Strings.CS.equals(key, e.key)) {
                return e;
            }
        }
        return null;
    }

    public static String getNameByKey(String key) {
        LeadsStageEnum e = ofKey(key);
        return e != null ? e.name : null;
    }

    /** 按名称(name)反查 key, 用于把市场表单等外部传入的中文标签归一化为枚举 key。 */
    public static String getKeyByName(String name) {
        for (LeadsStageEnum e : values()) {
            if (Strings.CS.equals(name, e.name)) {
                return e.key;
            }
        }
        return null;
    }
}
