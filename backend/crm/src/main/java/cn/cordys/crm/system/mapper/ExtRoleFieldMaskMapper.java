package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.RoleFieldMask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色字段脱敏配置扩展查询
 */
public interface ExtRoleFieldMaskMapper {

    /**
     * 按角色ID删除全部脱敏配置
     */
    void deleteByRoleId(@Param("roleId") String roleId);

    /**
     * 按角色ID查询全部脱敏配置
     */
    List<RoleFieldMask> selectByRoleId(@Param("roleId") String roleId);

    /**
     * 按角色ID和模块key查询脱敏配置
     */
    List<RoleFieldMask> selectByRoleIdAndModule(@Param("roleId") String roleId, @Param("moduleKey") String moduleKey);

    /**
     * 按多个角色ID和模块key批量查询脱敏配置
     */
    List<RoleFieldMask> selectByRoleIdsAndModule(@Param("roleIds") List<String> roleIds, @Param("moduleKey") String moduleKey);
}
