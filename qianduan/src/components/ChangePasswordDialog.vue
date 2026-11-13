<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    width="480px"
    class="gs-dialog password-dialog"
    align-center
  >
    <template #header>
      <div class="dialog-header">
        <el-icon><Lock /></el-icon>
        <span>修改密码</span>
      </div>
    </template>

    <!-- 顶部安全提示条 -->
    <div class="security-banner">
      <el-icon><Warning /></el-icon>
      <span>定期更换密码可有效保护账号安全</span>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="password-form">
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          placeholder="请输入当前密码"
        />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="6-20 位，建议字母+数字+符号"
          @input="calcStrength"
        />
        <!-- 强度条 -->
        <div v-if="form.newPassword" class="strength-bar">
          <div class="strength-track">
            <div
              class="strength-fill"
              :class="`level-${strength.level}`"
              :style="{ width: strength.percent + '%' }"
            ></div>
          </div>
          <div class="strength-label" :class="`level-${strength.level}`">
            强度：{{ strength.label }}
          </div>
        </div>
        <!-- 规则提示 -->
        <ul class="rule-tips">
          <li :class="{ met: hasMinLen }">
            <el-icon><CircleCheck v-if="hasMinLen" /><CircleClose v-else /></el-icon>
            <span>至少 6 位字符</span>
          </li>
          <li :class="{ met: hasLetter }">
            <el-icon><CircleCheck v-if="hasLetter" /><CircleClose v-else /></el-icon>
            <span>包含字母</span>
          </li>
          <li :class="{ met: hasNumber }">
            <el-icon><CircleCheck v-if="hasNumber" /><CircleClose v-else /></el-icon>
            <span>包含数字</span>
          </li>
          <li :class="{ met: hasSpecial }">
            <el-icon><CircleCheck v-if="hasSpecial" /><CircleClose v-else /></el-icon>
            <span>包含特殊符号</span>
          </li>
        </ul>
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
        />
        <div v-if="form.confirmPassword" class="match-tip" :class="{ ok: isMatch }">
          <el-icon><CircleCheck v-if="isMatch" /><CircleClose v-else /></el-icon>
          <span>{{ isMatch ? '两次密码一致' : '两次密码不一致' }}</span>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!canSubmit" @click="handleSubmit">
        确认修改
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Warning, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import api from '@/utils/api'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const formRef = ref(null)
const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 密码强度规则检测
const hasMinLen = computed(() => form.newPassword.length >= 6)
const hasLetter = computed(() => /[a-zA-Z]/.test(form.newPassword))
const hasNumber = computed(() => /\d/.test(form.newPassword))
const hasSpecial = computed(() => /[!@#$%^&*(),.?":{}|<>]/.test(form.newPassword))
const isMatch = computed(() => form.confirmPassword && form.confirmPassword === form.newPassword)
const canSubmit = computed(() => form.oldPassword && form.newPassword && isMatch.value && hasMinLen.value)

const strength = reactive({ level: 0, label: '弱', percent: 0 })

function calcStrength() {
  let score = 0
  if (hasMinLen.value) score += 25
  if (hasLetter.value) score += 25
  if (hasNumber.value) score += 25
  if (hasSpecial.value) score += 25
  if (form.newPassword.length >= 12) score = Math.min(100, score + 10)
  strength.percent = score
  if (score <= 25) { strength.level = 1; strength.label = '弱' }
  else if (score <= 50) { strength.level = 2; strength.label = '一般' }
  else if (score <= 75) { strength.level = 3; strength.label = '良好' }
  else { strength.level = 4; strength.label = '强' }
}

const validateNew = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入新密码'))
  if (value.length < 6 || value.length > 20) return callback(new Error('密码长度需 6-20 位'))
  if (value === form.oldPassword) return callback(new Error('新密码不能与当前密码相同'))
  callback()
}

const validateConfirm = (rule, value, callback) => {
  if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNew, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

watch(() => props.modelValue, (val) => {
  if (val) {
    Object.assign(form, { oldPassword: '', newPassword: '', confirmPassword: '' })
    strength.level = 0
    strength.percent = 0
    formRef.value?.clearValidate()
  }
})

function handleClose() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await api.put('/user/password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    handleClose()
  } catch (e) {
    // 响应拦截器已统一 ElMessage
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}

.security-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(90deg, rgba(91, 60, 196, 0.12), rgba(58, 110, 224, 0.08));
  border-left: 3px solid #5b3cc4;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 13px;
  color: #a8a8d4;

  .el-icon {
    color: #fbbf24;
    font-size: 16px;
  }
}

.password-form {
  :deep(.el-form-item__label) {
    color: #a8a8d4;
    font-weight: 500;
  }
}

// 强度条
.strength-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.strength-track {
  flex: 1;
  height: 4px;
  background: rgba(99, 102, 241, 0.15);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s ease;

  &.level-1 { background: linear-gradient(90deg, #ef4444, #f87171); }
  &.level-2 { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
  &.level-3 { background: linear-gradient(90deg, #5b3cc4, #818cf8); }
  &.level-4 { background: linear-gradient(90deg, #10b981, #22c55e); }
}

.strength-label {
  font-size: 12px;
  min-width: 60px;
  text-align: right;

  &.level-1 { color: #f87171; }
  &.level-2 { color: #fbbf24; }
  &.level-3 { color: #818cf8; }
  &.level-4 { color: #22c55e; }
}

// 规则提示
.rule-tips {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 16px;

  li {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #71717a;
    transition: color 0.25s ease;

    .el-icon { font-size: 13px; }

    &.met {
      color: #22c55e;
    }
  }
}

.match-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #f87171;

  .el-icon { font-size: 13px; }

  &.ok {
    color: #22c55e;
  }
}
</style>
