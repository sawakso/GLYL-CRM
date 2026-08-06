<template>
  <n-drawer
    v-model:show="visible"
    :title="t('objectSetting.fieldManage') + ' - ' + objectName"
    width="680"
    placement="right"
  >
    <n-drawer-content>
      <template #header>
        <div class="flex w-full items-center justify-between pr-[16px]">
          <span>{{ t('objectSetting.fieldManage') }} - {{ objectName }}</span>
          <n-button size="small" type="primary" @click="handleAddField">
            {{ t('objectSetting.addField') }}
          </n-button>
        </div>
      </template>

      <n-data-table
        :columns="columns"
        :data="fieldList"
        :loading="loading"
        size="small"
        :bordered="false"
        striped
        style="height: 100%"
      />
    </n-drawer-content>
  </n-drawer>

  <!-- 新增字段弹窗 -->
  <n-modal
    v-model:show="addVisible"
    preset="dialog"
    :title="t('objectSetting.addField')"
    positive-text="确定"
    negative-text="取消"
    @positive-click="handleAddConfirm"
  >
    <n-form>
      <n-form-item :label="t('objectSetting.fieldName')">
        <n-input v-model:value="newField.name" placeholder="字段名称" />
      </n-form-item>
      <n-form-item :label="t('objectSetting.fieldType')">
        <n-select v-model:value="newField.type" :options="fieldTypeOptions" placeholder="选择字段类型" />
      </n-form-item>
      <n-form-item :label="t('objectSetting.internalKey')">
        <n-input v-model:value="newField.internalKey" placeholder="内部标识（可选，默认自动生成）" />
      </n-form-item>
    </n-form>
  </n-modal>

  <!-- 编辑字段弹窗 -->
  <n-modal
    v-model:show="editVisible"
    preset="dialog"
    :title="t('objectSetting.editField')"
    positive-text="确定"
    negative-text="取消"
    @positive-click="handleEditConfirm"
  >
    <n-form>
      <n-form-item :label="t('objectSetting.fieldName')">
        <n-input v-model:value="editField.name" placeholder="字段名称" />
      </n-form-item>
      <n-form-item :label="t('objectSetting.internalKey')">
        <n-input v-model:value="editField.internalKey" placeholder="字段Key" />
      </n-form-item>
      <n-form-item :label="t('objectSetting.businessKey')">
        <n-input v-model:value="editField.businessKey" placeholder="业务Key（留空表示无）" />
      </n-form-item>
    </n-form>
  </n-modal>
</template>

