import test from 'node:test'
import assert from 'node:assert/strict'

import {
  inferProviderLabel,
  resolveAssistantErrorState,
  resolveRuntimeIndicator,
} from '../assistantPanelState.mjs'

test('infers the provider label from a DeepSeek model name', () => {
  assert.equal(inferProviderLabel('deepseek-v4-pro'), 'DeepSeek')
})

test('returns a thinking runtime indicator when the assistant is replying', () => {
  assert.deepEqual(
    resolveRuntimeIndicator({
      model: 'deepseek-v4-pro',
      submitting: true,
    }),
    {
      tone: 'active',
      title: '正在思考',
      detail: 'DeepSeek · deepseek-v4-pro',
    }
  )
})

test('maps configuration errors to a calm setup message', () => {
  assert.deepEqual(
    resolveAssistantErrorState('Request failed with status code 401'),
    {
      tone: 'warning',
      title: '助手暂时还没准备好',
      detail: '当前模型配置或 API Key 可能还未正确设置，请稍后再试。',
    }
  )
})

test('maps throttling errors to a retry suggestion', () => {
  assert.deepEqual(
    resolveAssistantErrorState('Request failed with status code 429'),
    {
      tone: 'warning',
      title: '现在有点忙',
      detail: '请求有点密集，我们稍等片刻再问一次会更顺畅。',
    }
  )
})

test('maps timeout style errors to a network-oriented explanation', () => {
  assert.deepEqual(
    resolveAssistantErrorState('Network timeout while streaming response'),
    {
      tone: 'danger',
      title: '这次连接没有接稳',
      detail: '网络或上游响应有点慢，重新发送一次通常就能恢复。',
    }
  )
})

test('falls back to a generic message for unknown errors', () => {
  assert.deepEqual(
    resolveAssistantErrorState('Something went wrong'),
    {
      tone: 'danger',
      title: '这次回答没有顺利完成',
      detail: '可以重新问我一次，我会继续结合当前页面内容帮你整理。',
    }
  )
})
