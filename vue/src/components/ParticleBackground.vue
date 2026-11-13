<template>
  <canvas ref="canvasRef" class="particle-canvas"></canvas>
</template>

<script setup>
import {onMounted, onUnmounted, ref} from 'vue';

const canvasRef = ref(null);

const CONFIG = {
  density: 9000,      // 每多少像素面积一个粒子
  maxCount: 70,
  maxTotal: 260,      // 点击生成粒子的总量上限，超出后移除最早的
  clickParticles: 4,  // 每次点击生成的粒子数
  size: 3,
  speed: 0.6,
  linkDistance: 150,
  mouseRadius: 160
};

let ctx = null;
let rafId = null;
let width = 0;
let height = 0;
let particles = [];
const mouse = {x: null, y: null};

const createParticles = () => {
  const count = Math.min(CONFIG.maxCount, Math.floor((width * height) / CONFIG.density));
  particles = Array.from({length: count}, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * CONFIG.speed,
    vy: (Math.random() - 0.5) * CONFIG.speed,
    r: Math.random() * CONFIG.size + 1
  }));
};

const resize = () => {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const dpr = window.devicePixelRatio || 1;
  width = canvas.clientWidth;
  height = canvas.clientHeight;
  canvas.width = width * dpr;
  canvas.height = height * dpr;
  ctx = canvas.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  createParticles();
};

const draw = () => {
  if (!ctx) return;
  ctx.clearRect(0, 0, width, height);

  particles.forEach(p => {
    p.x += p.vx;
    p.y += p.vy;
    if (p.x < 0 || p.x > width) p.vx *= -1;
    if (p.y < 0 || p.y > height) p.vy *= -1;
    p.x = Math.max(0, Math.min(width, p.x));
    p.y = Math.max(0, Math.min(height, p.y));
  });

  for (let i = 0; i < particles.length; i++) {
    const a = particles[i];

    ctx.beginPath();
    ctx.arc(a.x, a.y, a.r, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(255, 255, 255, 0.75)';
    ctx.fill();

    for (let j = i + 1; j < particles.length; j++) {
      const b = particles[j];
      const dist = Math.hypot(a.x - b.x, a.y - b.y);
      if (dist < CONFIG.linkDistance) {
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.lineTo(b.x, b.y);
        ctx.strokeStyle = `rgba(255, 255, 255, ${0.6 * (1 - dist / CONFIG.linkDistance)})`;
        ctx.lineWidth = 1;
        ctx.stroke();
      }
    }

    if (mouse.x !== null) {
      const dist = Math.hypot(a.x - mouse.x, a.y - mouse.y);
      if (dist < CONFIG.mouseRadius) {
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.lineTo(mouse.x, mouse.y);
        ctx.strokeStyle = `rgba(255, 255, 255, ${1 - dist / CONFIG.mouseRadius})`;
        ctx.lineWidth = 1;
        ctx.stroke();
      }
    }
  }

  rafId = requestAnimationFrame(draw);
};

const handleMouseMove = (e) => {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  mouse.x = e.clientX - rect.left;
  mouse.y = e.clientY - rect.top;
};

const handleMouseLeave = () => {
  mouse.x = null;
  mouse.y = null;
};

const handleClick = (e) => {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  for (let i = 0; i < CONFIG.clickParticles; i++) {
    particles.push({
      x,
      y,
      vx: (Math.random() - 0.5) * 3,
      vy: (Math.random() - 0.5) * 3,
      r: Math.random() * 2 + 2
    });
  }
  while (particles.length > CONFIG.maxTotal) {
    particles.shift();
  }
};

onMounted(() => {
  resize();
  draw();
  window.addEventListener('resize', resize);
  window.addEventListener('mousemove', handleMouseMove);
  window.addEventListener('mouseleave', handleMouseLeave);
  canvasRef.value.addEventListener('click', handleClick);
});

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId);
  window.removeEventListener('resize', resize);
  window.removeEventListener('mousemove', handleMouseMove);
  window.removeEventListener('mouseleave', handleMouseLeave);
  if (canvasRef.value) canvasRef.value.removeEventListener('click', handleClick);
});
</script>

<style scoped>
.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: block;
}
</style>