<script setup lang="ts">
  import {
    NButton,
    NDataTable,
    NDrawer,
    NDrawerContent,
    NForm,
    NFormItem,
    NInput,
    NModal,
    NSelect,
    NSwitch,
    useDialog,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FieldInfo } from '@lib/shared/models/system/module';

  import { addField, deleteField, getFieldList, toggleField, updateField } from '@/api/modules';
  import { useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  import type { DataTableColumns } from 'naive-ui';

  const visible = defineModel<boolean>('visible', { required: true });
  const props = defineProps<{ formKey: string; objectName: string }>();

  const { t } = useI18n();
  const Message = useMessage();
  const Dialog = useDialog();
  const userStore = useUserStore();

  const loading = ref(false);
  const fieldList = ref<FieldInfo[]>([]);

  async function loadFields() {
    try {
      loading.value = true;
      fieldList.value = await getFieldList(props.formKey);
    } finally {
      loading.value = false;
    }
  }

  watch(visible, (v) => {
    if (v) loadFields();
  });

  // 停用/启用
  async function handleToggle(row: FieldInfo) {
    await toggleField(props.formKey, row.id);
    await loadFields();
    Message.success(t('common.operationSuccess'));
  }

  // 删除: 必须先将字段停用才允许删除, 且需二次确认; 姓名字段非管理员不可删除
  function handleDelete(row: FieldInfo) {
    if (row.businessKey === 'name' && !userStore.isAdmin) {
      Message.warning(t('objectSetting.nameFieldNotDeletable'));
      return;
    }
    if (row.readable) {
      Message.warning(t('objectSetting.disableBeforeDelete'));
      return;
    }
    Dialog.warning({
      title: t('common.delete'),
      content: t('objectSetting.deleteFieldConfirm', { name: row.name }),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        await deleteField(props.formKey, row.id);
        Message.success(t('common.operationSuccess'));
        await loadFields();
      },
    });
  }

  // 新增
  const addVisible = ref(false);
  const newField = ref({ name: '', type: 'INPUT', internalKey: '' });
  const fieldTypeOptions = [
    { label: '单行文本', value: 'INPUT' },
    { label: '多行文本', value: 'TEXTAREA' },
    { label: '数字', value: 'INPUT_NUMBER' },
    { label: '日期', value: 'DATE_TIME' },
    { label: '单选', value: 'SELECT' },
    { label: '多选', value: 'SELECT_MULTIPLE' },
    { label: '单选按钮', value: 'RADIO' },
    { label: '复选框', value: 'CHECKBOX' },
    { label: '手机', value: 'PHONE' },
    { label: '图片', value: 'PICTURE' },
    { label: '定位', value: 'LOCATION' },
    { label: '链接', value: 'LINK' },
    { label: '成员单选', value: 'MEMBER' },
    { label: '成员多选', value: 'MEMBER_MULTIPLE' },
    { label: '部门单选', value: 'DEPARTMENT' },
    { label: '部门多选', value: 'DEPARTMENT_MULTIPLE' },
    { label: '数据源单选', value: 'DATA_SOURCE' },
    { label: '数据源多选', value: 'DATA_SOURCE_MULTIPLE' },
    { label: '附件', value: 'ATTACHMENT' },
    { label: '分隔符', value: 'DIVIDER' },
  ];

  function handleAddField() {
    newField.value = { name: '', type: 'INPUT', internalKey: '' };
    addVisible.value = true;
  }
  async function handleAddConfirm() {
    if (!newField.value.name) {
      Message.warning('字段名称不能为空');
      return false;
    }
    await addField(props.formKey, newField.value);
    Message.success(t('common.operationSuccess'));
    await loadFields();
    return true;
  }

  // 编辑字段名称/key
  const editVisible = ref(false);
  const editField = ref({ id: '', name: '', internalKey: '', businessKey: '' });
  function handleEditField(row: FieldInfo) {
    editField.value = {
      id: row.id,
      name: row.name || '',
      internalKey: row.internalKey || '',
      businessKey: row.businessKey || '',
    };
    editVisible.value = true;
  }
  async function handleEditConfirm() {
    if (!editField.value.name) {
      Message.warning('字段名称不能为空');
      return false;
    }
    await updateField(props.formKey, editField.value);
    Message.success(t('common.operationSuccess'));
    await loadFields();
    return true;
  }

  const columns: DataTableColumns<FieldInfo> = [
    { title: t('objectSetting.fieldName'), key: 'name', width: 160, ellipsis: { tooltip: true } },
    { title: t('objectSetting.fieldType'), key: 'type', width: 100 },
    { title: t('objectSetting.internalKey'), key: 'internalKey', width: 140, ellipsis: { tooltip: true } },
    {
      title: t('objectSetting.status'),
      key: 'readable',
      width: 80,
      render(row) {
        return h(NSwitch, {
          value: row.readable,
          size: 'small',
          rubberBand: false,
          disabled: !hasAnyPermission(['MODULE_SETTING:UPDATE']),
          onUpdateValue: () => handleToggle(row),
        });
      },
    },
    {
      title: t('common.operation'),
      key: 'actions',
      width: 140,
      render(row) {
        if (!hasAnyPermission(['MODULE_SETTING:UPDATE'])) return null;
        const btns = [
          h(
            NButton,
            { size: 'small', type: 'primary', secondary: true, onClick: () => handleEditField(row) },
            { default: () => t('common.edit') }
          ),
        ];
        // 姓名字段非管理员不允许删除(不可见删除按钮)
        if (!(row.businessKey === 'name' && !userStore.isAdmin)) {
          btns.push(
            h(
              NButton,
              { size: 'small', type: 'error', secondary: true, onClick: () => handleDelete(row) },
              { default: () => t('common.delete') }
            )
          );
        }
        return h('div', { class: 'flex gap-[4px]' }, btns);
      },
    },
  ];
</script>
