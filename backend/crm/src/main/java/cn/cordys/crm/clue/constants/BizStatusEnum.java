package cn.cordys.crm.clue.constants;

import lombok.Getter;
import org.apache.commons.lang3.Strings;

/**
 * 业务状态枚举 (纷享销客维度)
 * 销售团队内部使用的精细化状态
 */
@Getter
public enum BizStatusEnum {

    NEW("NEW", "新建"),
    TRYING("TRYING", "尝试联系"),
    INTERESTED("INTERESTED", "有意向"),
    NOT_INTERESTED("NOT_INTERESTED", "无意向"),
    CONVERTED("CONVERTED", "已转化");

    private final String key;
    private final String name;

    BizStatusEnum(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public static BizStatusEnum ofKey(String key) {
        for (BizStatusEnum e : values()) {
            if (Strings.CS.equals(key, e.key)) {
                return e;
            }
        }
        return null;
    }

    public static String getNameByKey(String key) {
        BizStatusEnum e = ofKey(key);
        return e != null ? e.name : null;
    }
}
