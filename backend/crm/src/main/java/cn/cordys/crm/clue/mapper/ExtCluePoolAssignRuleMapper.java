package cn.cordys.crm.clue.mapper;

import cn.cordys.crm.clue.domain.CluePoolAssignRule;
import cn.cordys.crm.clue.dto.CluePoolAssignRuleDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCluePoolAssignRuleMapper {

    /**
     * 批量插入分配规则
     *
     * @param rules 分配规则集合
     */
    void batchInsert(@Param("rules") List<CluePoolAssignRule> rules);

    /**
     * 删除线索池的所有分配规则
     *
     * @param poolId 线索池ID
     */
    void deleteByPoolId(@Param("poolId") String poolId, @Param("organizationId") String organizationId);

    /**
     * 查询线索池的分配规则(按 pos 排序)
     *
     * @param poolId 线索池ID
     *
     * @return 分配规则集合(含目标人员名称)
     */
    List<CluePoolAssignRuleDTO> selectByPoolId(@Param("poolId") String poolId);

    /**
     * 批量查询多个线索池的分配规则(分页回显用)
     *
     * @param poolIds 线索池ID集合
     *
     * @return 分配规则集合(含目标人员名称)
     */
    List<CluePoolAssignRuleDTO> selectByPoolIds(@Param("poolIds") List<String> poolIds);

    /**
     * 更新循环分配当前指针
     *
     * @param id           规则ID
     * @param currentIndex 当前指针
     * @param updateUser   更新人
     * @param updateTime   更新时间
     */
    int compareAndSetCurrentIndex(@Param("id") String id, @Param("organizationId") String organizationId,
                                  @Param("expectedIndex") Integer expectedIndex,
                                  @Param("currentIndex") Integer currentIndex,
                                  @Param("updateUser") String updateUser, @Param("updateTime") Long updateTime);

    /**
     * 查询启用的分配规则(按 pos 排序,定时任务/触发用)
     *
     * @param poolId 线索池ID
     *
     * @return 分配规则集合(原始实体)
     */
    List<CluePoolAssignRule> selectEnabledByPoolId(@Param("poolId") String poolId,
                                                   @Param("organizationId") String organizationId);
}
