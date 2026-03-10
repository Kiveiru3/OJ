# 题目导入说明（核心版）

## 1. 生成题库 JSON

```bash
node scripts/generate_classic_problems.js
```

默认输出：`scripts/data/classic_problems_200.json`

## 2. 批量导入（推荐）

```bash
node scripts/import_classic_problems.js --token <JWT>
```

说明：
- 现在默认走后端批量接口：`POST /problem/batch-import`
- 支持一次导入多题（包含隐藏测试数据）
- 默认按标题跳过已存在题目（避免重复导入）

常用参数：
- `--base-url`：接口地址，默认 `http://localhost:8082/api`
- `--input`：题库 JSON 路径
- `--start`：起始题号（1-based）
- `--limit`：最多导入数量，`0` 表示导入到末尾
- `--dry-run`：只校验 JSON，不发送请求
- `--no-batch`：使用旧版逐题导入模式（慢）

示例：

```bash
# 先导入 20 题验证
node scripts/import_classic_problems.js --token <JWT> --limit 20

# 从第 101 题开始导入 50 题
node scripts/import_classic_problems.js --token <JWT> --start 101 --limit 50

# 仅校验
node scripts/import_classic_problems.js --dry-run
```

## 3. 顺序分批导入（每批20题）

```bash
node scripts/import_in_order_batches.js --token <JWT> --batch-size 20 --start 1 --end 200
```

## 4. 注意事项

- 需要教师或管理员 JWT。
- 后端需已完成数据库迁移并正常启动。
- 如果返回 `failed > 0`，查看错误列表后修复对应题目数据再重试。
