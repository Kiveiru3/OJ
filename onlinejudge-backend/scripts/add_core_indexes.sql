-- Core indexes for OnlineJudge hot paths
-- Safe to run multiple times.

SET @db = DATABASE();

-- submission: user list and sorting by submit time
SET @exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 'submission'
      AND index_name = 'idx_submission_user_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_submission_user_ctime ON submission(user_id, create_time)',
              'SELECT ''idx_submission_user_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- submission: user list with status/language filters
SET @exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 'submission'
      AND index_name = 'idx_submission_user_status_lang_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_submission_user_status_lang_ctime ON submission(user_id, status, language, create_time)',
              'SELECT ''idx_submission_user_status_lang_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- submission: problem detail and teacher/admin problem submissions
SET @exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 'submission'
      AND index_name = 'idx_submission_problem_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_submission_problem_ctime ON submission(problem_id, create_time)',
              'SELECT ''idx_submission_problem_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- problem: public list filters
SET @exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 'problem'
      AND index_name = 'idx_problem_status_difficulty_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_problem_status_difficulty_ctime ON problem(status, difficulty, create_time)',
              'SELECT ''idx_problem_status_difficulty_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- test_case: judging loads cases by problem
SET @exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 'test_case'
      AND index_name = 'idx_test_case_problem'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_test_case_problem ON test_case(problem_id)',
              'SELECT ''idx_test_case_problem exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
