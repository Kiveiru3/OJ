USE onlinejudge;

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(100) NOT NULL,
  `config_value` TEXT NULL,
  `description` VARCHAR(255) NULL,
  `update_user_id` BIGINT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT NULL,
  `operator_username` VARCHAR(64) NULL,
  `module` VARCHAR(64) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(64) NULL,
  `target_id` BIGINT NULL,
  `detail` TEXT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_aol_module_action` (`module`, `action`),
  KEY `idx_aol_operator` (`operator_id`),
  KEY `idx_aol_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
VALUES
('site.name', 'Online Judge', 'Website display name'),
('site.announcement', '', 'Homepage announcement'),
('contest.default_page_size', '20', 'Default contest ranking page size'),
('contest.default_penalty_per_wrong', '20', 'Default wrong submission penalty(minutes) for new contests')
ON DUPLICATE KEY UPDATE
`description` = VALUES(`description`);
