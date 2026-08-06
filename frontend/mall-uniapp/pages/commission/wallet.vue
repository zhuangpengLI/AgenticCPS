<template>
  <view />
</template>

<script setup>
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';

  const buildQuery = (options = {}) =>
    Object.entries(options)
      .map(
        ([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value ?? ''))}`,
      )
      .join('&');

  onLoad((options) => {
    const query = buildQuery(options);
    sheep.$router.redirect(`/pages/cps/wallet${query ? `?${query}` : ''}`);
  });
</script>
