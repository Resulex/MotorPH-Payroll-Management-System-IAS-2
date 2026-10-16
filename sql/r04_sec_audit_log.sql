-- R-04-Sec incremental script for payrollsystem_db_aoop.
-- Apply this script manually. The application does not execute it.
-- payrollsystem_db_aoopDump20250716.sql is intentionally unchanged.
--
-- audit_logs has no foreign keys so a failed login for an unknown
-- employee ID can still be recorded. Do not store passwords,
-- password hashes, or secrets in details.

CREATE TABLE IF NOT EXISTS audit_logs (
  audit_id BIGINT NOT NULL AUTO_INCREMENT,
  event_type VARCHAR(64) NOT NULL,
  actor_employee_id INT NULL,
  subject_employee_id INT NULL,
  attempted_employee_id VARCHAR(64) NULL,
  details VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (audit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
