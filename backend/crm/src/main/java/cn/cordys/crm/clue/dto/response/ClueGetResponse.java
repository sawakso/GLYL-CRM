package cn.cordys.crm.clue.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ClueGetResponse {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "上次修改前的线索阶段")
    private String lastStage;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "联系人名称")
    private String contact;

    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "意向产品")
    private List<String> products;

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "领取时间")
    private Long collectionTime;

    @Schema(description = "剩余归属天数")
    private Integer reservedDays;

    @Schema(description = "最新跟进人")
    private String follower;

    @Schema(description = "最新跟进人名称")
    private String followerName;

    @Schema(description = "最新跟进日期")
    private Long followTime;

    @Schema(description = "线索池ID")
    private String poolId;

    @Schema(description = "默认回收公海名称")
    private String recyclePoolName;

    @Schema(description = "转移类型")
    private String transitionType;

    @Schema(description = "失败原因ID")
    private String reasonId;

    @Schema(description = "失败原因ID")
    private String reasonName;

    /*------ start: 纷享销客扩展字段 (ext_ver 1.11.0) ------*/
    @Schema(description = "电话") private String tel;
    @Schema(description = "手机") private String mobile;
    @Schema(description = "邮件") private String email;
    @Schema(description = "地址") private String address;
    @Schema(description = "网址") private String url;
    @Schema(description = "企业名称") private String company;
    @Schema(description = "部门") private String department;
    @Schema(description = "职务") private String jobTitle;
    @Schema(description = "名片") private String picturePath;
    @Schema(description = "来源") private String source;
    @Schema(description = "线索阶段") private String leadsStage;
    @Schema(description = "业务状态") private String bizStatus;
    @Schema(description = "生命状态") private String lifeStatus;
    @Schema(description = "锁定状态") private String lockStatus;
    @Schema(description = "业务类型") private String recordType;
    @Schema(description = "下次跟进时间") private Long nextFollowedTime;
    @Schema(description = "下次跟进要点") private String nextFollowedRemark;
    @Schema(description = "最后跟进时间") private Long lastFollowTime;
    @Schema(description = "最后跟进人") private String lastFollower;
    @Schema(description = "客户咨询详情") private String remark;
    @Schema(description = "外部负责人") private String outOwner;
    @Schema(description = "分配管理员") private String assignerId;
    @Schema(description = "归属部门") private String dataOwnDepartment;
    @Schema(description = "负责人主属部门") private String ownerDepartment;
    @Schema(description = "转换时间") private Long transformTime;
    @Schema(description = "负责人变更时间") private Long ownerChangeTime;
    @Schema(description = "退回/收回时间") private Long returnedTime;
    @Schema(description = "转MQL时间") private Long changedToMqlTime;
    @Schema(description = "预计收回时间") private Long expireTime;
    @Schema(description = "转换概率") private BigDecimal conversionProbability;
    @Schema(description = "剩余保有时间(毫秒)") private Long remainingTime;
    @Schema(description = "是否超时") private Boolean isOvertime;
    @Schema(description = "是否存在重复数据") private Boolean isDuplicated;
    @Schema(description = "工商注册") private Boolean bizRegName;
    @Schema(description = "销售人员退回原因") private String backReason;
    @Schema(description = "线索无效原因") private String closeReason;
    @Schema(description = "处理结果") private String completedResult;
    @Schema(description = "申请延期天数") private Integer extendDays;
    @Schema(description = "延期原因") private String extendReason;
    @Schema(description = "市场活动名称") private String marketingEventId;
    @Schema(description = "归集到的线索") private String collectedTo;
    @Schema(description = "手机归属地") private String phoneNumberAttribution;
    @Schema(description = "企业微信UserId") private String enterpriseWechatUserId;
    /*------ end: 纷享销客扩展字段 (ext_ver 1.11.0) ------*/

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;

    /**
     * 附件集合
     */
    @Schema(description = "附件集合")
    private Map<String, List<Attachment>> attachmentMap;
}
