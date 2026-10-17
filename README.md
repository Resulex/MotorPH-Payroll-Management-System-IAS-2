# AOOPMotorPh_Payroll_System

## Configuration

Database and e-mail credentials are **not** stored in the source code. The application
resolves them at runtime, in this order (first non-blank value wins):

1. Java system property with the same key, e.g. `-Ddb.password=...`
2. Environment variable
3. Local `config.properties` file, or the legacy `config/db.local.properties` used by
   the R-04 audit logging - both are git-ignored and never packaged into the JAR

Property files may use either the canonical key (`db.user`) or the environment-variable
name (`MOTORPH_DB_USER`). A `config.properties` file takes precedence when both exist.

| Setting | Environment variable | Required |
|---|---|---|
| `db.url` | `MOTORPH_DB_URL` (alias `MYSQL_URL`) | no - defaults to `jdbc:mysql://localhost:3306/payrollsystem_db_aoop` |
| `db.user` | `MOTORPH_DB_USER` (alias `MYSQL_USER`) | **yes** |
| `db.password` | `MOTORPH_DB_PASSWORD` (alias `MYSQL_PASS`) | **yes** |
| `mail.from` | `MOTORPH_MAIL_USERNAME` (alias `MAIL_FROM`) | no |
| `mail.password` | `MOTORPH_MAIL_PASSWORD` (alias `MAIL_PASSWORD`) | no |
| `mail.host` | - | no - defaults to `smtp.gmail.com` |
| `mail.port` | - | no - defaults to `587` |

Set up a local file once:

```bash
cp config.properties.example config.properties
# then edit config.properties
```

Or export the values before starting the app:

```bash
export MOTORPH_DB_USER=root
export MOTORPH_DB_PASSWORD='your-password'
mvn exec:java
```

Use a file in another location with `-Dmotorph.config=/path/to/config.properties`.

Never put credentials in `src/main/resources`: resources are packaged into the shaded
JAR. When the e-mail settings are absent the app logs a warning and skips notifications
instead of failing the surrounding workflow.

# Email Notification Testing
For testing the HR team email notification. Use these 
Mailinator						
https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=motorPHHRTeam#						
motorPHHRTeam

For testing Employee email notification, you can use Mailinator too, and just replace the email of the employee fromthe  user_credentials table. You can also use your personal email after changing the value in the table.
Mailinator	
https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=empMPH10008#						
empMPH10008						
