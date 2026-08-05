<template>
  <CrmModal
    v-model:show="visible"
    :title="t('clue.statusTransition')"
    :positive-text="t('common.confirm')"
    :negative-text="t('common.cancel')"
    :width="520"
    :confirm-loading="loading"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <div v-if="loadingData" class="flex justify-center py-[40px]">
      <n-spin size="medium" />
    </div>
    <template v-else>
      <div class="mb-[20px] rounded-md bg-[var(--primary-7)] px-[16px] py-[12px]">
        <div class="text-[13px] text-[var(--text-n2)]">{{ t('clue.currentStage') }}</div>
        <div class="mt-[4px] flex items-center gap-[8px]">
          <n-tag type="info" :bordered="false" size="medium">{{ transitionData?.currentStageName || '-' }}</n-tag>
          <span v-if="transitionData?.currentLeadsStage" class="text-[12px] text-[var(--text-n3)]">
            {{ t('clue.leadsStage') }}: {{ transitionData?.currentLeadsStage }}
          </span>
        </div>
      </div>

      <div v-if="!transitionData?.availableTransitions?.length" class="py-[20px] text-center text-[var(--text-n3)]">
        {{ t('clue.noAvailableTransitions') }}
      </div>
      <template v-else>
        <div class="mb-[12px] text-[14px] font-medium text-[var(--text-n1)]">{{ t('clue.selectTargetStage') }}</div>
        <n-radio-group v-model:value="selectedStage" class="flex flex-col gap-[8px]">
          <div
            v-for="opt in transitionData?.availableTransitions"
            :key="opt.stage"
            class="flex items-center gap-[12px] rounded-md border px-[12px] py-[10px] transition-all"
            :class="selectedStage === opt.stage ? 'border-[var(--primary-8)] bg-[var(--primary-7)]' : 'border-[var(--divider-n1)]'"
            @click="selectedStage = opt.stage"
          >
            <n-radio :value="opt.stage" />
            <div class="flex-1">
              <div class="text-[14px] font-medium text-[var(--text-n1)]">{{ opt.stageName }}</div>
              <div class="mt-[2px] text-[12px] text-[var(--text-n3)]">
                {{ t('clue.leadsStage') }}: {{ opt.suggestedLeadsStage }} &middot;
                {{ t('clue.bizStatus') }}: {{ opt.suggestedBizStatus }}
              </div>
            </div>
          </div>
        </n-radio-group>
      </template>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NRadio, NRadioGroup, NSpin, NTag } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ClueStatusTransition } from '@lib/shared/models/clue';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { getClueStatusTransitions, transitionClueStatus } from '@/api/modules';

  const { t } = useI18n();

  const props = defineProps<{
    clueId: string;
  }>();

  const emit = defineEmits<{
    (e: 'success'): void;
  }>();

  const visible = defineModel<boolean>('show', { required: true });

  const loading = ref(false);
  const loadingData = ref(false);
  const transitionData = ref<ClueStatusTransition | null>(null);
  const selectedStage = ref('');

  async function loadTransitions() {
    loadingData.value = true;
    try {
      transitionData.value = await getClueStatusTransitions(props.clueId);
    } catch (e) {
      console.error(e);
    } finally {
      loadingData.value = false;
    }
  }

  async function handleConfirm() {
    if (!selectedStage.value) return;
    loading.value = true;
    try {
      const opt = transitionData.value?.availableTransitions?.find((t) => t.stage === selectedStage.value);
      await transitionClueStatus({
        id: props.clueId,
        stage: selectedStage.value,
        leadsStage: opt?.suggestedLeadsStage,
        bizStatus: opt?.suggestedBizStatus,
      });
      emit('success');
      visible.value = false;
    } catch (e) {
      console.error(e);
    } finally {
      loading.value = false;
    }
  }

  function handleCancel() {
    selectedStage.value = '';
    visible.value = false;
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        selectedStage.value = '';
        loadTransitions();
      }
    }
  );
</script>

<style scoped></style>
