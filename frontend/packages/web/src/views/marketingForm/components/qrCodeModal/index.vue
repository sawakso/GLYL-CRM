<template>
  <n-modal v-model:show="visible" preset="card" :title="t('marketingForm.qrCode')" class="w-[420px]" :bordered="false">
    <div v-if="formItem" class="flex flex-col items-center gap-[16px] py-[16px]">
      <div class="text-[16px] font-medium">{{ formItem.name }}</div>
      <div class="text-[12px] text-[var(--text-n3)]">{{ t('marketingForm.scanTip') }}</div>
      <div v-if="qrDataUrl" class="rounded-[8px] border border-[var(--border-2)] p-[12px]">
        <img :src="qrDataUrl" alt="QR Code" class="h-[220px] w-[220px]" />
      </div>
      <n-spin v-else size="large" />
      <div class="flex w-full items-center gap-[8px]">
        <n-input :value="qrUrl" readonly size="small" class="flex-1" />
        <n-button size="small" type="primary" @click="handleCopyUrl">
          {{ t('marketingForm.copyUrl') }}
        </n-button>
      </div>
      <n-button size="small" tertiary @click="handleDownload">
        {{ t('marketingForm.downloadQr') }}
      </n-button>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
  import { NButton, NInput, NModal, NSpin, useMessage } from 'naive-ui';

  import type { MarketingFormListItem } from '@lib/shared/api/modules/marketingForm';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import QRCode from 'qrcode';

  const props = defineProps<{
    formItem: MarketingFormListItem | null;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const Message = useMessage();

  const qrDataUrl = ref('');

  // QR URL: {VITE_PUBLIC_FORM_BASE_URL}/mobile/#/pub/form/{token}
  // 开发环境指向移动端 dev server (端口 3000), 生产环境指向部署域名。
  const qrUrl = computed(() => {
    if (!props.formItem?.qrToken) return '';
    const baseUrl = (import.meta.env.VITE_PUBLIC_FORM_BASE_URL || window.location.origin).replace(/\/+$/, '');
    return `${baseUrl}/mobile/#/pub/form/${props.formItem.qrToken}`;
  });

  async function generateQr() {
    if (!qrUrl.value) {
      qrDataUrl.value = '';
      return;
    }
    try {
      qrDataUrl.value = await QRCode.toDataURL(qrUrl.value, {
        width: 220,
        margin: 1,
        color: { dark: '#000000', light: '#ffffff' },
      });
    } catch (error) {
      console.error(error);
      qrDataUrl.value = '';
    }
  }

  function handleCopyUrl() {
    if (!qrUrl.value) return;
    navigator.clipboard
      .writeText(qrUrl.value)
      .then(() => {
        Message.success(t('marketingForm.copySuccess'));
      })
      .catch(() => {
        Message.error(t('common.copyNotSupport'));
      });
  }

  function handleDownload() {
    if (!qrDataUrl.value) return;
    const link = document.createElement('a');
    link.href = qrDataUrl.value;
    link.download = `marketing-form-${props.formItem?.qrToken || 'qr'}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        generateQr();
      } else {
        qrDataUrl.value = '';
      }
    }
  );
</script>
