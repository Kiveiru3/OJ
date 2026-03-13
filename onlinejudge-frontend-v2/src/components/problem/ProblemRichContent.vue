<template>
  <article class="problem-rich" v-html="safeHtml" />
</template>

<script setup>
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import katex from 'katex'
import MarkdownIt from 'markdown-it'
import texmath from 'markdown-it-texmath'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

md.use(texmath, {
  engine: katex,
  delimiters: 'dollars',
  katexOptions: { throwOnError: false, output: 'html' }
})

const defaultImageRenderer = md.renderer.rules.image
md.renderer.rules.image = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  token.attrSet('loading', 'lazy')
  token.attrSet('referrerpolicy', 'no-referrer')
  token.attrSet('class', 'problem-image')
  if (defaultImageRenderer) {
    return defaultImageRenderer(tokens, idx, options, env, self)
  }
  return self.renderToken(tokens, idx, options)
}

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return self.renderToken(tokens, idx, options)
}

const safeHtml = computed(() => {
  const source = String(props.content || '').trim()
  if (!source) return '<p>暂无内容</p>'
  const raw = md.render(source)
  return DOMPurify.sanitize(raw)
})
</script>

<style scoped>
.problem-rich {
  color: #1f2937;
  font-size: 16px;
  line-height: 1.9;
}

.problem-rich :deep(h1),
.problem-rich :deep(h2),
.problem-rich :deep(h3) {
  margin: 1rem 0 0.5rem;
  font-weight: 700;
  color: #0f172a;
}

.problem-rich :deep(p),
.problem-rich :deep(ul),
.problem-rich :deep(ol),
.problem-rich :deep(blockquote) {
  margin: 0.6rem 0;
}

.problem-rich :deep(ul),
.problem-rich :deep(ol) {
  padding-left: 1.5rem;
}

.problem-rich :deep(code) {
  border-radius: 6px;
  background: #f1f5f9;
  padding: 0.1rem 0.35rem;
  font-size: 0.9em;
  color: #0f172a;
}

.problem-rich :deep(pre) {
  overflow-x: auto;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  padding: 0.85rem 1rem;
}

.problem-rich :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.problem-rich :deep(blockquote) {
  border-left: 3px solid #cbd5e1;
  padding-left: 0.75rem;
  color: #475569;
}

.problem-rich :deep(.problem-image) {
  display: block;
  max-width: 100%;
  border-radius: 10px;
  margin: 0.8rem 0;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.problem-rich :deep(.katex-display) {
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0.25rem 0;
}
</style>
