package cn.cordys.crm.clue.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * 线索
 *
 * @author jianxing
 * @date 2025-03-10 11:57:28
 */
@Data
@Table(name = "clue")
public class Clue extends BaseModel {

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "上次修改前的线索阶段")
    private String lastStage;

    @Schema(description = "联系人名称")
    private String contact;

    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "意向产品id")
    private List<String> products;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "创建时间")
    private Long collectionTime;

    @Schema(description = "是否在线索池")
    private Boolean inSharedPool;

    @Schema(description = "转移成客户或者线索")
    private String transitionType;

    @Schema(description = "客户id或者线索id")
    private String transitionId;

    @Schema(description = "最新跟进人")
    private String follower;

    @Schema(description = "最新跟进时间")
    private Long followTime;

    @Schema(description = "线索池ID")
    private String poolId;

    @Schema(description = "最近来源线索池ID")
    private String sourcePoolId;

    @Schema(description = "最近入池时间")
    private Long poolEntryTime;

    @Schema(description = "未分配超时提醒发送时间")
    private Long unassignedReminderSentAt;

    @Schema(description = "未跟进超时提醒发送时间")
    private Long unfollowedReminderSentAt;

    @Schema(description = "线索池原因ID")
    private String reasonId;

    /*------ start: 纷享销客扩展字段 (ext_ver 1.11.0) ------*/

    // 联系/企业信息
    @Schema(description = "电话")
    private String tel;

    @Schema(description = "手机")
    private String mobile;

    @Schema(description = "邮件")
    private String email;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "网址")
    private String url;

    @Schema(description = "企业名称")
    private String company;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "职务")
    private String jobTitle;

    @Schema(description = "名片")
    private String picturePath;

    // 状态/管理
    @Schema(description = "来源")
    private String source;

    @Schema(description = "线索阶段")
    private String leadsStage;

    @Schema(description = "业务状态")
    private String bizStatus;

    @Schema(description = "生命状态")
    private String lifeStatus;

    @Schema(description = "锁定状态")
    private String lockStatus;

    @Schema(description = "业务类型")
    private String recordType;

    // 跟进/负责人
    @Schema(description = "下次跟进时间")
    private Long nextFollowedTime;

    @Schema(description = "下次跟进要点")
    private String nextFollowedRemark;

    @Schema(description = "最后跟进时间")
    private Long lastFollowTime;

    @Schema(description = "最后跟进人")
    private String lastFollower;

    @Schema(description = "客户咨询详情")
    private String remark;

    @Schema(description = "外部负责人")
    private String outOwner;

    @Schema(description = "分配管理员")
    private String assignerId;

    @Schema(description = "归属部门")
    private String dataOwnDepartment;

    @Schema(description = "负责人主属部门")
    private String ownerDepartment;

    // 时间/转换
    @Schema(description = "转换时间")
    private Long transformTime;

    @Schema(description = "负责人变更时间")
    private Long ownerChangeTime;

    @Schema(description = "退回/收回时间")
    private Long returnedTime;

    @Schema(description = "转MQL时间")
    private Long changedToMqlTime;

    @Schema(description = "预计收回时间")
    private Long expireTime;

    // 转换/布尔
    @Schema(description = "转换概率")
    private BigDecimal conversionProbability;

    @Schema(description = "剩余保有时间(毫秒)")
    private Long remainingTime;

    @Schema(description = "是否超时")
    private Boolean isOvertime;

    @Schema(description = "是否存在重复数据")
    private Boolean isDuplicated;

    @Schema(description = "疑似重复时关联的原线索ID")
    private String duplicateClueId;

    @Schema(description = "表单回流去重指纹 formId:key:value(并发安全去重兜底)")
    private String dedupFingerprint;

    @Schema(description = "工商注册")
    private Boolean bizRegName;

    // 原因/结果
    @Schema(description = "销售人员退回原因")
    private String backReason;

    @Schema(description = "线索无效原因")
    private String closeReason;

    @Schema(description = "处理结果")
    private String completedResult;

    // 延期
    @Schema(description = "申请延期天数")
    private Integer extendDays;

    @Schema(description = "延期原因")
    private String extendReason;

    // 关联
    @Schema(description = "市场活动名称")
    private String marketingEventId;

    @Schema(description = "归集到的线索")
    private String collectedTo;

    // 其他
    @Schema(description = "手机归属地")
    private String phoneNumberAttribution;

    @Schema(description = "企业微信UserId")
    private String enterpriseWechatUserId;

    // 审批
    @Schema(description = "审批状态")
    private String approvalStatus;

    @Schema(description = "是否已审批通过")
    private Boolean approved;

    /*------ end: 纷享销客扩展字段 (ext_ver 1.11.0) ------*/
}
