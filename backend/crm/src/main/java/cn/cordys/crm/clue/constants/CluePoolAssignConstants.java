package cn.cordys.crm.clue.constants;

/**
 * 线索池分配规则常量
 */
public final class CluePoolAssignConstants {

    private CluePoolAssignConstants() {
    }

    /**
     * 领取模式 - 成员可见可领取,管理员可分配
     */
    public static final String PICK_MODE_VISIBLE_PICKABLE = "VISIBLE_PICKABLE";

    /**
     * 领取模式 - 成员不可见,管理员可分配
     */
    public static final String PICK_MODE_ADMIN_ASSIGN_ONLY = "ADMIN_ASSIGN_ONLY";

    /**
     * 分配方式 - 仅分配给某人
     */
    public static final String ASSIGN_TYPE_SINGLE = "SINGLE";

    /**
     * 分配方式 - 循环分配
     */
    public static final String ASSIGN_TYPE_ROUND_ROBIN = "ROUND_ROBIN";

    /**
     * 条件操作符 - 等于
     */
    public static final String OPERATOR_EQUALS = "EQUALS";

    /**
     * 条件操作符 - 不等于
     */
    public static final String OPERATOR_NOT_EQUALS = "NOT_EQUALS";

    /**
     * 条件操作符 - 包含
     */
    public static final String OPERATOR_CONTAINS = "CONTAINS";
}
