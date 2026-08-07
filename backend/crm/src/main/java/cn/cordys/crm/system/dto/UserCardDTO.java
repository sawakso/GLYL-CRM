package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户简要卡片信息(点击员工姓名/头像时悬浮展示)
 */
@Data
public class UserCardDTO {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "工号")
    private String employeeId;

    @Schema(description = "工作城市")
    private String workCity;

    @Schema(description = "直属上级ID")
    private String supervisorId;

    @Schema(description = "直属上级姓名")
    private String supervisorName;

    @Schema(description = "主属部门ID")
    private String departmentId;

    @Schema(description = "主属部门名称")
    private String departmentName;

    @Schema(description = "部门路径名称(如 业务部/专业销售部/华北区)")
    private String deptPath;
}
