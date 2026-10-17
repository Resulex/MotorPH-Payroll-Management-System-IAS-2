# MotorPH Payroll Management System

Java Swing desktop payroll system backed by MySQL 8.0. This README documents the
reference architecture for the application and the security controls implemented
across the GUI, the DAO layer, and the database.

## Architecture

The diagram below is the reference architecture for the system.

<img width="693" height="1608" alt="MotorPH Workflow Diagram MS2 drawio" src="https://github.com/user-attachments/assets/fc74959a-e768-4fdb-bd91-a126dfe054ba" />


### Layer responsibilities

| Layer | Component | Responsibility |
|---|---|---|
| Actors | Admin / HR / Employee | Authenticated users of the desktop application |
| Application | MotorPH Java Desktop GUI | Login, dashboard, employee, attendance, leave and payroll screens |
| Application | Session Manager | Tracks activity and invalidates the session after 15 minutes of inactivity |
| Application | Application Modules | Coordinates user actions and calls the appropriate DAOs |
| Application | `config.properties` | Supplies database and e-mail credentials at runtime; never committed |
| Application | AuditLog DAO | Writes security events to `audit_logs` using parameterized SQL |
| Application | Payroll/Emp DAO | Reads and writes payroll, employee, attendance and leave data using parameterized SQL |
| Application | `DbConnection.java` | Single JDBC entry point; obtains credentials and opens connections |
| Database | MySQL 8.0 Server | Stores payroll data and security audit records |
| Database | Core Data Tables | Employees, attendance, leave, payroll, users and reference tables |
| Database | `audit_logs` Table | Persistent audit trail for logins, lockouts and role changes |

## Security controls

| Control | Implementation | Location |
|---|---|---|
| Secure credential management (R-03) | Credentials are resolved at runtime from environment variables or a git-ignored properties file. No credentials are stored in source. | `utils/AppConfig.java`, `config.properties.example`, `.gitignore` |
| Least privilege (R-12) | The application connects through the dedicated `motorph_app` MySQL account, which has DML-only access to the payroll schema. | MySQL DCL, `config.properties` |
| Persistent audit logging (R-04) | Successful logins, failed logins, account lockouts and role changes are written to `audit_logs`. | `dao/AuditLogDao.java`, `dao/UserDao.java`, `dao/SystemAdministratorDao.java`, `sql/r04_sec_audit_log.sql` |
| Session timeout | Fifteen minutes of inactivity invalidates the session and returns the user to the login screen. | `utils/SessionManager.java`, `gui/UserHomePage.java` |
| Parameterized queries | Every DAO uses `PreparedStatement` with `?` placeholders instead of string-built SQL. | `dao/*.java` |

## Configuration

Database and e-mail credentials are **not** stored in the source code. The
application resolves them in this order (first non-blank value wins):

1. Java system property with the same key, e.g. `-Ddb.password=...`
2. Environment variable
3. Local `config.properties` file, or the legacy `config/db.local.properties`

Property files may use either the canonical key (`db.user`) or the
environment-variable name (`MOTORPH_DB_USER`). `config.properties` takes
precedence when both files exist.

| Setting | Environment variable | Required |
|---|---|---|
| `db.url` | `MOTORPH_DB_URL` (alias `MYSQL_URL`) | no — defaults to `jdbc:mysql://localhost:3306/payrollsystem_db_aoop` |
| `db.user` | `MOTORPH_DB_USER` (alias `MYSQL_USER`) | **yes** |
| `db.password` | `MOTORPH_DB_PASSWORD` (alias `MYSQL_PASS`) | **yes** |
| `mail.from` | `MOTORPH_MAIL_USERNAME` (alias `MAIL_FROM`) | no |
| `mail.password` | `MOTORPH_MAIL_PASSWORD` (alias `MAIL_PASSWORD`) | no |
| `mail.host` | — | no — defaults to `smtp.gmail.com` |
| `mail.port` | — | no — defaults to `587` |

Set up the local file once:

```bash
cp config.properties.example config.properties
# then set db.user and db.password
```

Or export the values before starting the application:

```bash
export MOTORPH_DB_USER=motorph_app
export MOTORPH_DB_PASSWORD='your-password'
```

Use a file in another location with `-Dmotorph.config=/path/to/config.properties`.

Never put credentials in `src/main/resources`: resources are packaged into the
shaded JAR. `config.properties` and `config/db.local.properties` are git-ignored.

## Prerequisites

- Java 21
- Maven 3.10+
- MySQL 8.0.42 (Connector/J 8.4.0)
- The `payrollsystem_db_aoop` database restored from
  `payrollsystem_db_aoopDump20250716.sql`

## Database setup

Restore the base schema, apply the audit-log table, and create the
least-privilege application account:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS payrollsystem_db_aoop"
mysql -u root -p payrollsystem_db_aoop < payrollsystem_db_aoopDump20250716.sql
mysql -u root -p payrollsystem_db_aoop < sql/r04_sec_audit_log.sql
```

```sql
CREATE USER 'motorph_app'@'localhost' IDENTIFIED BY '<strong-password>';
GRANT SELECT, INSERT, UPDATE, DELETE ON payrollsystem_db_aoop.* TO 'motorph_app'@'localhost';
```

Set `db.user=motorph_app` and the same password in `config.properties`.

## Build and run

```bash
mvn clean verify            # compile, run tests and generate the JaCoCo report
mvn -DskipTests package     # build the shaded JAR
java -jar target/OOPMotorPhPayrollSystem-1.0-SNAPSHOT.jar
```

The GUI asks for an employee ID and password. The seeded accounts are in the
`user_credentials` table of the restored database.

## Verification

```bash
mvn -q -DskipTests compile                 # compile check
mvn test                                   # DAO tests require a live MySQL
bash tools/leakdemo/check-sql-safety.sh    # instant static SQL-safety check
bash tools/leakdemo/run-leak-demo.sh       # before/after JDBC resource-leak evidence
```

Expected results:

- No hardcoded credentials remain in tracked source files.
- SonarQube `java:S2068` / `java:S6437` findings are closed.
- The leak demo reports 1000 → 0 leaked statements and 600 → 0 leaked result sets.
- Missing database credentials produce a clear configuration error without
  printing the secret; missing e-mail credentials log a warning and skip the
  notification instead of failing the workflow.

## Project structure

```
src/main/java/
  alex/oopmotorphpayrollsystem/   application entry point
  dao/                            DAOs, AuditLogDao, shared mappers
  gui/                            Swing screens and session-timeout wiring
  models/                         domain models
  utils/                          AppConfig, DbConnection, EmailSender, SessionManager
src/main/resources/               reports, images and seed CSVs
sql/r04_sec_audit_log.sql         audit_logs table DDL
tools/leakdemo/                   before/after JDBC evidence harness
config.properties.example         local configuration template
```

## Email Notification Testing

For testing the HR team email notification, use Mailinator:

- https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=motorPHHRTeam#
- Inbox: `motorPHHRTeam`

For testing the Employee email notification, use Mailinator as well and replace
the employee's e-mail in the `user_credentials` table. A personal e-mail can also
be used after updating that table:

- https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=empMPH10008#
- Inbox: `empMPH10008`

# Email Notification Testing
For testing the HR team email notification. Use these 
Mailinator						
https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=motorPHHRTeam#						
motorPHHRTeam

For testing Employee email notification, you can use Mailinator too, and just replace the email of the employee fromthe  user_credentials table. You can also use your personal email after changing the value in the table.
Mailinator	
https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=empMPH10008#						
empMPH10008						
