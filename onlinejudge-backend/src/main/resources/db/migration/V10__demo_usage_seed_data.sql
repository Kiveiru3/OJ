-- Demo usage seed data
-- Make the system look actively used across homepage, contests,
-- discussions, messaging, teacher analytics, and admin console.

SET @seed_now = NOW();
SET @demo_password = '$2a$10$FoAxdf9RStrvA10Njh7ErOOOhjIgJf/OVkcfS9ZjdMbXgr4N9Dl.O';

-- ------------------------------------------------------------
-- Demo users
-- ------------------------------------------------------------

INSERT INTO `user` (
  `username`, `password`, `email`, `nickname`, `role`, `status`, `deleted`, `create_time`, `update_time`
)
VALUES
  ('demo_admin', @demo_password, 'demo_admin@oj.local', '平台运维', 'ADMIN', 1, 0, DATE_SUB(@seed_now, INTERVAL 30 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY)),
  ('demo_teacher', @demo_password, 'demo_teacher@oj.local', '林老师', 'TEACHER', 1, 0, DATE_SUB(@seed_now, INTERVAL 28 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY)),
  ('demo_student', @demo_password, 'demo_student@oj.local', '陈雨', 'STUDENT', 1, 0, DATE_SUB(@seed_now, INTERVAL 26 DAY), DATE_SUB(@seed_now, INTERVAL 30 MINUTE)),
  ('li_xuan', @demo_password, 'li_xuan@oj.local', '李轩', 'STUDENT', 1, 0, DATE_SUB(@seed_now, INTERVAL 24 DAY), DATE_SUB(@seed_now, INTERVAL 2 HOUR)),
  ('zhao_qi', @demo_password, 'zhao_qi@oj.local', '赵琪', 'STUDENT', 1, 0, DATE_SUB(@seed_now, INTERVAL 23 DAY), DATE_SUB(@seed_now, INTERVAL 3 HOUR)),
  ('sun_yi', @demo_password, 'sun_yi@oj.local', '孙屹', 'STUDENT', 1, 0, DATE_SUB(@seed_now, INTERVAL 22 DAY), DATE_SUB(@seed_now, INTERVAL 50 MINUTE)),
  ('qin_mo', @demo_password, 'qin_mo@oj.local', '秦墨', 'STUDENT', 1, 0, DATE_SUB(@seed_now, INTERVAL 21 DAY), DATE_SUB(@seed_now, INTERVAL 5 HOUR)),
  ('wu_tong', @demo_password, 'wu_tong@oj.local', '吴桐', 'STUDENT', 1, 0, DATE_SUB(@seed_now, INTERVAL 20 DAY), DATE_SUB(@seed_now, INTERVAL 80 MINUTE))
ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `email` = VALUES(`email`),
  `nickname` = VALUES(`nickname`),
  `role` = VALUES(`role`),
  `status` = VALUES(`status`),
  `deleted` = VALUES(`deleted`),
  `update_time` = VALUES(`update_time`);

SET @uid_admin = (SELECT `id` FROM `user` WHERE `username` = 'demo_admin' LIMIT 1);
SET @uid_teacher = (SELECT `id` FROM `user` WHERE `username` = 'demo_teacher' LIMIT 1);
SET @uid_demo_student = (SELECT `id` FROM `user` WHERE `username` = 'demo_student' LIMIT 1);
SET @uid_li_xuan = (SELECT `id` FROM `user` WHERE `username` = 'li_xuan' LIMIT 1);
SET @uid_zhao_qi = (SELECT `id` FROM `user` WHERE `username` = 'zhao_qi' LIMIT 1);
SET @uid_sun_yi = (SELECT `id` FROM `user` WHERE `username` = 'sun_yi' LIMIT 1);
SET @uid_qin_mo = (SELECT `id` FROM `user` WHERE `username` = 'qin_mo' LIMIT 1);
SET @uid_wu_tong = (SELECT `id` FROM `user` WHERE `username` = 'wu_tong' LIMIT 1);

