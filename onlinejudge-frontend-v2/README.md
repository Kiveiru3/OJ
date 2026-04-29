# OnlineJudge Frontend V2

> Vue 技术栈重构版前端（不改后端）

## 技术栈
- Vue 3 + Vite
- Vue Router + Pinia
- Tailwind CSS
- shadcn-vue（Vue 版组件体系）
- Axios

## 设计方向
- 企业级高级灰
- 8px 圆角 + 柔和阴影 + 卡片式布局
- 骨架屏、悬浮动效、按钮过渡
- 页面命名改为：`首页指挥舱 / 题库中心 / 赛事中枢 / 交流广场 / 代码工坊 / 个人中心`

## 本地运行
```bash
npm install
npm run dev
```

## 后端联调
默认请求地址：`http://localhost:8083/api`
可通过环境变量覆盖：

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8083/api
```
