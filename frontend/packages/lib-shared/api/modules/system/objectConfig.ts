import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  GetObjectConfigListUrl,
  GetObjectNameMapUrl,
  GetObjectNameUrl,
  RenameObjectConfigUrl,
  SwitchObjectConfigUrl,
} from '@lib/shared/api/requrls/system/objectConfig';

export interface ObjectConfigItem {
  id: string | null;
  key: string;
  name: string;
  defaultName: string;
  type: 'PRESET' | 'CUSTOM';
  enable: boolean;
  deletable: boolean;
  formId: string | null;
}

export interface ObjectConfigRenameParams {
  key: string;
  name?: string;
}

export default function useObjectConfigApi(CDR: CordysAxios) {
  function getObjectConfigList() {
    return CDR.get<ObjectConfigItem[]>({ url: GetObjectConfigListUrl });
  }

  function renameObjectConfig(data: ObjectConfigRenameParams) {
    return CDR.post({ url: RenameObjectConfigUrl, data });
  }

  function switchObjectConfig(key: string) {
    return CDR.get({ url: `${SwitchObjectConfigUrl}/${key}` });
  }

  function getObjectName(key: string) {
    return CDR.get<string>({ url: `${GetObjectNameUrl}/${key}` });
  }

  function getObjectNameMap() {
    return CDR.get<Record<string, string>>({ url: GetObjectNameMapUrl });
  }

  return {
    getObjectConfigList,
    renameObjectConfig,
    switchObjectConfig,
    getObjectName,
    getObjectNameMap,
  };
}
