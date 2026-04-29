<template>
  <div class="editor-shell overflow-hidden rounded-[22px] border border-line bg-slate-950 shadow-[0_28px_80px_rgba(15,23,42,0.18)]">
    <div class="flex items-center justify-between border-b border-white/10 bg-white/[0.04] px-4 py-3">
      <div class="flex items-center gap-2">
        <span class="inline-flex items-center rounded-full border border-sky-400/30 bg-sky-400/10 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-sky-200">
          {{ languageLabel }}
        </span>
        <button
          type="button"
          :class="[
            'inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-medium transition',
            autocompleteEnabled
              ? 'border-emerald-400/20 bg-emerald-400/10 text-emerald-200 hover:border-emerald-300/40 hover:bg-emerald-400/15'
              : 'border-slate-500/30 bg-slate-500/10 text-slate-300 hover:border-slate-400/40 hover:bg-slate-500/15'
          ]"
          @click="emit('update:autocompleteEnabled', !autocompleteEnabled)"
        >
          自动补全 {{ autocompleteEnabled ? '开' : '关' }}
        </button>
      </div>
      <div class="text-[11px] text-slate-400">
        {{ cursorLabel }} · {{ autocompleteEnabled ? 'Ctrl + Space' : '已关闭' }}
      </div>
    </div>

    <div ref="editorRoot" class="editor-root" :style="{ height }" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { basicSetup } from 'codemirror'
import {
  acceptCompletion,
  autocompletion,
  completeAnyWord,
  completeFromList,
  completionKeymap,
  snippetCompletion,
  startCompletion
} from '@codemirror/autocomplete'
import { indentWithTab } from '@codemirror/commands'
import { cpp } from '@codemirror/lang-cpp'
import { java } from '@codemirror/lang-java'
import { python } from '@codemirror/lang-python'
import { Compartment, EditorState } from '@codemirror/state'
import { EditorView, keymap, placeholder } from '@codemirror/view'
import { oneDark } from '@codemirror/theme-one-dark'

const props = defineProps({
  modelValue: { type: String, default: '' },
  language: { type: String, default: 'JAVA' },
  height: { type: String, default: '500px' },
  placeholder: { type: String, default: '请输入代码...' },
  autocompleteEnabled: { type: Boolean, default: true },
  readOnly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'update:autocompleteEnabled'])

const editorRoot = ref(null)
const cursorLabel = ref('Ln 1, Col 1')

const languageCompartment = new Compartment()
const autocompleteCompartment = new Compartment()
const editableCompartment = new Compartment()
const placeholderCompartment = new Compartment()

let view = null

const LANGUAGE_LABELS = {
  JAVA: 'Java',
  CPP: 'C++',
  PYTHON: 'Python'
}

const languageLabel = computed(() => LANGUAGE_LABELS[props.language] || 'Code')

const JAVA_COMPLETIONS = [
  ...keywordCompletions([
    'public',
    'private',
    'protected',
    'static',
    'final',
    'class',
    'void',
    'int',
    'long',
    'double',
    'boolean',
    'String',
    'return',
    'if',
    'else',
    'for',
    'while',
    'break',
    'continue',
    'Arrays',
    'Collections',
    'List',
    'Map',
    'Set',
    'Queue',
    'Deque'
  ]),
  snippetCompletion('if (${condition}) {\n\t${}\n}', {
    label: 'if',
    type: 'snippet',
    detail: 'if 判断'
  }),
  snippetCompletion('for (int ${i = 0}; ${i} < ${n}; ${i}++) {\n\t${}\n}', {
    label: 'fori',
    type: 'snippet',
    detail: 'for 循环'
  }),
  snippetCompletion('while (${condition}) {\n\t${}\n}', {
    label: 'while',
    type: 'snippet',
    detail: 'while 循环'
  }),
  snippetCompletion('Scanner in = new Scanner(System.in);\n${}', {
    label: 'scanner',
    type: 'snippet',
    detail: 'Scanner 输入'
  }),
  snippetCompletion(
    'BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\nStringTokenizer st = new StringTokenizer(br.readLine());\n${}',
    {
      label: 'fastio',
      type: 'snippet',
      detail: '快速输入'
    }
  ),
  snippetCompletion('Arrays.sort(${array});', {
    label: 'sort',
    type: 'function',
    detail: '数组排序'
  }),
  snippetCompletion('Queue<Integer> queue = new ArrayDeque<>();\n${}', {
    label: 'queue',
    type: 'snippet',
    detail: '队列模板'
  }),
  snippetCompletion('Deque<Integer> stack = new ArrayDeque<>();\n${}', {
    label: 'stack',
    type: 'snippet',
    detail: '栈模板'
  })
]

