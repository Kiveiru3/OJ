# 经典算法题批量导入（200题）

本目录提供两步工具：

1. `generate_classic_problems.js`  
生成 `scripts/data/classic_problems_200.json`（200 道经典算法题，含样例与测试用例）。

2. `import_classic_problems.js`  
通过后端接口批量导入题目（`/problem/create` + `/problem/{id}/test-cases`）。

## 1. 生成题库数据

```bash
node scripts/generate_classic_problems.js
```

生成结果：

- 文件：`scripts/data/classic_problems_200.json`
- 题量：200
- 难度分布（当前版本）：`EASY=84, MEDIUM=74, HARD=42`
- 题面：包含 `题目背景 / 任务描述 / 数据范围 / 解题提示` 四段结构化内容，便于读题。

## 2. 批量导入到后端

```bash
node scripts/import_classic_problems.js --token <你的JWT>
```

常用参数：

- `--base-url`：接口地址（默认 `http://localhost:8082/api`）
- `--input`：题库 JSON 文件路径
- `--start`：从第几题开始导入（1-based）
- `--limit`：最多导入多少题（0 表示全部）
- `--delay`：每题导入间隔毫秒（默认 80）
- `--dry-run`：只校验数据，不发请求

示例：

```bash
# 只导入前 20 题试运行
node scripts/import_classic_problems.js --token <JWT> --limit 20

# 从第 51 题开始，导入 50 题
node scripts/import_classic_problems.js --token <JWT> --start 51 --limit 50

# 只检查数据结构
node scripts/import_classic_problems.js --dry-run
```

## 3. 注意事项

- 需要使用教师或管理员账号的 token。
- 导入脚本会按“题目标题”自动跳过已存在题目，避免重复导入。
- 建议先 `--limit 10` 小批量验证后再全量导入。
