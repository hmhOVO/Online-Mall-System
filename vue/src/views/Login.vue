<template>
  <div class="auth-bg">
    <ParticleBackground class="particles"/>
    <div class="login-card" :class="{ 'card-hide': loading }">
      <div class="card-title">登 录</div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="login">
        <el-form-item prop="username">
          <el-input v-model="form.username" autocomplete="off" prefix-icon="User" :validate-event="false"
                    placeholder="请输入用户名" class="auth-input"/>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" show-password autocomplete="off" prefix-icon="Lock" :validate-event="false"
                    placeholder="请输入您的密码" class="auth-input"/>
        </el-form-item>
        <el-form-item class="remember-item">
          <el-checkbox v-model="form.rememberMe">记住我（7天内免登录）</el-checkbox>
        </el-form-item>
        <button type="button" class="auth-button" :disabled="loading" @click="login">登 陆</button>
      </el-form>
    </div>
    <div v-if="loading" class="rotating-plane"></div>
  </div>
</template>

<script setup>
import {ref} from "vue";
import {ElMessage} from "element-plus";
import Request from "@/utils/request.js";
import ParticleBackground from "@/components/ParticleBackground.vue";

const formRef = ref();
const loading = ref(false);
const form = ref({
  username: '',
  password: '',
  rememberMe: false
});

const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, message: '用户名至少3位', trigger: 'blur'},
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'}
  ],
};

const login = async () => {
  formRef.value.validate(async valid => {
    if (!valid || loading.value) return;
    loading.value = true;
    try {
      const res = await Request.post('/auth/login', form.value);
      const user = res.data || {};
      localStorage.removeItem("code_user");
      localStorage.setItem("code_user", JSON.stringify(user));
      ElMessage.success('登录成功');
      setTimeout(() => {
        window.location.href = '/';
      }, 600);
    } catch (error) {
      loading.value = false;
      console.error('登录失败', error);
    }
  });
};
</script>

<style scoped>
.auth-bg {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  overflow: hidden;
  background-image: url("@/assets/imgs/login-bg.jpg");
  background-position: 50% 50%;
  background-size: cover;
  background-repeat: no-repeat;
}

.particles {
  z-index: 1;
}

.login-card {
  position: relative;
  z-index: 2;
  width: 430px;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 0 25px rgba(0, 0, 0, 0.35);
  padding: 64px 50px 46px;
  transition: all 0.6s ease;
}

.login-card.card-hide {
  animation: login-small 0.8s forwards;
}

.card-title {
  margin-bottom: 52px;
  color: #333333;
  font-size: 30px;
  font-weight: 500;
  letter-spacing: 3px;
}

.auth-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-bottom: 1px solid #d0d0d0;
  border-radius: 0;
  padding: 8px 0;
  transition: border-color 0.3s;
}

.auth-input :deep(.el-input__wrapper.is-focus),
.auth-input :deep(.el-input__wrapper:hover) {
  box-shadow: none;
  border-bottom-color: #1E90FF;
}

.auth-input :deep(.el-input__prefix) {
  color: #909399;
  font-size: 20px;
  margin-right: 10px;
}

.auth-input :deep(.el-input__inner) {
  color: #333333;
  font-size: 16px;
}

:deep(.el-form-item) {
  margin-bottom: 34px;
}

:deep(.el-form-item.is-error .el-input__wrapper) {
  border-bottom-color: var(--el-color-danger);
}

.remember-item {
  margin-top: -6px;
  margin-bottom: 24px;
}

.auth-button {
  cursor: pointer;
  width: 100%;
  height: 52px;
  line-height: 1;
  border: none;
  border-radius: 8px;
  background-color: #1E90FF;
  color: #ffffff;
  font-size: 18px;
  letter-spacing: 3px;
  transition: background-color 0.3s, opacity 0.3s;
}

.auth-button:hover {
  background-color: #1877d2;
}

.auth-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-footer {
  margin-top: 24px;
  text-align: right;
  font-size: 14px;
  color: #909399;
}

.auth-footer a {
  color: #1E90FF;
  cursor: pointer;
}

@keyframes login-small {
  0% {
    transform: scale(1);
  }
  100% {
    transform: scale(0.2);
    opacity: 0;
  }
}

.rotating-plane {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 3;
  width: 80px;
  height: 80px;
  margin-top: -40px;
  margin-left: -40px;
  background-color: #ffffff;
  animation: sk-rotating-plane 1.2s infinite ease-in-out;
}

@keyframes sk-rotating-plane {
  0% {
    transform: perspective(120px) rotateX(0deg) rotateY(0deg);
  }
  50% {
    transform: perspective(120px) rotateX(-180.1deg) rotateY(0deg);
  }
  100% {
    transform: perspective(120px) rotateX(-180deg) rotateY(-179.9deg);
  }
}
</style>
