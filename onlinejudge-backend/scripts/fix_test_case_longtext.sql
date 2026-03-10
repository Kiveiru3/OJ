USE onlinejudge;

-- Expand test_case payload capacity for large official datasets.
-- TEXT (~64KB) is too small for many ICPC test inputs/outputs.
ALTER TABLE `test_case`
  MODIFY COLUMN `input` LONGTEXT NOT NULL COMMENT '输入数据',
  MODIFY COLUMN `output` LONGTEXT NOT NULL COMMENT '期望输出';

-- Verify current column types.
SELECT
  COLUMN_NAME,
  COLUMN_TYPE,
  IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'test_case'
  AND COLUMN_NAME IN ('input', 'output');

