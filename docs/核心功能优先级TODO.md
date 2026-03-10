# 核心功能优先级 TODO（毕业设计）

更新时间：2026-03-10

## P0（必须完成）

- [x] 数据库结构兼容迁移
  - 完成：`contest_participant.create_time` 兼容补齐
  - 完成：`contest.scoreboard_freeze_time` 兼容补齐
  - 完成：`contest.penalty_per_wrong` 兼容补齐
  - 完成：Flyway 迁移脚本 `onlinejudge-backend/src/main/resources/db/migration/V4__contest_compatibility_backfill.sql`
- [x] 账号与权限闭环（管理员/教师/学生）
  - 完成：统一鉴权工具与越权拦截
  - 完成：关键控制器权限回归测试
- [x] 题目管理闭环
  - 完成：题目 CRUD
  - 完成：测试点维护
  - 完成：批量导入接口 `POST /problem/batch-import`
- [x] 判题主链路稳定性
  - 完成：本地判题超时控制修复（防卡死）
  - 完成：Java/C++/Python 评测结果稳定回写

## P1（核心可用性）

- [x] 提交记录与提交详情闭环
  - 完成：提交详情页回跳题目并保留筛选上下文
  - 完成：重判接口 `POST /submission/{id}/rejudge`
  - 完成：提交详情页接入重判按钮（教师/管理员）
- [x] 竞赛核心闭环
  - 完成：竞赛创建/编辑/删除
  - 完成：报名、实时榜、快照榜
  - 完成：封榜时间与错误罚时
  - 完成：端到端冒烟脚本 `npm run smoke:contest`
- [x] 管理后台核心（系统配置 + 操作日志 + 监控）
  - 完成：后台接口与前端页面联调
  - 完成：接口冒烟覆盖（admin configs/logs/monitor）

## P2（答辩交付）

- [x] 核心接口自动化冒烟测试
  - 完成：`onlinejudge-frontend/scripts/smoke_test.js`
  - 完成：覆盖重判接口与批量导入接口
- [x] 最小部署与演示文档
  - 完成：`docs/最小部署与演示文档.md`
  - 覆盖：MySQL + Redis + Backend + Frontend + Docker 判题 + 演示流程 + 常见故障处理

## 当前结论

核心功能已闭环，可进入：

1. 数据与内容填充（题目与测试数据规模化）
2. 答辩材料准备（架构图、流程图、压测截图、功能演示录屏）
