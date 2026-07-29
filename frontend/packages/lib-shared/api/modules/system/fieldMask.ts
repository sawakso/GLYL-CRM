import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  GetFieldMaskConfigUrl,
  GetFieldMaskFieldsUrl,
  SaveFieldMaskConfigUrl,
} from '@lib/shared/api/requrls/system/fieldMask';

export interface FieldMaskFieldItem {
  id: string;
  name: string;
  type: string;
  internalKey?: string;
}

export interface FieldMaskConfigItem {
  moduleKey: string;
  fieldId?: string;
  fieldKey?: string;
  fieldType: string;
}

export interface FieldMaskConfigParams {
  roleId: string;
  masks: FieldMaskConfigItem[];
}

export default function (CDR: CordysAxios) {
  function getFieldMaskFields(moduleKey: string) {
    return CDR.get<FieldMaskFieldItem[]>({ url: `${GetFieldMaskFieldsUrl}/${moduleKey}` });
  }

  function getFieldMaskConfig(roleId: string) {
    return CDR.get<FieldMaskConfigItem[]>({ url: `${GetFieldMaskConfigUrl}/${roleId}` });
  }

  function saveFieldMaskConfig(data: FieldMaskConfigParams) {
    return CDR.post({ url: SaveFieldMaskConfigUrl, data });
  }

  return {
    getFieldMaskFields,
    getFieldMaskConfig,
    saveFieldMaskConfig,
  };
}