const CPP_COMPLETIONS = [
  ...keywordCompletions([
    'int',
    'long long',
    'double',
    'string',
    'vector',
    'pair',
    'queue',
    'stack',
    'deque',
    'set',
    'map',
    'unordered_map',
    'unordered_set',
    'priority_queue',
    'return',
    'if',
    'else',
    'for',
    'while',
    'break',
    'continue',
    'sort',
    'lower_bound',
    'upper_bound'
  ]),
  snippetCompletion('if (${condition}) {\n\t${}\n}', {
    label: 'if',
    type: 'snippet',
    detail: 'if 判断'
  }),
  snippetCompletion('for (int ${i = 0}; ${i} < ${n}; ${i}++) {\n\t${}\n}', {
    label: 'fori',
    type: 'snippet',
    detail: 'for 循环'
  }),
  snippetCompletion('while (${condition}) {\n\t${}\n}', {
    label: 'while',
    type: 'snippet',
    detail: 'while 循环'
  }),
  snippetCompletion('ios::sync_with_stdio(false);\ncin.tie(nullptr);\n${}', {
    label: 'fastio',
    type: 'snippet',
    detail: '快速输入'
  }),
  snippetCompletion('vector<int> ${nums};', {
    label: 'vector',
    type: 'snippet',
    detail: 'vector 容器'
  }),
  snippetCompletion('queue<int> q;\n${}', {
    label: 'queue',
    type: 'snippet',
    detail: '队列模板'
  }),
  snippetCompletion('stack<int> st;\n${}', {
    label: 'stack',
    type: 'snippet',
    detail: '栈模板'
  }),
  snippetCompletion('sort(${nums}.begin(), ${nums}.end());', {
    label: 'sort',
    type: 'function',
    detail: '排序'
  }),
  snippetCompletion('auto ${name} = [&](int ${node}) {\n\t${}\n};', {
    label: 'lambda',
    type: 'snippet',
    detail: 'lambda 模板'
  })
]

const PYTHON_COMPLETIONS = [
  ...keywordCompletions([
    'def',
    'return',
    'if',
    'elif',
    'else',
    'for',
    'while',
    'break',
    'continue',
    'True',
    'False',
    'None',
    'list',
    'dict',
    'set',
    'tuple',
    'enumerate',
    'range',
    'len',
    'sorted'
  ]),
  snippetCompletion('if ${condition}:\n\t${}', {
    label: 'if',
    type: 'snippet',
    detail: 'if 判断'
  }),
  snippetCompletion('for ${i} in range(${n}):\n\t${}', {
    label: 'fori',
    type: 'snippet',
    detail: 'for 循环'
  }),
  snippetCompletion('while ${condition}:\n\t${}', {
    label: 'while',
    type: 'snippet',
    detail: 'while 循环'
  }),
  snippetCompletion('def ${name}(${params}):\n\t${}', {
    label: 'def',
    type: 'snippet',
    detail: '函数定义'
  }),
  snippetCompletion('import sys\ninput = sys.stdin.readline\n${}', {
    label: 'fastio',
    type: 'snippet',
    detail: '快速输入'
  }),
  snippetCompletion('from collections import deque\n${}', {
    label: 'deque',
    type: 'snippet',
    detail: '双端队列'
  }),
  snippetCompletion('from heapq import heappush, heappop\n${}', {
    label: 'heapq',
    type: 'snippet',
    detail: '堆模板'
  }),
  snippetCompletion('${nums}.sort()', {
    label: 'sort',
    type: 'function',
    detail: '原地排序'
  })
]

const AUTOCOMPLETE_SOURCES = {
  JAVA: completeFromList(JAVA_COMPLETIONS),
  CPP: completeFromList(CPP_COMPLETIONS),
  PYTHON: completeFromList(PYTHON_COMPLETIONS)
}

function keywordCompletions(words) {
  return words.map((label) => ({
    label,
    type: 'keyword'
  }))
}

function getLanguageExtension(language) {
  if (language === 'CPP') return cpp()
  if (language === 'PYTHON') return python()
  return java()
}

function getAutocompleteExtension(language, enabled) {
  if (!enabled) return []
  return autocompletion({
    activateOnTyping: true,
    maxRenderedOptions: 14,
    override: [
      completeAnyWord,
      AUTOCOMPLETE_SOURCES[language] || AUTOCOMPLETE_SOURCES.JAVA
    ]
  })
}

function updateCursor(state) {
  const head = state.selection.main.head
  const line = state.doc.lineAt(head)
  cursorLabel.value = `Ln ${line.number}, Col ${head - line.from + 1}`
}

