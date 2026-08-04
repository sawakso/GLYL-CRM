import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddMarketingFormUrl,
  DeleteMarketingFormUrl,
  GetMarketingFormListUrl,
  GetMarketingFormUrl,
  GetPublicMarketingFormUrl,
  SubmitPublicMarketingFormUrl,
  UpdateMarketingFormStatusUrl,
  UpdateMarketingFormUrl,
} from '@lib/shared/api/requrls/marketingForm';

export interface MarketingFormSaveRequest {
  id?: string;
  name: string;
  description?: string;
  targetPoolId?: string;
  fieldMapping?: string;
  dedupStrategy?: string;
  dedupWindow?: number | null;
  dedupKey?: string | null;
  requireName?: boolean;
  fields?: any[];
  formProp?: any;
}

export interface MarketingFormListItem {
  id: string;
  name: string;
  description?: string;
  targetPoolId?: string;
  targetPoolName?: string;
  fieldMapping?: string;
  dedupStrategy?: string;
  dedupWindow?: number | null;
  dedupKey?: string | null;
  requireName?: boolean;
  qrToken?: string;
  status?: string;
  submissionCount?: number;
  createTime?: number;
}

export interface MarketingFormDetail extends MarketingFormListItem {
  fields?: any[];
  formProp?: any;
}

export interface MarketingFormPublicConfig {
  name: string;
  description?: string;
  fields?: any[];
  formProp?: any;
  organizationId?: string;
  dedupTip?: string; // 去重提示文案(未启用去重时为 null)
  requireName?: boolean; // 是否强制姓名必填才能提交
}

export default function useMarketingFormApi(CDR: CordysAxios) {
  function addMarketingForm(data: MarketingFormSaveRequest) {
    return CDR.post({ url: AddMarketingFormUrl, data });
  }

  function updateMarketingForm(data: MarketingFormSaveRequest) {
    return CDR.post({ url: UpdateMarketingFormUrl, data });
  }

  function getMarketingFormDetail(id?: string) {
    return CDR.get<MarketingFormDetail>({ url: `${GetMarketingFormUrl}/${id}` });
  }

  function getMarketingFormList() {
    return CDR.get<MarketingFormListItem[]>({ url: GetMarketingFormListUrl });
  }

  function deleteMarketingForm(id: string) {
    return CDR.get({ url: `${DeleteMarketingFormUrl}/${id}` });
  }

  function updateMarketingFormStatus(id: string, status: string) {
    return CDR.get({ url: `${UpdateMarketingFormStatusUrl}/${id}/${status}` });
  }

  // 公开端点 (免登录)
  function getPublicMarketingForm(token: string) {
    return CDR.get<MarketingFormPublicConfig>({ url: `${GetPublicMarketingFormUrl}/${token}` });
  }

  function submitPublicMarketingForm(token: string, data: { moduleFields: any[]; deviceId?: string }) {
    return CDR.post<string>({ url: `${SubmitPublicMarketingFormUrl}/${token}/submit`, data });
  }

  return {
    addMarketingForm,
    updateMarketingForm,
    getMarketingFormDetail,
    getMarketingFormList,
    deleteMarketingForm,
    updateMarketingFormStatus,
    getPublicMarketingForm,
    submitPublicMarketingForm,
  };
}
