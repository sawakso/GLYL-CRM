package cn.cordys.crm.workflow.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.workflow.domain.WorkflowDefinition;
import cn.cordys.crm.workflow.domain.WorkflowEdge;
import cn.cordys.crm.workflow.domain.WorkflowNode;
import cn.cordys.crm.workflow.dto.WorkflowDefinitionDTO;
import cn.cordys.crm.workflow.dto.WorkflowEdgeDTO;
import cn.cordys.crm.workflow.dto.WorkflowNodeDTO;
import cn.cordys.crm.workflow.dto.WorkflowSaveRequest;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流定义管理
 */
@Service
public class WorkflowService {

    @Resource
    private BaseMapper<WorkflowDefinition> workflowDefinitionMapper;

    @Resource
    private BaseMapper<WorkflowNode> workflowNodeMapper;

    @Resource
    private BaseMapper<WorkflowEdge> workflowEdgeMapper;

    /**
     * 获取工作流列表(不含节点和连线)
     */
    public List<WorkflowDefinitionDTO> list(String orgId, String workflowType) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowDefinition::getOrganizationId, orgId);
        if (StringUtils.isNotBlank(workflowType)) {
            wrapper.eq(WorkflowDefinition::getWorkflowType, workflowType);
        }
        wrapper.orderByAsc(WorkflowDefinition::getCreateTime);

        List<WorkflowDefinition> definitions = workflowDefinitionMapper.selectListByLambda(wrapper);
        return definitions.stream().map(this::toDTO).toList();
    }

    /**
     * 获取工作流详情(含节点和连线)
     */
    public WorkflowDefinitionDTO getDetail(String id, String orgId) {
        WorkflowDefinition definition = workflowDefinitionMapper.selectByPrimaryKey(id);
        if (definition == null || !StringUtils.equals(definition.getOrganizationId(), orgId)) {
            return null;
        }

        WorkflowDefinitionDTO dto = toDTO(definition);

        // 查询节点
        LambdaQueryWrapper<WorkflowNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(WorkflowNode::getWorkflowId, id);
        nodeWrapper.orderByAsc(WorkflowNode::getSortOrder);
        List<WorkflowNode> nodes = workflowNodeMapper.selectListByLambda(nodeWrapper);
        dto.setNodes(nodes.stream().map(this::toNodeDTO).toList());

        // 查询连线
        LambdaQueryWrapper<WorkflowEdge> edgeWrapper = new LambdaQueryWrapper<>();
        edgeWrapper.eq(WorkflowEdge::getWorkflowId, id);
        List<WorkflowEdge> edges = workflowEdgeMapper.selectListByLambda(edgeWrapper);
        dto.setEdges(edges.stream().map(this::toEdgeDTO).toList());

        return dto;
    }

    /**
     * 保存工作流(新增或更新, 全量替换节点和连线)
     */
    @Transactional(rollbackFor = Exception.class)
    public String save(WorkflowSaveRequest request, String userId, String orgId) {
        WorkflowDefinition definition;
        boolean isNew = StringUtils.isBlank(request.getId());

        if (isNew) {
            definition = new WorkflowDefinition();
            definition.setId(IDGenerator.nextStr());
            definition.setOrganizationId(orgId);
            definition.setCreateTime(System.currentTimeMillis());
            definition.setCreateUser(userId);
        } else {
            definition = workflowDefinitionMapper.selectByPrimaryKey(request.getId());
            if (definition == null || !StringUtils.equals(definition.getOrganizationId(), orgId)) {
                throw new IllegalArgumentException("工作流不存在");
            }
            // 删除旧的节点和连线
            deleteNodesAndEdges(definition.getId());
        }

        definition.setName(request.getName());
        definition.setDescription(request.getDescription());
        definition.setWorkflowType(request.getWorkflowType());
        definition.setFormKey(request.getFormKey());
        definition.setTriggerType(request.getTriggerType());
        definition.setTriggerConfig(request.getTriggerConfig());
        definition.setEnable(request.getEnable() != null ? request.getEnable() : true);
        definition.setUpdateTime(System.currentTimeMillis());
        definition.setUpdateUser(userId);

        if (isNew) {
            workflowDefinitionMapper.insert(definition);
        } else {
            workflowDefinitionMapper.update(definition);
        }

        // 保存节点
        if (!CollectionUtils.isEmpty(request.getNodes())) {
            List<WorkflowNode> nodes = new ArrayList<>();
            for (int i = 0; i < request.getNodes().size(); i++) {
                WorkflowNodeDTO nodeDTO = request.getNodes().get(i);
                WorkflowNode node = new WorkflowNode();
                node.setId(StringUtils.isNotBlank(nodeDTO.getId()) ? nodeDTO.getId() : IDGenerator.nextStr());
                node.setWorkflowId(definition.getId());
                node.setNodeType(nodeDTO.getNodeType());
                node.setNodeKey(nodeDTO.getNodeKey());
                node.setName(nodeDTO.getName());
                node.setConfig(nodeDTO.getConfig());
                node.setPosX(nodeDTO.getPosX() != null ? nodeDTO.getPosX() : 0);
                node.setPosY(nodeDTO.getPosY() != null ? nodeDTO.getPosY() : 0);
                node.setSortOrder(nodeDTO.getSortOrder() != null ? nodeDTO.getSortOrder() : i);
                node.setCreateTime(System.currentTimeMillis());
                node.setUpdateTime(System.currentTimeMillis());
                node.setCreateUser(userId);
                node.setUpdateUser(userId);
                nodes.add(node);
            }
            workflowNodeMapper.batchInsert(nodes);
        }

        // 保存连线
        if (!CollectionUtils.isEmpty(request.getEdges())) {
            List<WorkflowEdge> edges = new ArrayList<>();
            for (WorkflowEdgeDTO edgeDTO : request.getEdges()) {
                WorkflowEdge edge = new WorkflowEdge();
                edge.setId(StringUtils.isNotBlank(edgeDTO.getId()) ? edgeDTO.getId() : IDGenerator.nextStr());
                edge.setWorkflowId(definition.getId());
                edge.setSourceNodeId(edgeDTO.getSourceNodeId());
                edge.setTargetNodeId(edgeDTO.getTargetNodeId());
                edge.setConditionExpr(edgeDTO.getConditionExpr());
                edge.setEdgeType(StringUtils.isNotBlank(edgeDTO.getEdgeType()) ? edgeDTO.getEdgeType() : "DEFAULT");
                edge.setCreateTime(System.currentTimeMillis());
                edge.setUpdateTime(System.currentTimeMillis());
                edge.setCreateUser(userId);
                edge.setUpdateUser(userId);
                edges.add(edge);
            }
            workflowEdgeMapper.batchInsert(edges);
        }

        return definition.getId();
    }

    /**
     * 删除工作流
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, String orgId) {
        WorkflowDefinition definition = workflowDefinitionMapper.selectByPrimaryKey(id);
        if (definition == null || !StringUtils.equals(definition.getOrganizationId(), orgId)) {
            return;
        }
        deleteNodesAndEdges(id);
        workflowDefinitionMapper.deleteByPrimaryKey(id);
    }

    /**
     * 启用/禁用工作流
     */
    @Transactional(rollbackFor = Exception.class)
    public void switchEnable(String id, String userId, String orgId) {
        WorkflowDefinition definition = workflowDefinitionMapper.selectByPrimaryKey(id);
        if (definition == null || !StringUtils.equals(definition.getOrganizationId(), orgId)) {
            return;
        }
        WorkflowDefinition update = new WorkflowDefinition();
        update.setId(id);
        update.setEnable(!definition.getEnable());
        update.setUpdateTime(System.currentTimeMillis());
        update.setUpdateUser(userId);
        workflowDefinitionMapper.update(update);
    }

    private void deleteNodesAndEdges(String workflowId) {
        LambdaQueryWrapper<WorkflowNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(WorkflowNode::getWorkflowId, workflowId);
        workflowNodeMapper.deleteByLambda(nodeWrapper);

        LambdaQueryWrapper<WorkflowEdge> edgeWrapper = new LambdaQueryWrapper<>();
        edgeWrapper.eq(WorkflowEdge::getWorkflowId, workflowId);
        workflowEdgeMapper.deleteByLambda(edgeWrapper);
    }

    private WorkflowDefinitionDTO toDTO(WorkflowDefinition definition) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setId(definition.getId());
        dto.setName(definition.getName());
        dto.setDescription(definition.getDescription());
        dto.setWorkflowType(definition.getWorkflowType());
        dto.setFormKey(definition.getFormKey());
        dto.setTriggerType(definition.getTriggerType());
        dto.setTriggerConfig(definition.getTriggerConfig());
        dto.setEnable(definition.getEnable());
        dto.setCreateTime(definition.getCreateTime());
        dto.setUpdateTime(definition.getUpdateTime());
        dto.setCreateUser(definition.getCreateUser());
        dto.setUpdateUser(definition.getUpdateUser());
        return dto;
    }

    private WorkflowNodeDTO toNodeDTO(WorkflowNode node) {
        WorkflowNodeDTO dto = new WorkflowNodeDTO();
        dto.setId(node.getId());
        dto.setNodeType(node.getNodeType());
        dto.setNodeKey(node.getNodeKey());
        dto.setName(node.getName());
        dto.setConfig(node.getConfig());
        dto.setPosX(node.getPosX());
        dto.setPosY(node.getPosY());
        dto.setSortOrder(node.getSortOrder());
        return dto;
    }

    private WorkflowEdgeDTO toEdgeDTO(WorkflowEdge edge) {
        WorkflowEdgeDTO dto = new WorkflowEdgeDTO();
        dto.setId(edge.getId());
        dto.setSourceNodeId(edge.getSourceNodeId());
        dto.setTargetNodeId(edge.getTargetNodeId());
        dto.setConditionExpr(edge.getConditionExpr());
        dto.setEdgeType(edge.getEdgeType());
        return dto;
    }
}
