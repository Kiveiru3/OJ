function encodeSvg(svg) {
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

function hashString(value) {
  const text = String(value || 'user')
  let hash = 0
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) >>> 0
  }
  return hash
}

function pickPalette(seed) {
  const palettes = [
    ['#0ea5e9', '#2563eb'],
    ['#22c55e', '#14b8a6'],
    ['#f59e0b', '#f97316'],
    ['#a855f7', '#ec4899'],
    ['#64748b', '#334155'],
    ['#ef4444', '#f97316']
  ]
  return palettes[seed % palettes.length]
}

export function getDisplayName(user = {}) {
  return user?.nickname || user?.username || (user?.userId ? `用户#${user.userId}` : '用户')
}

export function getAvatarUrl(user = {}) {
  const avatar = typeof user?.avatar === 'string' ? user.avatar.trim() : ''
  if (avatar) return avatar

  const name = getDisplayName(user)
  const firstChar = name.slice(0, 1).toUpperCase() || 'U'
  const hash = hashString(`${user?.id || user?.userId || ''}:${name}`)
  const [start, end] = pickPalette(hash)
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">
  <defs>
    <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${start}" />
      <stop offset="100%" stop-color="${end}" />
    </linearGradient>
  </defs>
  <rect width="96" height="96" rx="24" fill="url(#g)" />
  <text x="48" y="56" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-size="42" fill="white" font-weight="700">${firstChar}</text>
</svg>`
  return encodeSvg(svg)
}
