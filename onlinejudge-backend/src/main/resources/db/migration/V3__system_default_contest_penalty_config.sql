-- Add default config for contest wrong submission penalty.

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
VALUES
('contest.default_penalty_per_wrong', '20', 'Default wrong submission penalty(minutes) for new contests')
ON DUPLICATE KEY UPDATE
`description` = VALUES(`description`);
