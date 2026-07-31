// 市场活动表单 (需登录, 市场部管理用)
export const AddMarketingFormUrl = '/marketing-form/add'; // 新建市场表单
export const UpdateMarketingFormUrl = '/marketing-form/update'; // 更新市场表单
export const GetMarketingFormUrl = '/marketing-form/get'; // 市场表单详情
export const GetMarketingFormListUrl = '/marketing-form/list'; // 市场表单列表
export const DeleteMarketingFormUrl = '/marketing-form/delete'; // 删除市场表单
export const UpdateMarketingFormStatusUrl = '/marketing-form/status'; // 更新状态(DRAFT/ACTIVE/CLOSED)

// 公开端点 (免登录, 意向客户扫码填写用)
export const GetPublicMarketingFormUrl = '/pub/marketing-form'; // 获取公开表单配置
export const SubmitPublicMarketingFormUrl = '/pub/marketing-form'; // 提交表单(回流成线索)
