<template>
  <div class="system-config">
    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- AI配置 -->
      <el-tab-pane label="AI配置" name="ai">
        <!-- API密钥 -->
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">API 密钥</span>
              <span class="card-desc">配置 AI 服务的访问密钥</span>
            </div>
          </template>
          <el-form label-width="160px" label-position="right">
            <el-form-item label="DeepSeek API Key">
              <div class="masked-input">
                <span class="masked-text">{{ maskApiKey(form.deepseek_api_key) }}</span>
                <el-button type="primary" link @click="openKeyDialog">修改</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 模型配置 -->
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">模型配置</span>
              <span class="card-desc">选择使用的 AI 模型</span>
            </div>
          </template>
          <el-form label-width="160px" label-position="right">
            <el-form-item label="DeepSeek 文本模型">
              <el-select v-model="form.deepseek_model" style="width: 300px">
                <el-option label="deepseek-v4-flash（推荐·高性价比）" value="deepseek-v4-flash" />
                <el-option label="deepseek-v4-pro（复杂推理）" value="deepseek-v4-pro" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 接口配置 -->
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">接口配置</span>
              <span class="card-desc">API 地址与超时设置</span>
            </div>
          </template>
          <el-form label-width="160px" label-position="right">
            <el-form-item label="DeepSeek Base URL">
              <el-input v-model="form.deepseek_base_url" style="width: 400px" placeholder="https://api.deepseek.com" />
            </el-form-item>
            <el-form-item label="DeepSeek 超时(秒)">
              <el-input-number v-model="form.deepseek_timeout" :min="10" :max="300" :step="10" />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 智能设置 -->
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">智能设置</span>
              <span class="card-desc">AI 功能的相关开关</span>
            </div>
          </template>
          <el-form label-width="160px" label-position="right">
            <el-form-item label="SSE 流式输出">
              <el-switch
                v-model="form.sse_enabled"
                active-text="开启"
                inactive-text="关闭"
                :active-value="1"
                :inactive-value="0"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 智能限流 -->
      <el-tab-pane label="智能限流" name="rate">
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">限流配置</span>
              <span class="card-desc">限制 AI 功能的调用频率，防止滥用</span>
            </div>
          </template>
          <el-form label-width="200px" label-position="right">
            <el-form-item label="任务拆解限流(次/分钟)">
              <el-input-number v-model="form.ai_rate_task_split" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="风险分析限流(次/分钟)">
              <el-input-number v-model="form.ai_rate_risk_analyze" :min="1" :max="100" />
            </el-form-item>
          </el-form>
          <el-alert
            title="提示：修改限流阈值后约 10 分钟生效（Guava 缓存过期后重新加载）。"
            type="info"
            :closable="false"
            show-icon
            style="margin-top: 16px"
          />
        </el-card>
      </el-tab-pane>

      <!-- 任务设置 -->
      <el-tab-pane label="任务设置" name="task">
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">任务配置</span>
              <span class="card-desc">与任务相关的系统参数</span>
            </div>
          </template>
          <el-form label-width="200px" label-position="right">
            <el-form-item label="逾期预警阈值(天)">
              <el-input-number v-model="form.overdue_warning_days" :min="1" :max="30" />
              <span class="form-tip">距离截止日期不足该天数时，任务标记为预警状态</span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 底部操作栏 -->
    <div class="footer-actions">
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
    </div>

    <!-- API Key 修改弹窗 -->
    <el-dialog v-model="keyDialogVisible" title="修改 DeepSeek API Key" width="500px">
      <el-form label-width="140px">
        <el-form-item label="API Key">
          <el-input v-model="newKeyValue" type="password" show-password placeholder="请输入新的 API Key" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="keyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmKeyChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'

const activeTab = ref('ai')
const saving = ref(false)
const loading = ref(false)

const form = reactive({
  deepseek_api_key: '',
  deepseek_base_url: '',
  deepseek_model: '',
  deepseek_timeout: 60,
  ai_rate_task_split: 10,
  ai_rate_risk_analyze: 10,
  overdue_warning_days: 3,
  sse_enabled: 1
})

const originalForm = {}

const keyDialogVisible = ref(false)
const newKeyValue = ref('')

function maskApiKey(key) {
  if (!key) return '未配置'
  if (key.length <= 8) return '****'
  return key.slice(0, 4) + '****' + key.slice(-4)
}

function openKeyDialog() {
  newKeyValue.value = ''
  keyDialogVisible.value = true
}

function confirmKeyChange() {
  if (!newKeyValue.value.trim()) {
    ElMessage.warning('请输入 API Key')
    return
  }
  form.deepseek_api_key = newKeyValue.value.trim()
  keyDialogVisible.value = false
  ElMessage.success('已修改，点击底部"保存配置"后生效')
}

async function loadConfig() {
  loading.value = true
  try {
    const res = await request.get('/admin/config')
    const list = res.data || []
    list.forEach(item => {
      const key = item.configKey.replace(/\./g, '_')
      if (form.hasOwnProperty(key)) {
        if (key === 'deepseek_timeout' ||
            key === 'ai_rate_task_split' || key === 'ai_rate_risk_analyze' ||
            key === 'overdue_warning_days' || key === 'sse_enabled') {
          form[key] = Number(item.configValue) || 0
        } else {
          form[key] = item.configValue || ''
        }
        originalForm[key] = form[key]
      }
    })
  } catch (e) {
    console.error('加载配置失败', e)
  } finally {
    loading.value = false
  }
}

function handleReset() {
  Object.keys(originalForm).forEach(key => {
    form[key] = originalForm[key]
  })
  ElMessage.info('已重置为上次保存的配置')
}

async function handleSave() {
  saving.value = true
  try {
    const params = {
      'deepseek.api_key': form.deepseek_api_key,
      'deepseek.base_url': form.deepseek_base_url,
      'deepseek.model': form.deepseek_model,
      'deepseek.timeout': String(form.deepseek_timeout),
      'ai.rate.task_split': String(form.ai_rate_task_split),
      'ai.rate.risk_analyze': String(form.ai_rate_risk_analyze),
      'overdue.warning.days': String(form.overdue_warning_days),
      'sse.enabled': String(form.sse_enabled)
    }
    const res = await request.put('/admin/config/batch', params)
    ElMessage.success('配置保存成功')
    Object.keys(params).forEach(key => {
      const formKey = key.replace(/\./g, '_')
      if (form.hasOwnProperty(formKey)) {
        originalForm[formKey] = form[formKey]
      }
    })
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.system-config {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}

.config-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
  background: #fff;
  padding: 0 20px;
  border-radius: 8px;
}

.config-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  font-size: 13px;
  color: #909399;
}

.masked-input {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 32px;
}

.masked-text {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  color: #606266;
  background: #f5f7fa;
  padding: 4px 12px;
  border-radius: 4px;
  min-width: 200px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.footer-actions {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 16px 24px;
  margin: 20px -20px -20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
  z-index: 10;
}
</style>
