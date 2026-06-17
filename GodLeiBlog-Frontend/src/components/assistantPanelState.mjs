const PROVIDER_RULES = [
  { match: /deepseek/i, label: 'DeepSeek' },
  { match: /qwen|qwq|tongyi/i, label: 'Qwen' },
  { match: /kimi|moonshot/i, label: 'Kimi' },
  { match: /gpt|o1|o3|o4|openai/i, label: 'OpenAI' },
  { match: /openrouter/i, label: 'OpenRouter' },
  { match: /siliconflow/i, label: 'SiliconFlow' },
  { match: /glm|zhipu/i, label: 'GLM' },
  { match: /claude|anthropic/i, label: 'Claude' },
]

export function inferProviderLabel(model = '', providerLabel = '') {
  const explicit = String(providerLabel || '').trim()
  if (explicit) return explicit

  const normalizedModel = String(model || '').trim()
  if (!normalizedModel) return 'AI'

  const matchedRule = PROVIDER_RULES.find((rule) => rule.match.test(normalizedModel))
  return matchedRule?.label || 'AI'
}

export function resolveRuntimeIndicator({ model = '', providerLabel = '', submitting = false } = {}) {
  const resolvedProviderLabel = inferProviderLabel(model, providerLabel)
  const modelLine = model ? `${resolvedProviderLabel} · ${model}` : `${resolvedProviderLabel} · 智能对话`

  if (submitting) {
    return {
      tone: 'active',
      title: '正在思考',
      detail: modelLine,
    }
  }

  return {
    tone: 'ready',
    title: model ? '当前对话模型' : '已准备好回应',
    detail: modelLine,
  }
}

export function resolveAssistantErrorState(message = '') {
  const normalized = String(message || '').toLowerCase()

  if (
    /401|403|api key|apikey|unauthorized|forbidden|未配置|未启用|base url|模型|model/.test(normalized)
  ) {
    return {
      tone: 'warning',
      title: '助手暂时还没准备好',
      detail: '当前模型配置或 API Key 可能还未正确设置，请稍后再试。',
    }
  }

  if (/429|too many requests|rate limit|频繁|忙/.test(normalized)) {
    return {
      tone: 'warning',
      title: '现在有点忙',
      detail: '请求有点密集，我们稍等片刻再问一次会更顺畅。',
    }
  }

  if (/timeout|timed out|network|fetch|stream|连接|中断|超时/.test(normalized)) {
    return {
      tone: 'danger',
      title: '这次连接没有接稳',
      detail: '网络或上游响应有点慢，重新发送一次通常就能恢复。',
    }
  }

  return {
    tone: 'danger',
    title: '这次回答没有顺利完成',
    detail: '可以重新问我一次，我会继续结合当前页面内容帮你整理。',
  }
}
