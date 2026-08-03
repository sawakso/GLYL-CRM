import { MemberSelectTypeEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';

import type { FormDesignKeyEnum, FormLinkScenarioEnum } from '../../enums/formDesignEnum';
import type { ModuleField, TableQueryParams } from '../common';
import type { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

export interface ModuleNavCommon {
  id?: string;
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  organizationId?: string;
  enable: boolean;
  pos?: number;
}

// 模块首页-导航模块列表
export interface ModuleNavBaseInfoItem extends ModuleNavCommon {
  moduleKey: string;
  disabled?: boolean;
}

// 顶部导航配置
export interface ModuleNavTopItem extends ModuleNavCommon {
  navigationKey: string;
}

export interface ModuleNavItem extends ModuleNavBaseInfoItem {
  icon: string;
  key: string;
  label: string;
}

// 模块首页-导航模块排序入参
export interface ModuleSortParams {
  start: number;
  end: number;
  dragModuleId: string; // 拖拽模块ID
}

export interface SelectedUsersItem {
  id: string; // ID
  scope?: MemberSelectTypeEnum; // 范围
  name: string; // 名称
  disabled?: boolean;
  enable?: boolean;
}

export interface ModuleConditionsItem {
  column: string;
  operator: string;
  value: string;
  scope?: string[];
}

export interface OpportunityBaseInfoItem {
  name: string;
  enable: boolean;
  operator: string; // 操作符
  auto: boolean; // 自动回收
}

// 模块商机列表
export interface OpportunityItem extends OpportunityBaseInfoItem {
  id: string;
  organizationId: string;
  ownerId: string; // 管理员ID
  scopeId: string; // 范围ID
  condition: string; // 回收条件
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  members: SelectedUsersItem[]; // 成员集合
  owners: SelectedUsersItem[]; // 管理员集合
  createUserName: string;
  updateUserName: string;
}

// 模块商机详情
export interface OpportunityDetail extends OpportunityBaseInfoItem {
  id?: string;
  conditions: ModuleConditionsItem[]; // 规则条件集合
}

export interface OpportunityParams extends OpportunityDetail {
  scopeIds: string[];
  ownerIds: string[];
}

// 线索池领取规则
export interface CluePoolPickRuleParams {
  limitOnNumber: boolean; // 是否限制领取数量
  pickNumber?: number; // 领取数量
  limitPreOwner: boolean; // 是否限制前归属人领取
  pickIntervalDays?: number; // 领取间隔天数
  limitNew: boolean; // 是否限制新数据领取
  newPickInterval?: number; // 新数据领取间隔天数
}

// 线索池回收规则
export interface CluePoolRecycleRuleParams {
  operator: string; // 操作符
  conditions: ModuleConditionsItem[]; // 规则条件集合
}

// 线索池分配规则匹配条件
export interface AssignRuleCondition {
  fieldId: string; // 自定义字段ID(内容字段) 或 时间字段哨兵(CLUE_CREATE_TIME)
  operator: string; // 操作符: EQUALS/NOT_EQUALS/CONTAINS 或 BEFORE/AFTER/BETWEEN
  value: string; // 匹配值(内容) 或 时间(起始,毫秒时间戳字符串)
  conditionType?: 'FIELD' | 'TIME'; // 条件类型: FIELD(内容字段)/TIME(时间判断)
  value2?: string; // 时间区间结束值(BETWEEN 用,毫秒时间戳字符串)
  timeRange?: number | number[] | null; // 仅前端使用: BETWEEN 时间区间选择器绑定
}

// 线索池分配规则
export interface CluePoolAssignRuleParams {
  id?: string;
  poolId?: string;
  ruleName?: string; // 规则名称
  conditionList?: AssignRuleCondition[]; // 匹配条件
  assignType: string; // 分配方式: SINGLE/ROUND_ROBIN
  assignTargetType?: 'USER' | 'DEPT'; // 目标类型: USER(指定人员)/DEPT(按部门动态解析)
  targetUserNames?: SelectedUsersItem[]; // 目标人员(回显, USER 模式)
  targetDeptNames?: SelectedUsersItem[]; // 目标部门名称(回显, DEPT 模式, 仅前端使用)
  targetDeptIds?: string[]; // 目标部门ID(DEPT 模式)
  includeChildDept?: boolean; // 部门目标是否含子部门
  currentIndex?: number; // 循环分配指针
  pos?: number; // 排序
  enable?: boolean; // 启用/禁用
}

// 编辑线索池请求参数
export interface CluePoolParams {
  id?: string; // ID
  name: string; // 线索池名称
  description?: string; // 线索池描述
  scopeIds: string[]; // 范围ID集合
  ownerIds: string[]; // 管理员ID集合
  collaboratorIds: string[]; // 协同管理员ID集合
  enable: boolean; // 启用/禁用
  auto: boolean; // 自动回收
  pickMode?: string; // 领取模式: VISIBLE_PICKABLE/ADMIN_ASSIGN_ONLY
  newLeadRemind?: boolean; // 新线索提醒
  unassignedReminderMinutes: number; // 未分配超时提醒总分钟数
  unfollowedReminderMinutes: number; // 未跟进超时提醒总分钟数
  notifyPoolAdminOnUnfollowedTimeout: boolean; // 未跟进超时通知池管理员
  allowTransferAfterPick: boolean; // 领取后允许转移
  restrictTransferInToMembers: boolean; // 仅允许转入线索池成员
  restrictReturnToMembers: boolean; // 仅允许退回线索池成员
  clearTeamOnOwnerChange: boolean; // 负责人变化时清空团队
  clearExternalOwnerOnOwnerEmpty: boolean; // 负责人为空时清空外部负责人
  clearExternalTeamOnExternalOwnerEmpty: boolean; // 外部负责人为空时清空外部团队
  clearOwnerOnPoolTransfer: boolean; // 转移线索池时清空负责人
  clearExternalOwnerOnPoolTransfer: boolean; // 转移线索池时清空外部负责人
  allowViewChangeLogBeforePick: boolean; // 领取前可查看变更记录
  allowEditTeamBeforePick: boolean; // 领取前可编辑团队
  allowSendSalesRecordBeforePick: boolean; // 领取前可发送销售记录
  allowViewSalesRecordBeforePick: boolean; // 领取前可查看销售记录
  allowViewPoolLog: boolean; // 可查看线索池日志
  pickRule: CluePoolPickRuleParams; // 领取规则
  recycleRule: CluePoolRecycleRuleParams; // 回收规则
  assignRules: CluePoolAssignRuleParams[]; // 分配规则集合
  autoAssignEnabled?: boolean; // 是否开启定时自动分配
  autoAssignCron?: string; // 定时自动分配 cron 表达式
  hiddenFieldIds: string[]; // 隐藏的表格字段
}

export interface CluePoolForm extends Omit<CluePoolParams, 'scopeIds' | 'ownerIds' | 'collaboratorIds'> {
  adminIds: SelectedUsersItem[];
  collaboratorIds: SelectedUsersItem[]; // 协同管理员
  userIds: SelectedUsersItem[]; // 成员ID
  hiddenFieldIds: string[]; // 隐藏的表格字段
  updateTime?: number; // 最近更新时间，仅用于编辑回显
}

// 线索池列表项
export interface CluePoolItem {
  id: string;
  createUser: string;
  updateUser: string;
  updateUserName: string;
  createTime: number;
  updateTime: number;
  name: string;
  description?: string;
  scopeId: string;
  organizationId: string;
  ownerId: string;
  collaboratorId?: string;
  enable: boolean;
  auto: boolean;
  pickMode?: string; // 领取模式
  newLeadRemind?: boolean; // 新线索提醒
  unassignedReminderMinutes?: number; // 未分配超时提醒总分钟数
  unfollowedReminderMinutes?: number; // 未跟进超时提醒总分钟数
  notifyPoolAdminOnUnfollowedTimeout?: boolean; // 未跟进超时通知池管理员
  allowTransferAfterPick?: boolean; // 领取后允许转移
  restrictTransferInToMembers?: boolean; // 仅允许转入线索池成员
  restrictReturnToMembers?: boolean; // 仅允许退回线索池成员
  clearTeamOnOwnerChange?: boolean; // 负责人变化时清空团队
  clearExternalOwnerOnOwnerEmpty?: boolean; // 负责人为空时清空外部负责人
  clearExternalTeamOnExternalOwnerEmpty?: boolean; // 外部负责人为空时清空外部团队
  clearOwnerOnPoolTransfer?: boolean; // 转移线索池时清空负责人
  clearExternalOwnerOnPoolTransfer?: boolean; // 转移线索池时清空外部负责人
  allowViewChangeLogBeforePick?: boolean; // 领取前可查看变更记录
  allowEditTeamBeforePick?: boolean; // 领取前可编辑团队
  allowSendSalesRecordBeforePick?: boolean; // 领取前可发送销售记录
  allowViewSalesRecordBeforePick?: boolean; // 领取前可查看销售记录
  allowViewPoolLog?: boolean; // 可查看线索池日志
  members: SelectedUsersItem[];
  owners: SelectedUsersItem[];
  collaborators?: SelectedUsersItem[];
  pickRule: CluePoolPickRuleParams; // 领取规则
  recycleRule: CluePoolRecycleRuleParams; // 回收规则
  assignRules?: CluePoolAssignRuleParams[]; // 分配规则集合
  autoAssignEnabled?: boolean; // 是否开启定时自动分配
  autoAssignCron?: string; // 定时自动分配 cron 表达式
  currentClueCount?: number; // 当前线索数量
  fieldConfigs: {
    editable: boolean;
    enable: boolean;
    fieldId: string;
    fieldName: string;
  }[]; // 隐藏的表格字段
}

// 库容参数
export interface CapacityParams {
  scopeIds: string[]; // 范围ID集合
  capacity?: number; // 容量
}

// 库容列表项
export interface CapacityItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  organizationId: string;
  scopeId: string;
  capacity: number;
  members: SelectedUsersItem[];
  filters?: { column: 'string'; operator: 'string'; value: 'string' }[];
}

// 表单设计保存参数
export type FormFooterDirection = 'flex-row' | 'flex-row-reverse' | 'justify-center';
export interface FormActionButton {
  text: string;
  enable: boolean;
}
export interface FormFieldLinkItem {
  current: string;
  link: string;
  enable: boolean;
}
export interface FormConfigLinkScenarioItem {
  key: FormLinkScenarioEnum;
  linkFields: FormFieldLinkItem[];
}
export type FormConfigLinkProp = Partial<Record<FormDesignKeyEnum, FormConfigLinkScenarioItem[]>>;
export type FormViewSize = 'small' | 'medium' | 'large';
export interface FormConfig {
  layout: number;
  labelPos: 'left' | 'top';
  inputWidth: 'custom' | 'full';
  optBtnContent: FormActionButton[];
  optBtnPos: FormFooterDirection;
  viewSize?: FormViewSize;
  linkProp?: FormConfigLinkProp;
}

export interface SaveFormDesignConfigParams {
  formKey: FormDesignKeyEnum;
  fields: FormCreateField[];
  formProp: FormConfig;
}

export interface FormDesignConfigDetailParams {
  fields: FormCreateField[];
  formProp: FormConfig;
}

export interface FormDesignDataSourceTableQueryParams extends TableQueryParams {
  field: string;
}

export interface ReasonParams {
  id?: string;
  name: string;
  module: ReasonTypeEnum;
}

export interface UpdateReasonEnableParams {
  module: ReasonTypeEnum;
  enable: boolean;
}

export interface ReasonItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  name: string;
  module: ReasonTypeEnum;
  type: string;
  organizationId: string;
}

export interface ReasonConfig {
  enable: boolean;
  dictList: ReasonItem[];
}

export interface SortReasonParams {
  dragDictId: string; // 拖拽元素id
  start: number; // 排序前
  end: number; // 排序后
}

export interface DefaultSearchSetFormModel {
  searchFields: Record<string, any>;
  resultDisplay: boolean;
  sortSetting: string[];
}

export interface CheckRepeatParams {
  id: string;
  value: string;
  formKey: string;
}

export interface CheckRepeatInfo {
  repeat: boolean;
  name: string;
}

export interface GetRefDataSourceFieldParams {
  sourceIds: string[];
  dataSourceType: string;
}

export interface RefDataSourceFieldItem {
  id: string;
  moduleFields: ModuleField[];
  optionMap?: Record<string, any[]>;
  [key: string]: any;
}
