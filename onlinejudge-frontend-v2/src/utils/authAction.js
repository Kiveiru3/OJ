import { useUiStore } from '@/stores/useUiStore'

export async function requireLoginAction({ userStore, router, redirect, actionText = '执行此操作' }) {
  if (userStore?.isLoggedIn) {
    return true
  }

  const ui = useUiStore()
  const ok = await ui.confirm({
    title: '需要登录',
    message: `${actionText}需要先登录。\n登录后将自动返回当前页面。`,
    okText: '去登录',
    cancelText: '取消'
  })
  if (!ok) {
    return false
  }

  const target = redirect || `${window.location.pathname}${window.location.search}`
  router.push(`/login?redirect=${encodeURIComponent(target)}`)
  return false
}
