<template>
  <span class="crm-user-name-wrapper">
    <NPopover
      v-if="userId"
      trigger="click"
      :show="visible"
      placement="right-start"
      :show-arrow="false"
      :to="false"
      style="padding: 0; border: none"
      @update:show="onShowChange"
    >
      <template #trigger>
        <span class="crm-user-name-trigger" @click.stop="visible = true">
          <slot name="prefix"></slot>
          <span class="crm-user-name-text">{{ nameText }}</span>
        </span>
      </template>
      <div class="crm-user-card" @click.stop>
        <div class="crm-user-card-header">
          <NAvatar round :size="44" :src="card?.avatar || undefined" :fallback-src="undefined">
            {{ card?.name?.charAt(0) || nameText?.charAt(0) || '?' }}
          </NAvatar>
          <div class="crm-user-card-headinfo">
            <div class="crm-user-card-name">{{ card?.name || nameText }}</div>
            <div v-if="card?.position" class="crm-user-card-pos">{{ card.position }}</div>
          </div>
        </div>
        <div v-if="loading" class="crm-user-card-loading">加载中...</div>
        <div v-else-if="card" class="crm-user-card-body">
          <div v-if="departmentPath.length" class="crm-user-card-row crm-user-card-row-department">
            <span class="crm-user-card-label">部门</span>
            <div class="crm-user-card-value">
              <div v-for="(item, index) in departmentPath" :key="index" class="crm-user-card-dept-line">
                {{ item }}
              </div>
            </div>
          </div>
          <div v-if="card?.supervisorName" class="crm-user-card-row">
            <span class="crm-user-card-label">直属上级</span>
            <span class="crm-user-card-value">{{ card.supervisorName }}</span>
          </div>
          <div v-if="card?.phone" class="crm-user-card-row">
            <span class="crm-user-card-label">手机</span>
            <span class="crm-user-card-value">{{ card.phone }}</span>
          </div>
          <div v-if="card?.email" class="crm-user-card-row">
            <span class="crm-user-card-label">邮箱</span>
            <span class="crm-user-card-value">{{ card.email }}</span>
          </div>
          <div v-if="card?.workCity" class="crm-user-card-row">
            <span class="crm-user-card-label">城市</span>
            <span class="crm-user-card-value">{{ card.workCity }}</span>
          </div>
          <div v-if="card?.employeeId" class="crm-user-card-row">
            <span class="crm-user-card-label">工号</span>
            <span class="crm-user-card-value">{{ card.employeeId }}</span>
          </div>
        </div>
        <div v-else class="crm-user-card-loading">暂无信息</div>
      </div>
    </NPopover>
    <span v-else class="crm-user-name-plain">{{ nameText || '-' }}</span>
  </span>
</template>

<script setup lang="ts">
  import { NAvatar, NPopover } from 'naive-ui';
  import { computed, ref } from 'vue';

  import { getUserCard } from '@/api/modules';
  import type { UserCardInfo } from '@lib/shared/models/system/org';

  const props = withDefaults(
    defineProps<{
      userId?: string;
      name?: string;
    }>(),
    {
      userId: '',
      name: '',
    }
  );

  const visible = ref(false);
  const loading = ref(false);
  const card = ref<UserCardInfo | null>(null);

  const nameText = props.name || '-';

  // 部门路径按 / 拆分，每个层级显示一行
  const departmentPath = computed(() => {
    const path = card.value?.deptPath || card.value?.departmentName || '';
    return String(path)
      .split('/')
      .map((item) => item.trim())
      .filter(Boolean);
  });

  async function loadCard() {
    if (!props.userId || card.value) return;
    loading.value = true;
    try {
      const res = await getUserCard(props.userId);
      card.value = res as unknown as UserCardInfo;
    } catch {
      // 加载失败时卡片显示"暂无信息"
    } finally {
      loading.value = false;
    }
  }

  function onShowChange(show: boolean) {
    visible.value = show;
    if (show) loadCard();
  }
</script>

<style scoped lang="less">
  .crm-user-name-trigger {
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    color: var(--primary-color, #18a058);
    &:hover {
      text-decoration: underline;
    }
  }
  .crm-user-name-text {
    overflow: hidden;
    max-width: 150px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .crm-user-name-plain {
    color: inherit;
  }
  .crm-user-card {
    padding: 16px;
    width: 280px;
    font-size: 13px;
    border-radius: 8px;
    background: #ffffff;
  }
  .crm-user-card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--divider-color, #eeeeee);
  }
  .crm-user-card-headinfo {
    min-width: 0;
  }
  .crm-user-card-name {
    font-size: 16px;
    font-weight: 600;
  }
  .crm-user-card-pos {
    margin-top: 2px;
    color: var(--text-color-3, #888888);
  }
  .crm-user-card-loading {
    padding-top: 12px;
    text-align: center;
    color: var(--text-color-3, #888888);
  }
  .crm-user-card-body {
    padding-top: 12px;
  }
  .crm-user-card-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 4px 0;
  }
  .crm-user-card-label {
    flex-shrink: 0;
    width: 60px;
    color: var(--text-color-3, #888888);
  }
  .crm-user-card-value {
    min-width: 0;
    flex: 1;
    overflow-wrap: anywhere;
    word-break: break-all;
  }
  .crm-user-card-row-department {
    align-items: flex-start;
  }
  .crm-user-card-dept-line {
    line-height: 1.5;
  }
  .crm-user-card-dept-line + .crm-user-card-dept-line {
    margin-top: 2px;
  }
</style>