function syncEditorDoc(nextValue) {
  if (!view) return
  const current = view.state.doc.toString()
  if (nextValue === current) return
  view.dispatch({
    changes: {
      from: 0,
      to: current.length,
      insert: nextValue
    }
  })
}

function createEditor() {
  if (!editorRoot.value) return

  view = new EditorView({
    parent: editorRoot.value,
    state: EditorState.create({
      doc: props.modelValue || '',
      extensions: [
        basicSetup,
        oneDark,
        EditorState.tabSize.of(2),
        languageCompartment.of(getLanguageExtension(props.language)),
        autocompleteCompartment.of(getAutocompleteExtension(props.language, props.autocompleteEnabled)),
        editableCompartment.of(EditorView.editable.of(!props.readOnly)),
        placeholderCompartment.of(placeholder(props.placeholder)),
        keymap.of([
          { key: 'Ctrl-Space', run: startCompletion },
          { key: 'Tab', run: acceptCompletion },
          indentWithTab,
          ...completionKeymap
        ]),
        EditorView.theme({
          '&': {
            height: '100%',
            backgroundColor: 'transparent',
            color: '#e2e8f0',
            fontSize: '14px'
          },
          '&.cm-focused': {
            outline: 'none'
          },
          '.cm-scroller': {
            fontFamily: '"JetBrains Mono", "Fira Code", "Cascadia Code", Consolas, monospace',
            lineHeight: '1.75'
          },
          '.cm-content': {
            padding: '16px 18px 28px'
          },
          '.cm-line': {
            padding: '0 0 0 8px'
          },
          '.cm-gutters': {
            minWidth: '52px',
            border: 'none',
            backgroundColor: 'transparent',
            color: '#64748b',
            padding: '16px 0 24px 8px'
          },
          '.cm-activeLine': {
            backgroundColor: 'rgba(148, 163, 184, 0.08)'
          },
          '.cm-activeLineGutter': {
            backgroundColor: 'transparent',
            color: '#cbd5e1'
          },
          '.cm-selectionBackground, ::selection': {
            backgroundColor: 'rgba(56, 189, 248, 0.25) !important'
          },
          '.cm-cursor, .cm-dropCursor': {
            borderLeftColor: '#f8fafc'
          },
          '.cm-tooltip': {
            border: '1px solid rgba(148, 163, 184, 0.2)',
            borderRadius: '16px',
            backgroundColor: '#0f172a',
            color: '#e2e8f0',
            boxShadow: '0 24px 80px rgba(2, 6, 23, 0.42)',
            overflow: 'hidden'
          },
          '.cm-tooltip-autocomplete ul': {
            padding: '8px'
          },
          '.cm-tooltip-autocomplete ul li': {
            borderRadius: '10px',
            padding: '8px 10px'
          },
          '.cm-tooltip-autocomplete ul li[aria-selected]': {
            backgroundColor: 'rgba(56, 189, 248, 0.18)',
            color: '#f8fafc'
          },
          '.cm-completionIcon': {
            opacity: 0.8
          },
          '.cm-panels': {
            backgroundColor: '#0f172a',
            color: '#cbd5e1'
          }
        }),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            emit('update:modelValue', update.state.doc.toString())
          }
          if (update.docChanged || update.selectionSet) {
            updateCursor(update.state)
          }
        })
      ]
    })
  })

  updateCursor(view.state)
}

watch(
  () => props.modelValue,
  (nextValue) => {
    syncEditorDoc(nextValue || '')
  }
)

watch(
  () => [props.language, props.autocompleteEnabled],
  ([nextLanguage, nextAutocompleteEnabled]) => {
    if (!view) return
    view.dispatch({
      effects: [
        languageCompartment.reconfigure(getLanguageExtension(nextLanguage)),
        autocompleteCompartment.reconfigure(getAutocompleteExtension(nextLanguage, nextAutocompleteEnabled))
      ]
    })
  }
)

watch(
  () => props.readOnly,
  (nextReadOnly) => {
    if (!view) return
    view.dispatch({
      effects: editableCompartment.reconfigure(EditorView.editable.of(!nextReadOnly))
    })
  }
)

watch(
  () => props.placeholder,
  (nextPlaceholder) => {
    if (!view) return
    view.dispatch({
      effects: placeholderCompartment.reconfigure(placeholder(nextPlaceholder))
    })
  }
)

onMounted(() => {
  createEditor()
})

onBeforeUnmount(() => {
  if (view) {
    view.destroy()
    view = null
  }
})
</script>

<style scoped>
.editor-root {
  min-height: 320px;
}

:deep(.cm-editor) {
  height: 100%;
}
</style>
