# 洛谷题库隐藏数据生成（20题）

本目录新增了三份脚本：

- `scripts/lib/luogu_hidden_20_recipes.js`
  - 20 道核心题的 `buildCase + solve` 规则库
- `scripts/generate_luogu_hidden_cases_pack.js`
  - 在原始题库 JSON 上追加隐藏测试点（默认保留已有测试点）
- `scripts/verify_luogu_hidden_cases_pack.js`
  - 用同一套参考解回放校验样例与测试点，检查数据一致性

## 1) 生成隐藏数据

```bash
node scripts/generate_luogu_hidden_cases_pack.js \
  --input scripts/data/luogu_offline_problems_1000.json \
  --output scripts/data/luogu_offline_problems_1000_hidden_20.json \
  --report scripts/data/luogu_hidden_20_report.json \
  --seed 20260225
```

## 2) 校验生成结果

```bash
node scripts/verify_luogu_hidden_cases_pack.js \
  --input scripts/data/luogu_offline_problems_1000_hidden_20.json \
  --limit-per-problem 300
```

## 3) 导入到后端

```bash
node scripts/import_classic_problems.js \
  --input scripts/data/luogu_offline_problems_1000_hidden_20.json \
  --token <JWT>
```

## 可选参数

- `generate_luogu_hidden_cases_pack.js`
  - `--hidden-per-problem <n>`：强制每题隐藏点数量
  - `--replace-cases`：不用原有测试点，只保留新生成隐藏点
- `verify_luogu_hidden_cases_pack.js`
  - `--sample-only`：仅校验题目样例

