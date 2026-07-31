package cn.cordys.crm.clue.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ClueAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "客户名称")
    private String name;

    @Size(max = 32)
    @Schema(description = "负责人")
    private String owner;

    @Size(max = 255)
    @Schema(description = "联系人名称")
    private String contact;

    @Size(max = 30)
    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "意向产品", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> products;

    /*------ start: 纷享销客扩展字段 (ext_ver 1.11.0) ------*/
    @Size(max = 30) @Schema(description = "电话") private String tel;
    @Size(max = 30) @Schema(description = "手机") private String mobile;
    @Size(max = 100) @Schema(description = "邮件") private String email;
    @Size(max = 255) @Schema(description = "地址") private String address;
    @Size(max = 255) @Schema(description = "网址") private String url;
    @Size(max = 255) @Schema(description = "企业名称") private String company;
    @Size(max = 255) @Schema(description = "部门") private String department;
    @Size(max = 255) @Schema(description = "职务") private String jobTitle;
    @Schema(description = "名片") private String picturePath;
    @Size(max = 30) @Schema(description = "来源") private String source;
    @Size(max = 30) @Schema(description = "线索阶段") private String leadsStage;
    @Size(max = 30) @Schema(description = "业务状态") private String bizStatus;
    @Size(max = 30) @Schema(description = "生命状态") private String lifeStatus;
    @Size(max = 30) @Schema(description = "锁定状态") private String lockStatus;
    @Size(max = 30) @Schema(description = "业务类型") private String recordType;
    @Schema(description = "下次跟进时间") private Long nextFollowedTime;
    @Size(max = 1000) @Schema(description = "下次跟进要点") private String nextFollowedRemark;
    @Schema(description = "最后跟进时间") private Long lastFollowTime;
    @Size(max = 32) @Schema(description = "最后跟进人") private String lastFollower;
    @Size(max = 2000) @Schema(description = "客户咨询详情") private String remark;
    @Size(max = 32) @Schema(description = "外部负责人") private String outOwner;
    @Size(max = 32) @Schema(description = "分配管理员") private String assignerId;
    @Size(max = 32) @Schema(description = "归属部门") private String dataOwnDepartment;
    @Size(max = 255) @Schema(description = "负责人主属部门") private String ownerDepartment;
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
    @Size(max = 32) @Schema(description = "销售人员退回原因") private String backReason;
    @Size(max = 32) @Schema(description = "线索无效原因") private String closeReason;
    @Size(max = 2000) @Schema(description = "处理结果") private String completedResult;
    @Schema(description = "申请延期天数") private Integer extendDays;
    @Size(max = 1000) @Schema(description = "延期原因") private String extendReason;
    @Size(max = 32) @Schema(description = "市场活动名称") private String marketingEventId;
    @Size(max = 32) @Schema(description = "归集到的线索") private String collectedTo;
    @Size(max = 255) @Schema(description = "手机归属地") private String phoneNumberAttribution;
    @Size(max = 100) @Schema(description = "企业微信UserId") private String enterpriseWechatUserId;
    /*------ end: 纷享销客扩展字段 (ext_ver 1.11.0) ------*/

    @Schema(description = "模块字段值")
    private List<BaseModuleFieldValue> moduleFields;
}