INSERT INTO `admin_profile` (
  `user_id`, `admin_code`, `real_name`, `department`, `bio`, `create_time`, `update_time`
)
VALUES
  (@uid_admin, 'ADM-OPS-01', '周望', '平台运维中心', '负责评测平台账号、公告与活动编排维护。', DATE_SUB(@seed_now, INTERVAL 30 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE
  `admin_code` = VALUES(`admin_code`),
  `real_name` = VALUES(`real_name`),
  `department` = VALUES(`department`),
  `bio` = VALUES(`bio`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `teacher_profile` (
  `user_id`, `teacher_no`, `department`, `title`, `real_name`, `gender`, `bio`, `create_time`, `update_time`
)
VALUES
  (@uid_teacher, 'T2024-017', '计算机科学与技术学院', '副教授', '林知行', '男', '负责算法设计与程序设计基础课程，本学期带训练营和周赛。', DATE_SUB(@seed_now, INTERVAL 28 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE
  `teacher_no` = VALUES(`teacher_no`),
  `department` = VALUES(`department`),
  `title` = VALUES(`title`),
  `real_name` = VALUES(`real_name`),
  `gender` = VALUES(`gender`),
  `bio` = VALUES(`bio`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `student_profile` (
  `user_id`, `student_no`, `class_name`, `major`, `real_name`, `gender`, `bio`, `create_time`, `update_time`
)
VALUES
  (@uid_demo_student, '2023110108', '23级软件工程1班', '软件工程', '陈雨', '女', '最近在补图论和前缀和，习惯先写 Python 验证思路。', DATE_SUB(@seed_now, INTERVAL 26 DAY), DATE_SUB(@seed_now, INTERVAL 30 MINUTE)),
  (@uid_li_xuan, '2023110121', '23级软件工程1班', '软件工程', '李轩', '男', '稳定刷题型选手，周赛常用 C++。', DATE_SUB(@seed_now, INTERVAL 24 DAY), DATE_SUB(@seed_now, INTERVAL 2 HOUR)),
  (@uid_zhao_qi, '2023110136', '23级计算机2班', '计算机科学与技术', '赵琪', '女', '喜欢 BFS 和搜索题，最近在练图最短路。', DATE_SUB(@seed_now, INTERVAL 23 DAY), DATE_SUB(@seed_now, INTERVAL 3 HOUR)),
  (@uid_sun_yi, '2023110150', '23级人工智能1班', '人工智能', '孙屹', '男', '对栈和语法分析类题目很感兴趣。', DATE_SUB(@seed_now, INTERVAL 22 DAY), DATE_SUB(@seed_now, INTERVAL 50 MINUTE)),
  (@uid_qin_mo, '2023110163', '23级软件工程2班', '软件工程', '秦墨', '男', '刚开始做周赛，正在补基础题和调试习惯。', DATE_SUB(@seed_now, INTERVAL 21 DAY), DATE_SUB(@seed_now, INTERVAL 5 HOUR)),
  (@uid_wu_tong, '2023110172', '23级计算机1班', '计算机科学与技术', '吴桐', '女', '做题速度快，偶尔会在实现细节上翻车。', DATE_SUB(@seed_now, INTERVAL 20 DAY), DATE_SUB(@seed_now, INTERVAL 80 MINUTE))
ON DUPLICATE KEY UPDATE
  `student_no` = VALUES(`student_no`),
  `class_name` = VALUES(`class_name`),
  `major` = VALUES(`major`),
  `real_name` = VALUES(`real_name`),
  `gender` = VALUES(`gender`),
  `bio` = VALUES(`bio`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `system_config` (
  `config_key`, `config_value`, `description`, `update_user_id`, `create_time`, `update_time`
)
VALUES
  ('site.announcement', '', 'Homepage announcement', @uid_admin, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE
  `config_value` = CASE
    WHEN `config_value` IS NULL OR `config_value` = '' THEN VALUES(`config_value`)
    ELSE `config_value`
  END,
  `description` = VALUES(`description`),
  `update_user_id` = COALESCE(`update_user_id`, VALUES(`update_user_id`));

-- ------------------------------------------------------------
-- Problems and test cases
-- ------------------------------------------------------------

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '新生报到求和',
  '给定两个整数 a 和 b，输出它们的和。',
  '输入一行，包含两个整数 a 和 b。',
  '输出一个整数，表示 a + b。',
  '1 2',
  '3',
  '适合作为平台环境检查题。',
  1000, 262144, 'EASY', 'math,implementation', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 18 DAY), DATE_SUB(@seed_now, INTERVAL 18 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '新生报到求和' AND `deleted` = 0
);

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '签到前缀统计',
  '给定 n 天签到次数和 q 个区间查询，输出每个区间的签到总数。',
  '第一行包含 n 和 q。第二行包含 n 个整数。接下来 q 行每行两个整数 l、r，表示 1-based 区间。',
  '对每个查询输出一行答案。',
  '5 3
1 2 3 4 5
1 3
2 5
4 4',
  '6
14
4',
  '典型前缀和入门题。',
  1000, 262144, 'EASY', 'prefix-sum,array', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 17 DAY), DATE_SUB(@seed_now, INTERVAL 17 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '签到前缀统计' AND `deleted` = 0
);

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '实验室队列压缩',
  '给定一个只包含大写字母的字符串，删除相邻重复字符后输出压缩结果。',
  '输入一行字符串 s。',
  '输出压缩后的字符串。',
  'ABBCCCCDAA',
  'ABCDA',
  '顺序扫描即可完成。',
  1000, 262144, 'EASY', 'string,simulation', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 16 DAY), DATE_SUB(@seed_now, INTERVAL 16 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '实验室队列压缩' AND `deleted` = 0
);

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '教学楼温度波动',
  '给定一组连续天数的温差变化值，求最大连续子段和。',
  '第一行输入 n。第二行输入 n 个整数。',
  '输出一个整数，表示最大连续子段和。',
  '8
-2 3 5 -1 4 -5 2 2',
  '11',
  '使用 Kadane 算法可以在线性时间内解决。',
  1500, 262144, 'MEDIUM', 'dp,array', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 15 DAY), DATE_SUB(@seed_now, INTERVAL 15 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '教学楼温度波动' AND `deleted` = 0
);

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '奖学金名单筛选',
  '给定 n 名同学的姓名和成绩，按成绩从高到低、姓名字典序从小到大排序，输出前 k 名。',
  '第一行输入 n 和 k。接下来 n 行每行输入 name 和 score。',
  '输出前 k 名同学的姓名，每行一个。',
  '5 3
chenyu 92
lixuan 95
zhaoqi 95
sunyi 88
wutong 91',
  'lixuan
zhaoqi
chenyu',
  '重点是多关键字排序。',
  1500, 262144, 'MEDIUM', 'sort,simulation', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 14 DAY), DATE_SUB(@seed_now, INTERVAL 14 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '奖学金名单筛选' AND `deleted` = 0
);

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '校车最短路线',
  '在 n x m 的校园地图中，S 表示起点，T 表示终点，# 表示障碍，. 表示可通行，求最短步数。',
  '第一行输入 n 和 m。接下来输入 n 行地图。',
  '输出从 S 到 T 的最短步数，若无法到达输出 -1。',
  '4 5
S...#
.##.#
...#.
..T..',
  '6',
  '标准 BFS 最短路。',
  2000, 262144, 'MEDIUM', 'graph,bfs', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 13 DAY), DATE_SUB(@seed_now, INTERVAL 13 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '校车最短路线' AND `deleted` = 0
);

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '编译告警过滤',
  '给定一个只包含 ()[]{} 的字符串，判断括号是否完全匹配。',
  '输入一行字符串 s。',
  '若匹配输出 YES，否则输出 NO。',
  '{[()()]}',
  'YES',
  '使用栈维护括号匹配关系。',
  2000, 262144, 'HARD', 'stack,string', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 12 DAY), DATE_SUB(@seed_now, INTERVAL 12 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '编译告警过滤' AND `deleted` = 0
);

INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`,
  `ac_count`, `submit_count`, `deleted`, `create_time`, `update_time`
)
SELECT
  '科研任务调度',
  '给定若干任务依赖关系，若存在可行顺序则输出任意一种拓扑序，否则输出 IMPOSSIBLE。',
  '第一行输入 n 和 m，接下来 m 行输入 a b，表示完成 a 后才能完成 b。',
  '输出一个合法拓扑序，若不存在则输出 IMPOSSIBLE。',
  '4 3
1 2
1 3
3 4',
  '1 2 3 4',
  '入度为 0 的节点可以优先入队。',
  2500, 262144, 'HARD', 'graph,topological-sort', @uid_teacher, 1,
  0, 0, 0, DATE_SUB(@seed_now, INTERVAL 11 DAY), DATE_SUB(@seed_now, INTERVAL 11 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = '科研任务调度' AND `deleted` = 0
);

SET @pid_p1 = (SELECT `id` FROM `problem` WHERE `title` = '新生报到求和' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);
SET @pid_p2 = (SELECT `id` FROM `problem` WHERE `title` = '签到前缀统计' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);
SET @pid_p3 = (SELECT `id` FROM `problem` WHERE `title` = '实验室队列压缩' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);
SET @pid_p4 = (SELECT `id` FROM `problem` WHERE `title` = '教学楼温度波动' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);
SET @pid_p5 = (SELECT `id` FROM `problem` WHERE `title` = '奖学金名单筛选' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);
SET @pid_p6 = (SELECT `id` FROM `problem` WHERE `title` = '校车最短路线' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);
SET @pid_p7 = (SELECT `id` FROM `problem` WHERE `title` = '编译告警过滤' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);
SET @pid_p8 = (SELECT `id` FROM `problem` WHERE `title` = '科研任务调度' AND `deleted` = 0 ORDER BY `id` ASC LIMIT 1);

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p1, '1 2', '3', 1
WHERE @pid_p1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p1 AND `input` = '1 2' AND `output` = '3');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p1, '100 250', '350', 0
WHERE @pid_p1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p1 AND `input` = '100 250' AND `output` = '350');

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p2, '5 3
1 2 3 4 5
1 3
2 5
4 4', '6
14
4', 1
WHERE @pid_p2 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p2 AND `input` = '5 3
1 2 3 4 5
1 3
2 5
4 4');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p2, '4 2
2 2 2 2
1 4
2 3', '8
4', 0
WHERE @pid_p2 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p2 AND `input` = '4 2
2 2 2 2
1 4
2 3');

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p3, 'ABBCCCCDAA', 'ABCDA', 1
WHERE @pid_p3 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p3 AND `input` = 'ABBCCCCDAA' AND `output` = 'ABCDA');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p3, 'AAAAA', 'A', 0
WHERE @pid_p3 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p3 AND `input` = 'AAAAA' AND `output` = 'A');

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p4, '8
-2 3 5 -1 4 -5 2 2', '11', 1
WHERE @pid_p4 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p4 AND `input` = '8
-2 3 5 -1 4 -5 2 2');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p4, '5
-5 -2 -3 -4 -1', '-1', 0
WHERE @pid_p4 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p4 AND `input` = '5
-5 -2 -3 -4 -1');

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p5, '5 3
chenyu 92
lixuan 95
zhaoqi 95
sunyi 88
wutong 91', 'lixuan
zhaoqi
chenyu', 1
WHERE @pid_p5 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p5 AND `input` = '5 3
chenyu 92
lixuan 95
zhaoqi 95
sunyi 88
wutong 91');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p5, '4 2
amy 80
bob 80
carl 95
dora 88', 'carl
dora', 0
WHERE @pid_p5 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p5 AND `input` = '4 2
amy 80
bob 80
carl 95
dora 88');

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p6, '4 5
S...#
.##.#
...#.
..T..', '6', 1
WHERE @pid_p6 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p6 AND `input` = '4 5
S...#
.##.#
...#.
..T..');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p6, '3 3
S##
###
##T', '-1', 0
WHERE @pid_p6 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p6 AND `input` = '3 3
S##
###
##T');

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p7, '{[()()]}', 'YES', 1
WHERE @pid_p7 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p7 AND `input` = '{[()()]}' AND `output` = 'YES');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p7, '{[(])}', 'NO', 0
WHERE @pid_p7 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p7 AND `input` = '{[(])}' AND `output` = 'NO');

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p8, '4 3
1 2
1 3
3 4', '1 2 3 4', 1
WHERE @pid_p8 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p8 AND `input` = '4 3
1 2
1 3
3 4');
INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @pid_p8, '3 3
1 2
2 3
3 1', 'IMPOSSIBLE', 0
WHERE @pid_p8 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `test_case` WHERE `problem_id` = @pid_p8 AND `input` = '3 3
1 2
2 3
3 1');

-- ------------------------------------------------------------
-- Contests
-- ------------------------------------------------------------

SET @contest_warmup_start = DATE_SUB(@seed_now, INTERVAL 12 DAY);
SET @contest_warmup_end = DATE_ADD(@contest_warmup_start, INTERVAL 3 HOUR);
SET @contest_weekly_start = DATE_SUB(@seed_now, INTERVAL 6 HOUR);
SET @contest_weekly_end = DATE_ADD(@contest_weekly_start, INTERVAL 24 HOUR);
SET @contest_weekly_freeze = DATE_ADD(@contest_weekly_start, INTERVAL 20 HOUR);
SET @contest_mock_start = DATE_ADD(@seed_now, INTERVAL 2 DAY);
SET @contest_mock_end = DATE_ADD(@contest_mock_start, INTERVAL 3 HOUR);

INSERT INTO `contest` (
  `title`, `description`, `start_time`, `end_time`, `scoreboard_freeze_time`, `penalty_per_wrong`,
  `creator_id`, `status`, `deleted`, `create_time`, `update_time`
)
SELECT
  '新生训练热身赛',
  '面向新同学的入门热身赛，覆盖求和、前缀和和字符串基础题。',
  @contest_warmup_start, @contest_warmup_end, NULL, 20,
  @uid_teacher, 1, 0, DATE_SUB(@seed_now, INTERVAL 13 DAY), DATE_SUB(@seed_now, INTERVAL 13 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `contest` WHERE `title` = '新生训练热身赛'
);

INSERT INTO `contest` (
  `title`, `description`, `start_time`, `end_time`, `scoreboard_freeze_time`, `penalty_per_wrong`,
  `creator_id`, `status`, `deleted`, `create_time`, `update_time`
)
SELECT
  '春季周赛 Week 4',
  '本周进行中的训练赛，重点考察排序、动态规划与 BFS。',
  @contest_weekly_start, @contest_weekly_end, @contest_weekly_freeze, 20,
  @uid_teacher, 1, 0, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 1 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `contest` WHERE `title` = '春季周赛 Week 4'
);

INSERT INTO `contest` (
  `title`, `description`, `start_time`, `end_time`, `scoreboard_freeze_time`, `penalty_per_wrong`,
  `creator_id`, `status`, `deleted`, `create_time`, `update_time`
)
SELECT
  '数据结构专项模拟赛',
  '即将开始的专项模拟赛，提前开放报名用于熟悉比赛环境。',
  @contest_mock_start, @contest_mock_end, NULL, 20,
  @uid_teacher, 1, 0, DATE_SUB(@seed_now, INTERVAL 12 HOUR), DATE_SUB(@seed_now, INTERVAL 12 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `contest` WHERE `title` = '数据结构专项模拟赛'
);

SET @contest_warmup = (SELECT `id` FROM `contest` WHERE `title` = '新生训练热身赛' LIMIT 1);
SET @contest_weekly = (SELECT `id` FROM `contest` WHERE `title` = '春季周赛 Week 4' LIMIT 1);
SET @contest_mock = (SELECT `id` FROM `contest` WHERE `title` = '数据结构专项模拟赛' LIMIT 1);

INSERT INTO `contest_problem` (`contest_id`, `problem_id`)
VALUES
  (@contest_warmup, @pid_p1),
  (@contest_warmup, @pid_p2),
  (@contest_warmup, @pid_p3),
  (@contest_weekly, @pid_p4),
  (@contest_weekly, @pid_p5),
  (@contest_weekly, @pid_p6),
  (@contest_mock, @pid_p6),
  (@contest_mock, @pid_p7),
  (@contest_mock, @pid_p8)
ON DUPLICATE KEY UPDATE
  `problem_id` = VALUES(`problem_id`);

INSERT INTO `contest_participant` (`contest_id`, `user_id`, `create_time`)
VALUES
  (@contest_warmup, @uid_demo_student, DATE_ADD(@contest_warmup_start, INTERVAL 10 MINUTE)),
  (@contest_warmup, @uid_li_xuan, DATE_ADD(@contest_warmup_start, INTERVAL 12 MINUTE)),
  (@contest_warmup, @uid_zhao_qi, DATE_ADD(@contest_warmup_start, INTERVAL 14 MINUTE)),
  (@contest_warmup, @uid_qin_mo, DATE_ADD(@contest_warmup_start, INTERVAL 20 MINUTE)),
  (@contest_weekly, @uid_demo_student, DATE_ADD(@contest_weekly_start, INTERVAL 5 MINUTE)),
  (@contest_weekly, @uid_sun_yi, DATE_ADD(@contest_weekly_start, INTERVAL 7 MINUTE)),
  (@contest_weekly, @uid_wu_tong, DATE_ADD(@contest_weekly_start, INTERVAL 9 MINUTE)),
  (@contest_weekly, @uid_zhao_qi, DATE_ADD(@contest_weekly_start, INTERVAL 11 MINUTE)),
  (@contest_mock, @uid_demo_student, DATE_SUB(@contest_mock_start, INTERVAL 3 HOUR)),
  (@contest_mock, @uid_li_xuan, DATE_SUB(@contest_mock_start, INTERVAL 2 HOUR)),
  (@contest_mock, @uid_sun_yi, DATE_SUB(@contest_mock_start, INTERVAL 90 MINUTE))
ON DUPLICATE KEY UPDATE
  `create_time` = LEAST(`create_time`, VALUES(`create_time`));

-- ------------------------------------------------------------
-- Submissions and judge results
-- ------------------------------------------------------------

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_demo_student, @pid_p1, 'PYTHON',
  'demo-seed:sub-demo-student-p1-ac python add two ints',
  'ACCEPTED', 18, 16384, NULL, DATE_ADD(@contest_warmup_start, INTERVAL 25 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p1-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_demo_student, @pid_p2, 'PYTHON',
  'demo-seed:sub-demo-student-p2-ac python prefix sum solution',
  'ACCEPTED', 31, 24576, NULL, DATE_ADD(@contest_warmup_start, INTERVAL 65 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p2-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_li_xuan, @pid_p1, 'JAVA',
  'demo-seed:sub-li-xuan-p1-ac java add two ints',
  'ACCEPTED', 22, 32768, NULL, DATE_ADD(@contest_warmup_start, INTERVAL 18 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p1-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_li_xuan, @pid_p2, 'CPP',
  'demo-seed:sub-li-xuan-p2-wa cpp wrong answer prefix sum indices',
  'WRONG_ANSWER', 19, 20480, 'Case 2 mismatch at query result.', DATE_ADD(@contest_warmup_start, INTERVAL 40 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p2-wa%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_li_xuan, @pid_p2, 'CPP',
  'demo-seed:sub-li-xuan-p2-ac cpp prefix sum accepted',
  'ACCEPTED', 27, 21504, NULL, DATE_ADD(@contest_warmup_start, INTERVAL 58 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p2-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_li_xuan, @pid_p3, 'PYTHON',
  'demo-seed:sub-li-xuan-p3-ac python remove adjacent duplicates',
  'ACCEPTED', 16, 18432, NULL, DATE_ADD(@contest_warmup_start, INTERVAL 82 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p3-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_zhao_qi, @pid_p1, 'PYTHON',
  'demo-seed:sub-zhao-qi-p1-ac python add two ints',
  'ACCEPTED', 20, 16384, NULL, DATE_ADD(@contest_warmup_start, INTERVAL 24 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-zhao-qi-p1-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_qin_mo, @pid_p1, 'PYTHON',
  'demo-seed:sub-qin-mo-p1-ac python add two ints',
  'ACCEPTED', 25, 16384, NULL, DATE_ADD(@contest_warmup_start, INTERVAL 35 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-qin-mo-p1-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_qin_mo, @pid_p2, 'PYTHON',
  'demo-seed:sub-qin-mo-p2-wa python wrong answer range sum',
  'WRONG_ANSWER', 33, 22528, 'Index boundary handling failed on 1-based query.', DATE_ADD(@contest_warmup_start, INTERVAL 88 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-qin-mo-p2-wa%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_demo_student, @pid_p5, 'PYTHON',
  'demo-seed:sub-demo-student-p5-ac python sort scholarship list',
  'ACCEPTED', 44, 28672, NULL, DATE_ADD(@contest_weekly_start, INTERVAL 35 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p5-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_demo_student, @pid_p6, 'PYTHON',
  'demo-seed:sub-demo-student-p6-wa python wrong answer bfs route',
  'WRONG_ANSWER', 29, 20480, 'Shortest path not found for reachable sample.', DATE_ADD(@contest_weekly_start, INTERVAL 70 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p6-wa%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_wu_tong, @pid_p4, 'CPP',
  'demo-seed:sub-wu-tong-p4-ac cpp maximum subarray accepted',
  'ACCEPTED', 36, 23552, NULL, DATE_ADD(@contest_weekly_start, INTERVAL 25 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-wu-tong-p4-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_zhao_qi, @pid_p6, 'CPP',
  'demo-seed:sub-zhao-qi-p6-ac cpp bfs shortest path accepted',
  'ACCEPTED', 57, 32768, NULL, DATE_ADD(@contest_weekly_start, INTERVAL 95 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-zhao-qi-p6-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_sun_yi, @pid_p5, 'PYTHON',
  'demo-seed:sub-sun-yi-p5-wa python wrong answer sorting order',
  'WRONG_ANSWER', 32, 24576, 'Sorting order ignores score priority.', DATE_ADD(@contest_weekly_start, INTERVAL 20 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p5-wa%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_sun_yi, @pid_p7, 'JAVA',
  'demo-seed:sub-sun-yi-p7-ac java bracket stack accepted',
  'ACCEPTED', 63, 36864, NULL, DATE_SUB(@seed_now, INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p7-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_sun_yi, @pid_p1, 'PYTHON',
  'demo-seed:sub-sun-yi-p1-ac python add two ints',
  'ACCEPTED', 17, 16384, NULL, DATE_SUB(@seed_now, INTERVAL 9 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p1-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_sun_yi, @pid_p2, 'PYTHON',
  'demo-seed:sub-sun-yi-p2-ac python prefix sum accepted',
  'ACCEPTED', 26, 24576, NULL, DATE_SUB(@seed_now, INTERVAL 8 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p2-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_li_xuan, @pid_p4, 'CPP',
  'demo-seed:sub-li-xuan-p4-ac cpp maximum subarray accepted',
  'ACCEPTED', 33, 22528, NULL, DATE_SUB(@seed_now, INTERVAL 2 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p4-ac%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_wu_tong, @pid_p7, 'JAVA',
  'demo-seed:sub-wu-tong-p7-ce java compile error missing semicolon',
  'COMPILE_ERROR', NULL, NULL, 'Main.java:5: error: semicolon expected', DATE_SUB(@seed_now, INTERVAL 3 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-wu-tong-p7-ce%');

INSERT INTO `submission` (
  `user_id`, `problem_id`, `language`, `code`, `status`, `time_used`, `memory_used`, `error_message`, `create_time`
)
SELECT
  @uid_wu_tong, @pid_p3, 'PYTHON',
  'demo-seed:sub-wu-tong-p3-ac python remove adjacent duplicates',
  'ACCEPTED', 14, 17408, NULL, DATE_SUB(@seed_now, INTERVAL 4 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `submission` WHERE `code` LIKE '%demo-seed:sub-wu-tong-p3-ac%');

SET @sid_demo_student_p1 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p1-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_demo_student_p2 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p2-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_li_xuan_p1 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p1-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_li_xuan_p2_wa = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p2-wa%' ORDER BY `id` DESC LIMIT 1);
SET @sid_li_xuan_p2_ac = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p2-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_li_xuan_p3 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p3-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_zhao_qi_p1 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-zhao-qi-p1-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_qin_mo_p1 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-qin-mo-p1-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_qin_mo_p2_wa = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-qin-mo-p2-wa%' ORDER BY `id` DESC LIMIT 1);
SET @sid_demo_student_p5 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p5-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_demo_student_p6_wa = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-demo-student-p6-wa%' ORDER BY `id` DESC LIMIT 1);
SET @sid_wu_tong_p4 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-wu-tong-p4-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_zhao_qi_p6 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-zhao-qi-p6-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_sun_yi_p5_wa = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p5-wa%' ORDER BY `id` DESC LIMIT 1);
SET @sid_sun_yi_p7 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p7-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_sun_yi_p1 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p1-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_sun_yi_p2 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-sun-yi-p2-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_li_xuan_p4 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-li-xuan-p4-ac%' ORDER BY `id` DESC LIMIT 1);
SET @sid_wu_tong_p7_ce = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-wu-tong-p7-ce%' ORDER BY `id` DESC LIMIT 1);
SET @sid_wu_tong_p3 = (SELECT `id` FROM `submission` WHERE `code` LIKE '%demo-seed:sub-wu-tong-p3-ac%' ORDER BY `id` DESC LIMIT 1);

INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p1, @uid_demo_student, @pid_p1, 'PYTHON', 'ACCEPTED', 18, 16384, NULL,
       DATE_ADD(@contest_warmup_start, INTERVAL 26 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 26 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 26 MINUTE)
WHERE @sid_demo_student_p1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_demo_student_p1);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p2, @uid_demo_student, @pid_p2, 'PYTHON', 'ACCEPTED', 31, 24576, NULL,
       DATE_ADD(@contest_warmup_start, INTERVAL 66 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 66 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 66 MINUTE)
WHERE @sid_demo_student_p2 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_demo_student_p2);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_li_xuan_p1, @uid_li_xuan, @pid_p1, 'JAVA', 'ACCEPTED', 22, 32768, NULL,
       DATE_ADD(@contest_warmup_start, INTERVAL 19 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 19 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 19 MINUTE)
WHERE @sid_li_xuan_p1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_li_xuan_p1);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_li_xuan_p2_wa, @uid_li_xuan, @pid_p2, 'CPP', 'WRONG_ANSWER', 19, 20480, 'Case 2 mismatch at query result.',
       DATE_ADD(@contest_warmup_start, INTERVAL 41 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 41 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 41 MINUTE)
WHERE @sid_li_xuan_p2_wa IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_li_xuan_p2_wa);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_li_xuan_p2_ac, @uid_li_xuan, @pid_p2, 'CPP', 'ACCEPTED', 27, 21504, NULL,
       DATE_ADD(@contest_warmup_start, INTERVAL 59 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 59 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 59 MINUTE)
WHERE @sid_li_xuan_p2_ac IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_li_xuan_p2_ac);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_li_xuan_p3, @uid_li_xuan, @pid_p3, 'PYTHON', 'ACCEPTED', 16, 18432, NULL,
       DATE_ADD(@contest_warmup_start, INTERVAL 83 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 83 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 83 MINUTE)
WHERE @sid_li_xuan_p3 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_li_xuan_p3);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_zhao_qi_p1, @uid_zhao_qi, @pid_p1, 'PYTHON', 'ACCEPTED', 20, 16384, NULL,
       DATE_ADD(@contest_warmup_start, INTERVAL 25 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 25 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 25 MINUTE)
WHERE @sid_zhao_qi_p1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_zhao_qi_p1);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_qin_mo_p1, @uid_qin_mo, @pid_p1, 'PYTHON', 'ACCEPTED', 25, 16384, NULL,
       DATE_ADD(@contest_warmup_start, INTERVAL 36 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 36 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 36 MINUTE)
WHERE @sid_qin_mo_p1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_qin_mo_p1);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_qin_mo_p2_wa, @uid_qin_mo, @pid_p2, 'PYTHON', 'WRONG_ANSWER', 33, 22528, 'Index boundary handling failed on 1-based query.',
       DATE_ADD(@contest_warmup_start, INTERVAL 89 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 89 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 89 MINUTE)
WHERE @sid_qin_mo_p2_wa IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_qin_mo_p2_wa);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p5, @uid_demo_student, @pid_p5, 'PYTHON', 'ACCEPTED', 44, 28672, NULL,
       DATE_ADD(@contest_weekly_start, INTERVAL 36 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 36 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 36 MINUTE)
WHERE @sid_demo_student_p5 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_demo_student_p5);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p6_wa, @uid_demo_student, @pid_p6, 'PYTHON', 'WRONG_ANSWER', 29, 20480, 'Shortest path not found for reachable sample.',
       DATE_ADD(@contest_weekly_start, INTERVAL 71 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 71 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 71 MINUTE)
WHERE @sid_demo_student_p6_wa IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_demo_student_p6_wa);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_wu_tong_p4, @uid_wu_tong, @pid_p4, 'CPP', 'ACCEPTED', 36, 23552, NULL,
       DATE_ADD(@contest_weekly_start, INTERVAL 26 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 26 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 26 MINUTE)
WHERE @sid_wu_tong_p4 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_wu_tong_p4);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_zhao_qi_p6, @uid_zhao_qi, @pid_p6, 'CPP', 'ACCEPTED', 57, 32768, NULL,
       DATE_ADD(@contest_weekly_start, INTERVAL 96 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 96 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 96 MINUTE)
WHERE @sid_zhao_qi_p6 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_zhao_qi_p6);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_sun_yi_p5_wa, @uid_sun_yi, @pid_p5, 'PYTHON', 'WRONG_ANSWER', 32, 24576, 'Sorting order ignores score priority.',
       DATE_ADD(@contest_weekly_start, INTERVAL 21 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 21 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 21 MINUTE)
WHERE @sid_sun_yi_p5_wa IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_sun_yi_p5_wa);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_sun_yi_p7, @uid_sun_yi, @pid_p7, 'JAVA', 'ACCEPTED', 63, 36864, NULL,
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 1 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 1 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 1 DAY), INTERVAL 1 MINUTE)
WHERE @sid_sun_yi_p7 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_sun_yi_p7);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_sun_yi_p1, @uid_sun_yi, @pid_p1, 'PYTHON', 'ACCEPTED', 17, 16384, NULL,
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 9 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 9 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 9 DAY), INTERVAL 1 MINUTE)
WHERE @sid_sun_yi_p1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_sun_yi_p1);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_sun_yi_p2, @uid_sun_yi, @pid_p2, 'PYTHON', 'ACCEPTED', 26, 24576, NULL,
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 8 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 8 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 8 DAY), INTERVAL 1 MINUTE)
WHERE @sid_sun_yi_p2 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_sun_yi_p2);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_li_xuan_p4, @uid_li_xuan, @pid_p4, 'CPP', 'ACCEPTED', 33, 22528, NULL,
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 2 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 2 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 2 DAY), INTERVAL 1 MINUTE)
WHERE @sid_li_xuan_p4 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_li_xuan_p4);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_wu_tong_p7_ce, @uid_wu_tong, @pid_p7, 'JAVA', 'COMPILE_ERROR', NULL, NULL, 'Main.java:5: error: semicolon expected',
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 3 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 3 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 3 DAY), INTERVAL 1 MINUTE)
WHERE @sid_wu_tong_p7_ce IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_wu_tong_p7_ce);
INSERT INTO `judge_result` (
  `submission_id`, `user_id`, `problem_id`, `language`, `status`,
  `time_used`, `memory_used`, `error_message`, `judge_time`, `create_time`, `update_time`
)
SELECT @sid_wu_tong_p3, @uid_wu_tong, @pid_p3, 'PYTHON', 'ACCEPTED', 14, 17408, NULL,
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 4 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 4 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 4 DAY), INTERVAL 1 MINUTE)
WHERE @sid_wu_tong_p3 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `judge_result` WHERE `submission_id` = @sid_wu_tong_p3);

INSERT INTO `submission_case_result` (
  `submission_id`, `case_no`, `is_sample`, `status`, `time_used`, `memory_used`,
  `input_preview`, `expected_preview`, `actual_preview`, `error_message`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p5, 1, 1, 'ACCEPTED', 12, 12288, '5 3 / chenyu 92 ...', 'lixuan / zhaoqi / chenyu', 'lixuan / zhaoqi / chenyu', NULL,
       DATE_ADD(@contest_weekly_start, INTERVAL 36 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 36 MINUTE)
WHERE @sid_demo_student_p5 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `submission_case_result` WHERE `submission_id` = @sid_demo_student_p5 AND `case_no` = 1);
INSERT INTO `submission_case_result` (
  `submission_id`, `case_no`, `is_sample`, `status`, `time_used`, `memory_used`,
  `input_preview`, `expected_preview`, `actual_preview`, `error_message`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p5, 2, 0, 'ACCEPTED', 15, 16384, '4 2 / amy 80 ...', 'carl / dora', 'carl / dora', NULL,
       DATE_ADD(@contest_weekly_start, INTERVAL 36 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 36 MINUTE)
WHERE @sid_demo_student_p5 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `submission_case_result` WHERE `submission_id` = @sid_demo_student_p5 AND `case_no` = 2);
INSERT INTO `submission_case_result` (
  `submission_id`, `case_no`, `is_sample`, `status`, `time_used`, `memory_used`,
  `input_preview`, `expected_preview`, `actual_preview`, `error_message`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p6_wa, 1, 1, 'ACCEPTED', 11, 12288, '4 5 / S...# ...', '6', '6', NULL,
       DATE_ADD(@contest_weekly_start, INTERVAL 71 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 71 MINUTE)
WHERE @sid_demo_student_p6_wa IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `submission_case_result` WHERE `submission_id` = @sid_demo_student_p6_wa AND `case_no` = 1);
INSERT INTO `submission_case_result` (
  `submission_id`, `case_no`, `is_sample`, `status`, `time_used`, `memory_used`,
  `input_preview`, `expected_preview`, `actual_preview`, `error_message`, `create_time`, `update_time`
)
SELECT @sid_demo_student_p6_wa, 2, 0, 'WRONG_ANSWER', 14, 16384, '5 5 / custom hidden map', '8', '-1', 'Search terminated before expanding reachable nodes.',
       DATE_ADD(@contest_weekly_start, INTERVAL 71 MINUTE), DATE_ADD(@contest_weekly_start, INTERVAL 71 MINUTE)
WHERE @sid_demo_student_p6_wa IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `submission_case_result` WHERE `submission_id` = @sid_demo_student_p6_wa AND `case_no` = 2);
INSERT INTO `submission_case_result` (
  `submission_id`, `case_no`, `is_sample`, `status`, `time_used`, `memory_used`,
  `input_preview`, `expected_preview`, `actual_preview`, `error_message`, `create_time`, `update_time`
)
SELECT @sid_li_xuan_p2_wa, 1, 1, 'ACCEPTED', 9, 11264, '5 3 / 1 2 3 4 5 ...', '6 / 14 / 4', '6 / 3 / 0', 'Prefix sum formula used wrong indices.',
       DATE_ADD(@contest_warmup_start, INTERVAL 41 MINUTE), DATE_ADD(@contest_warmup_start, INTERVAL 41 MINUTE)
WHERE @sid_li_xuan_p2_wa IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `submission_case_result` WHERE `submission_id` = @sid_li_xuan_p2_wa AND `case_no` = 1);
INSERT INTO `submission_case_result` (
  `submission_id`, `case_no`, `is_sample`, `status`, `time_used`, `memory_used`,
  `input_preview`, `expected_preview`, `actual_preview`, `error_message`, `create_time`, `update_time`
)
SELECT @sid_wu_tong_p7_ce, 1, 1, 'COMPILE_ERROR', NULL, NULL, '{[()()]}', 'YES', NULL, 'Main.java:5: error: semicolon expected',
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 3 DAY), INTERVAL 1 MINUTE),
       DATE_ADD(DATE_SUB(@seed_now, INTERVAL 3 DAY), INTERVAL 1 MINUTE)
WHERE @sid_wu_tong_p7_ce IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `submission_case_result` WHERE `submission_id` = @sid_wu_tong_p7_ce AND `case_no` = 1);

-- ------------------------------------------------------------
-- Discussion posts, likes, comments
-- ------------------------------------------------------------

INSERT INTO `discussion_post` (
  `user_id`, `title`, `content`, `problem_id`, `view_count`, `like_count`, `audit_status`,
  `audit_user_id`, `audit_remark`, `audit_time`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_sun_yi,
  '签到前缀统计为什么用前缀和比逐次求和稳定很多？',
  '我把同一道题分别用逐次求和和前缀和写了一遍，数据量稍大时差距非常明显。建议新同学先把前缀数组的定义写清楚：pre[i] 表示前 i 项和，然后区间 [l, r] 的答案就是 pre[r] - pre[l-1]。如果是 1-based 下标，一定要注意边界。',
  @pid_p2, 156, 0, 1,
  @uid_admin, '内容完整，适合公开展示。', DATE_SUB(@seed_now, INTERVAL 5 DAY),
  0, DATE_SUB(@seed_now, INTERVAL 5 DAY), DATE_SUB(@seed_now, INTERVAL 5 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `discussion_post` WHERE `title` = '签到前缀统计为什么用前缀和比逐次求和稳定很多？'
);

INSERT INTO `discussion_post` (
  `user_id`, `title`, `content`, `problem_id`, `view_count`, `like_count`, `audit_status`,
  `audit_user_id`, `audit_remark`, `audit_time`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_teacher,
  '奖学金名单筛选的排序细节与常见失误',
  '这一题最容易错在排序关键字顺序。正确做法是先按分数降序，再按姓名升序。很多同学在 Python 里直接 sort() 忘了写 key，或者只排序姓名，导致样例能过但隐藏数据出错。建议先把元组结构定成 (-score, name)。',
  @pid_p5, 132, 0, 1,
  @uid_admin, '教师答疑帖，公开。', DATE_SUB(@seed_now, INTERVAL 3 DAY),
  0, DATE_SUB(@seed_now, INTERVAL 3 DAY), DATE_SUB(@seed_now, INTERVAL 3 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `discussion_post` WHERE `title` = '奖学金名单筛选的排序细节与常见失误'
);

INSERT INTO `discussion_post` (
  `user_id`, `title`, `content`, `problem_id`, `view_count`, `like_count`, `audit_status`,
  `audit_user_id`, `audit_remark`, `audit_time`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_demo_student,
  '校车最短路线卡在隐藏点位，BFS 还有哪些检查项？',
  '我本地样例能过，但提交后隐藏点一直 WA。回看了一遍代码，怀疑是起点入队后没有及时标记访问，或者对终点 T 的判断时机不对。有没有同学遇到过类似情况？',
  @pid_p6, 97, 0, 1,
  @uid_admin, '问题描述清晰，公开。', DATE_SUB(@seed_now, INTERVAL 1 DAY),
  0, DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `discussion_post` WHERE `title` = '校车最短路线卡在隐藏点位，BFS 还有哪些检查项？'
);

INSERT INTO `discussion_post` (
  `user_id`, `title`, `content`, `problem_id`, `view_count`, `like_count`, `audit_status`,
  `audit_user_id`, `audit_remark`, `audit_time`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_li_xuan,
  '教学楼温度波动这题用 DP 还是贪心更好？',
  '其实是标准最大连续子段和。虽然很多资料会写成 DP，但从实现角度看更像是用一个变量维护“以当前位置结尾的最优值”。如果是第一次写，建议先把转移方程写下来再压缩状态。',
  @pid_p4, 74, 0, 1,
  @uid_admin, '公开。', DATE_SUB(@seed_now, INTERVAL 2 DAY),
  0, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `discussion_post` WHERE `title` = '教学楼温度波动这题用 DP 还是贪心更好？'
);

INSERT INTO `discussion_post` (
  `user_id`, `title`, `content`, `problem_id`, `view_count`, `like_count`, `audit_status`,
  `audit_user_id`, `audit_remark`, `audit_time`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_qin_mo,
  '热身赛之后我想补基础实现题，大家有什么顺序建议？',
  '我发现自己基础题还是容易在边界和输入输出上丢分，想按“模拟 -> 字符串 -> 前缀和 -> BFS”的顺序补。老师或者学长学姐有没有更稳一点的练习路径？',
  NULL, 33, 0, 0,
  NULL, NULL, NULL,
  0, DATE_SUB(@seed_now, INTERVAL 6 HOUR), DATE_SUB(@seed_now, INTERVAL 6 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `discussion_post` WHERE `title` = '热身赛之后我想补基础实现题，大家有什么顺序建议？'
);

INSERT INTO `discussion_post` (
  `user_id`, `title`, `content`, `problem_id`, `view_count`, `like_count`, `audit_status`,
  `audit_user_id`, `audit_remark`, `audit_time`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_wu_tong,
  '直接贴完整代码求校车最短路线答案',
  '我懒得自己想了，谁能直接把这题 AC 代码发我一份？最好 C++ 和 Java 都给一下。',
  @pid_p6, 12, 0, 2,
  @uid_admin, '帖子内容不符合讨论区规范，已驳回。', DATE_SUB(@seed_now, INTERVAL 10 HOUR),
  0, DATE_SUB(@seed_now, INTERVAL 10 HOUR), DATE_SUB(@seed_now, INTERVAL 10 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `discussion_post` WHERE `title` = '直接贴完整代码求校车最短路线答案'
);

SET @post_prefix = (SELECT `id` FROM `discussion_post` WHERE `title` = '签到前缀统计为什么用前缀和比逐次求和稳定很多？' LIMIT 1);
SET @post_scholarship = (SELECT `id` FROM `discussion_post` WHERE `title` = '奖学金名单筛选的排序细节与常见失误' LIMIT 1);
SET @post_route = (SELECT `id` FROM `discussion_post` WHERE `title` = '校车最短路线卡在隐藏点位，BFS 还有哪些检查项？' LIMIT 1);
SET @post_dp = (SELECT `id` FROM `discussion_post` WHERE `title` = '教学楼温度波动这题用 DP 还是贪心更好？' LIMIT 1);
SET @post_pending = (SELECT `id` FROM `discussion_post` WHERE `title` = '热身赛之后我想补基础实现题，大家有什么顺序建议？' LIMIT 1);

INSERT INTO `discussion_post_like` (`post_id`, `user_id`, `create_time`)
VALUES
  (@post_prefix, @uid_demo_student, DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@post_prefix, @uid_li_xuan, DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@post_prefix, @uid_zhao_qi, DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@post_prefix, @uid_teacher, DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@post_prefix, @uid_qin_mo, DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@post_prefix, @uid_wu_tong, DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@post_scholarship, @uid_demo_student, DATE_SUB(@seed_now, INTERVAL 2 DAY)),
  (@post_scholarship, @uid_li_xuan, DATE_SUB(@seed_now, INTERVAL 2 DAY)),
  (@post_scholarship, @uid_sun_yi, DATE_SUB(@seed_now, INTERVAL 2 DAY)),
  (@post_scholarship, @uid_zhao_qi, DATE_SUB(@seed_now, INTERVAL 2 DAY)),
  (@post_route, @uid_teacher, DATE_SUB(@seed_now, INTERVAL 20 HOUR)),
  (@post_route, @uid_sun_yi, DATE_SUB(@seed_now, INTERVAL 20 HOUR)),
  (@post_route, @uid_li_xuan, DATE_SUB(@seed_now, INTERVAL 18 HOUR)),
  (@post_dp, @uid_demo_student, DATE_SUB(@seed_now, INTERVAL 30 HOUR)),
  (@post_dp, @uid_teacher, DATE_SUB(@seed_now, INTERVAL 30 HOUR))
ON DUPLICATE KEY UPDATE
  `create_time` = VALUES(`create_time`);

UPDATE `discussion_post` p
SET `like_count` = (
  SELECT COUNT(1) FROM `discussion_post_like` l WHERE l.`post_id` = p.`id`
)
WHERE p.`id` IN (@post_prefix, @post_scholarship, @post_route, @post_dp, @post_pending);

INSERT INTO `discussion_comment` (
  `post_id`, `user_id`, `parent_comment_id`, `content`, `deleted`, `create_time`, `update_time`
)
SELECT
  @post_prefix, @uid_demo_student, NULL,
  '我之前就是把区间和写成现算循环，数据一大马上超时。换成前缀和之后稳定很多。',
  0, DATE_SUB(@seed_now, INTERVAL 4 DAY), DATE_SUB(@seed_now, INTERVAL 4 DAY)
WHERE @post_prefix IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `discussion_comment`
    WHERE `post_id` = @post_prefix AND `user_id` = @uid_demo_student
      AND `content` = '我之前就是把区间和写成现算循环，数据一大马上超时。换成前缀和之后稳定很多。'
  );

INSERT INTO `discussion_comment` (
  `post_id`, `user_id`, `parent_comment_id`, `content`, `deleted`, `create_time`, `update_time`
)
SELECT
  @post_prefix, @uid_teacher, NULL,
  '补充一点：如果以后遇到二维统计，也可以继续往二维前缀和扩展。',
  0, DATE_ADD(DATE_SUB(@seed_now, INTERVAL 4 DAY), INTERVAL 20 MINUTE), DATE_ADD(DATE_SUB(@seed_now, INTERVAL 4 DAY), INTERVAL 20 MINUTE)
WHERE @post_prefix IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `discussion_comment`
    WHERE `post_id` = @post_prefix AND `user_id` = @uid_teacher
      AND `content` = '补充一点：如果以后遇到二维统计，也可以继续往二维前缀和扩展。'
  );

INSERT INTO `discussion_comment` (
  `post_id`, `user_id`, `parent_comment_id`, `content`, `deleted`, `create_time`, `update_time`
)
SELECT
  @post_scholarship, @uid_li_xuan, NULL,
  '我第一次 WA 就是只按姓名排序了，隐藏数据里同分选手很多，确实要注意主次关键字。',
  0, DATE_ADD(DATE_SUB(@seed_now, INTERVAL 2 DAY), INTERVAL 40 MINUTE), DATE_ADD(DATE_SUB(@seed_now, INTERVAL 2 DAY), INTERVAL 40 MINUTE)
WHERE @post_scholarship IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `discussion_comment`
    WHERE `post_id` = @post_scholarship AND `user_id` = @uid_li_xuan
      AND `content` = '我第一次 WA 就是只按姓名排序了，隐藏数据里同分选手很多，确实要注意主次关键字。'
  );

INSERT INTO `discussion_comment` (
  `post_id`, `user_id`, `parent_comment_id`, `content`, `deleted`, `create_time`, `update_time`
)
SELECT
  @post_route, @uid_teacher, NULL,
  '先检查两点：入队时是否立刻标记 visited，以及是否把终点 T 当作可走格子处理。',
  0, DATE_SUB(@seed_now, INTERVAL 18 HOUR), DATE_SUB(@seed_now, INTERVAL 18 HOUR)
WHERE @post_route IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `discussion_comment`
    WHERE `post_id` = @post_route AND `user_id` = @uid_teacher
      AND `content` = '先检查两点：入队时是否立刻标记 visited，以及是否把终点 T 当作可走格子处理。'
  );

SET @comment_route_teacher = (
  SELECT `id` FROM `discussion_comment`
  WHERE `post_id` = @post_route AND `user_id` = @uid_teacher
    AND `content` = '先检查两点：入队时是否立刻标记 visited，以及是否把终点 T 当作可走格子处理。'
  LIMIT 1
);

INSERT INTO `discussion_comment` (
  `post_id`, `user_id`, `parent_comment_id`, `content`, `deleted`, `create_time`, `update_time`
)
SELECT
  @post_route, @uid_demo_student, @comment_route_teacher,
  '收到，我回去重点查一下 visited 的时机，之前确实是在出队时才标记。',
  0, DATE_SUB(@seed_now, INTERVAL 17 HOUR), DATE_SUB(@seed_now, INTERVAL 17 HOUR)
WHERE @post_route IS NOT NULL
  AND @comment_route_teacher IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `discussion_comment`
    WHERE `post_id` = @post_route AND `user_id` = @uid_demo_student
      AND `content` = '收到，我回去重点查一下 visited 的时机，之前确实是在出队时才标记。'
  );

INSERT INTO `discussion_comment` (
  `post_id`, `user_id`, `parent_comment_id`, `content`, `deleted`, `create_time`, `update_time`
)
SELECT
  @post_dp, @uid_wu_tong, NULL,
  '我现在习惯先写转移，再把 dp 数组压成一个变量，读起来会更顺。',
  0, DATE_SUB(@seed_now, INTERVAL 28 HOUR), DATE_SUB(@seed_now, INTERVAL 28 HOUR)
WHERE @post_dp IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `discussion_comment`
    WHERE `post_id` = @post_dp AND `user_id` = @uid_wu_tong
      AND `content` = '我现在习惯先写转移，再把 dp 数组压成一个变量，读起来会更顺。'
  );

-- ------------------------------------------------------------
-- Social graph and messages
-- ------------------------------------------------------------

INSERT INTO `user_follow` (`follower_id`, `following_id`, `deleted`, `create_time`, `update_time`)
VALUES
  (@uid_demo_student, @uid_teacher, 0, DATE_SUB(@seed_now, INTERVAL 6 DAY), DATE_SUB(@seed_now, INTERVAL 6 DAY)),
  (@uid_demo_student, @uid_sun_yi, 0, DATE_SUB(@seed_now, INTERVAL 4 DAY), DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@uid_demo_student, @uid_li_xuan, 0, DATE_SUB(@seed_now, INTERVAL 4 DAY), DATE_SUB(@seed_now, INTERVAL 4 DAY)),
  (@uid_zhao_qi, @uid_demo_student, 0, DATE_SUB(@seed_now, INTERVAL 3 DAY), DATE_SUB(@seed_now, INTERVAL 3 DAY)),
  (@uid_wu_tong, @uid_demo_student, 0, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY)),
  (@uid_qin_mo, @uid_teacher, 0, DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE
  `deleted` = 0,
  `update_time` = VALUES(`update_time`);

INSERT INTO `private_message` (
  `from_user_id`, `to_user_id`, `content`, `read_flag`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_teacher, @uid_demo_student,
  '你这周在前缀和和 BFS 上的进度不错，周赛前把隐藏样例的边界再多测一轮。',
  0, 0, DATE_SUB(@seed_now, INTERVAL 3 HOUR), DATE_SUB(@seed_now, INTERVAL 3 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `private_message`
  WHERE `from_user_id` = @uid_teacher AND `to_user_id` = @uid_demo_student
    AND `content` = '你这周在前缀和和 BFS 上的进度不错，周赛前把隐藏样例的边界再多测一轮。'
    AND `create_time` = DATE_SUB(@seed_now, INTERVAL 3 HOUR)
);

INSERT INTO `private_message` (
  `from_user_id`, `to_user_id`, `content`, `read_flag`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_demo_student, @uid_teacher,
  '收到老师，我今晚会把最短路题的 visited 和终点判断再过一遍。',
  1, 0, DATE_SUB(@seed_now, INTERVAL 150 MINUTE), DATE_SUB(@seed_now, INTERVAL 150 MINUTE)
WHERE NOT EXISTS (
  SELECT 1 FROM `private_message`
  WHERE `from_user_id` = @uid_demo_student AND `to_user_id` = @uid_teacher
    AND `content` = '收到老师，我今晚会把最短路题的 visited 和终点判断再过一遍。'
    AND `create_time` = DATE_SUB(@seed_now, INTERVAL 150 MINUTE)
);

INSERT INTO `private_message` (
  `from_user_id`, `to_user_id`, `content`, `read_flag`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_sun_yi, @uid_demo_student,
  '我把编译告警过滤那题的栈写法整理了一版，等会儿发你看看。',
  0, 0, DATE_SUB(@seed_now, INTERVAL 100 MINUTE), DATE_SUB(@seed_now, INTERVAL 100 MINUTE)
WHERE NOT EXISTS (
  SELECT 1 FROM `private_message`
  WHERE `from_user_id` = @uid_sun_yi AND `to_user_id` = @uid_demo_student
    AND `content` = '我把编译告警过滤那题的栈写法整理了一版，等会儿发你看看。'
    AND `create_time` = DATE_SUB(@seed_now, INTERVAL 100 MINUTE)
);

INSERT INTO `private_message` (
  `from_user_id`, `to_user_id`, `content`, `read_flag`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_li_xuan, @uid_demo_student,
  '热身赛榜单我看了，你前两题都挺稳，后面可以多练下排序题。',
  1, 0, DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `private_message`
  WHERE `from_user_id` = @uid_li_xuan AND `to_user_id` = @uid_demo_student
    AND `content` = '热身赛榜单我看了，你前两题都挺稳，后面可以多练下排序题。'
    AND `create_time` = DATE_SUB(@seed_now, INTERVAL 1 DAY)
);

INSERT INTO `private_message` (
  `from_user_id`, `to_user_id`, `content`, `read_flag`, `deleted`, `create_time`, `update_time`
)
SELECT
  @uid_demo_student, @uid_li_xuan,
  '嗯，我这两天准备把奖学金名单筛选和温度波动都补完。',
  1, 0, DATE_SUB(@seed_now, INTERVAL 20 HOUR), DATE_SUB(@seed_now, INTERVAL 20 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `private_message`
  WHERE `from_user_id` = @uid_demo_student AND `to_user_id` = @uid_li_xuan
    AND `content` = '嗯，我这两天准备把奖学金名单筛选和温度波动都补完。'
    AND `create_time` = DATE_SUB(@seed_now, INTERVAL 20 HOUR)
);

-- ------------------------------------------------------------
-- Admin activity
-- ------------------------------------------------------------

INSERT INTO `admin_operation_log` (
  `operator_id`, `operator_username`, `module`, `action`, `target_type`, `target_id`, `detail`, `create_time`
)
SELECT
  @uid_admin, 'demo_admin', 'SYSTEM_CONFIG', 'UPDATE_ANNOUNCEMENT', 'CONFIG', NULL,
  '更新首页公告，补充本周题库、周赛与讨论区动态。', DATE_SUB(@seed_now, INTERVAL 2 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `admin_operation_log`
  WHERE `module` = 'SYSTEM_CONFIG' AND `action` = 'UPDATE_ANNOUNCEMENT'
    AND `detail` = '更新首页公告，补充本周题库、周赛与讨论区动态。'
    AND `create_time` = DATE_SUB(@seed_now, INTERVAL 2 DAY)
);

INSERT INTO `admin_operation_log` (
  `operator_id`, `operator_username`, `module`, `action`, `target_type`, `target_id`, `detail`, `create_time`
)
SELECT
  @uid_admin, 'demo_admin', 'CONTEST_MANAGE', 'PUBLISH_CONTEST', 'CONTEST', @contest_weekly,
  '发布进行中的春季周赛 Week 4，并开放报名。', DATE_SUB(@seed_now, INTERVAL 1 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM `admin_operation_log`
  WHERE `module` = 'CONTEST_MANAGE' AND `action` = 'PUBLISH_CONTEST'
    AND `target_id` = @contest_weekly
);

INSERT INTO `admin_operation_log` (
  `operator_id`, `operator_username`, `module`, `action`, `target_type`, `target_id`, `detail`, `create_time`
)
SELECT
  @uid_admin, 'demo_admin', 'FORUM_AUDIT', 'APPROVE_POST', 'DISCUSSION_POST', @post_route,
  '审核通过 BFS 求助帖，保留高价值提问。', DATE_SUB(@seed_now, INTERVAL 20 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `admin_operation_log`
  WHERE `module` = 'FORUM_AUDIT' AND `action` = 'APPROVE_POST'
    AND `target_id` = @post_route
);

INSERT INTO `admin_operation_log` (
  `operator_id`, `operator_username`, `module`, `action`, `target_type`, `target_id`, `detail`, `create_time`
)
SELECT
  @uid_admin, 'demo_admin', 'FORUM_AUDIT', 'REJECT_POST', 'DISCUSSION_POST',
  (SELECT `id` FROM `discussion_post` WHERE `title` = '直接贴完整代码求校车最短路线答案' LIMIT 1),
  '驳回索要现成答案的帖子，提醒遵守讨论区规范。', DATE_SUB(@seed_now, INTERVAL 9 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `admin_operation_log`
  WHERE `module` = 'FORUM_AUDIT' AND `action` = 'REJECT_POST'
    AND `detail` = '驳回索要现成答案的帖子，提醒遵守讨论区规范。'
);

INSERT INTO `admin_operation_log` (
  `operator_id`, `operator_username`, `module`, `action`, `target_type`, `target_id`, `detail`, `create_time`
)
SELECT
  @uid_admin, 'demo_admin', 'USER_MANAGE', 'RESET_PASSWORD', 'USER', @uid_qin_mo,
  '协助新用户恢复演示账号密码。', DATE_SUB(@seed_now, INTERVAL 6 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM `admin_operation_log`
  WHERE `module` = 'USER_MANAGE' AND `action` = 'RESET_PASSWORD'
    AND `target_id` = @uid_qin_mo
);

-- ------------------------------------------------------------
-- Recalculate problem statistics for seeded problems
-- ------------------------------------------------------------

UPDATE `problem`
SET
  `submit_count` = (SELECT COUNT(1) FROM `submission` s WHERE s.`problem_id` = `problem`.`id`),
  `ac_count` = (SELECT COUNT(1) FROM `submission` s WHERE s.`problem_id` = `problem`.`id` AND s.`status` = 'ACCEPTED'),
  `update_time` = @seed_now
WHERE `id` IN (@pid_p1, @pid_p2, @pid_p3, @pid_p4, @pid_p5, @pid_p6, @pid_p7, @pid_p8);
