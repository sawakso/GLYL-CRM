package cn.cordys.crm.clue.dto.request;

import cn.cordys.crm.clue.dto.CluePoolAssignRuleDTO;
import cn.cordys.crm.clue.dto.CluePoolPickRuleDTO;
import cn.cordys.crm.clue.dto.CluePoolRecycleRuleDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CluePoolAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "线索池名称")
    private String name;
    @Size(max = 1000)
    @Schema(description = "线索池描述")
    private String description;


    @NotNull
    @Schema(description = "范围ID集合")
    private List<String> scopeIds;

    @NotNull
    @Schema(description = "管理员ID集合")
    private List<String> ownerIds;

    @Schema(description = "协同管理员ID集合")
    private List<String> collaboratorIds;

    @NotNull
    @Schema(description = "启用/禁用")
    private Boolean enable;

    @NotNull
    @Schema(description = "自动回收")
    private Boolean auto;

    @Schema(description = "领取模式: VISIBLE_PICKABLE(成员可见可领取)/ADMIN_ASSIGN_ONLY(成员不可见,管理员可分配)")
    private String pickMode;

    @Schema(description = "新线索提醒:为管理员推送待办")
    private Boolean newLeadRemind;

    @Schema(description = "未分配超时提醒分钟数")
    private Integer unassignedReminderMinutes;

    @Schema(description = "未跟进超时提醒分钟数")
    private Integer unfollowedReminderMinutes;

    @Schema(description = "未跟进超时时通知线索池管理员")
    private Boolean notifyPoolAdminOnUnfollowedTimeout;

    @Schema(description = "领取后允许转移")
    private Boolean allowTransferAfterPick;

    @Schema(description = "仅允许转入线索池成员")
    private Boolean restrictTransferInToMembers;

    @Schema(description = "仅允许退回线索池成员")
    private Boolean restrictReturnToMembers;

    @Schema(description = "负责人变化时清空团队")
    private Boolean clearTeamOnOwnerChange;

    @Schema(description = "负责人为空时清空外部负责人")
    private Boolean clearExternalOwnerOnOwnerEmpty;

    @Schema(description = "外部负责人为空时清空外部团队")
    private Boolean clearExternalTeamOnExternalOwnerEmpty;

    @Schema(description = "转移线索池时清空负责人")
    private Boolean clearOwnerOnPoolTransfer;

    @Schema(description = "转移线索池时清空外部负责人")
    private Boolean clearExternalOwnerOnPoolTransfer;

    @Schema(description = "领取前可查看变更记录")
    private Boolean allowViewChangeLogBeforePick;

    @Schema(description = "领取前可编辑团队")
    private Boolean allowEditTeamBeforePick;

    @Schema(description = "领取前可发送销售记录")
    private Boolean allowSendSalesRecordBeforePick;

    @Schema(description = "领取前可查看销售记录")
    private Boolean allowViewSalesRecordBeforePick;

    @Schema(description = "可查看线索池日志")
    private Boolean allowViewPoolLog;

    @Schema(description = "启用定时自动分配")
    private Boolean autoAssignEnabled;

    @Schema(description = "定时自动分配cron表达式")
    private String autoAssignCron;

    @Schema(description = "表单回流去重策略(池默认): NONE/UPDATE/SKIP/MARK")
    private String dedupStrategy;

    @Schema(description = "去重时间窗(分钟), 0 表示不限时间窗")
    private Integer dedupWindow;

    @Schema(description = "身份判定键: AUTO/PHONE/DEVICE/IP")
    private String dedupKey;

    @Schema(description = "领取规则")
    private CluePoolPickRuleDTO pickRule;

    @Schema(description = "回收规则")
    private CluePoolRecycleRuleDTO recycleRule;

    @Schema(description = "分配规则集合")
    private List<CluePoolAssignRuleDTO> assignRules;

    @Schema(description = "隐藏字段ID集合")
    private Set<@NotBlank String> hiddenFieldIds;
}
