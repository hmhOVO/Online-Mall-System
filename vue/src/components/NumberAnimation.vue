<template>
  <span class="number-animation">{{ animatedValue }}</span>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';

const props = defineProps({
  targetValue: {
    type: [Number, String],
    required: true
  },
  duration: {
    type: Number,
    default: 800
  },
  suffix: {
    type: String,
    default: ''
  },
  decimals: {
    type: Number,
    default: 0
  }
});

const animatedValue = ref(0);

const animateNumber = (target) => {
  const numTarget = typeof target === 'string' ? parseFloat(target) : target;
  if (isNaN(numTarget)) {
    animatedValue.value = target + props.suffix;
    return;
  }
  
  const start = 0;
  const startTime = performance.now();
  
  const update = (currentTime) => {
    const progress = Math.min((currentTime - startTime) / props.duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    const current = start + (numTarget - start) * eased;
    
    if (props.decimals > 0) {
      animatedValue.value = current.toFixed(props.decimals) + props.suffix;
    } else {
      animatedValue.value = Math.round(current) + props.suffix;
    }
    
    if (progress < 1) {
      requestAnimationFrame(update);
    }
  };
  
  requestAnimationFrame(update);
};

onMounted(() => {
  animateNumber(props.targetValue);
});

watch(() => props.targetValue, (newVal) => {
  animateNumber(newVal);
});
</script>

<style scoped>
.number-animation {
  font-variant-numeric: tabular-nums;
}
</style>
