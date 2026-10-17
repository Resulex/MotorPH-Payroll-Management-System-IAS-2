# AOOPMotorPh_Payroll_System

# Email Notification Testing
For testing the HR team email notification. Use these 
Mailinator						
https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=motorPHHRTeam#						
motorPHHRTeam

For testing Employee email notification, you can use Mailinator too, and just replace the email of the employee fromthe  user_credentials table. You can also use your personal email after changing the value in the table.
Mailinator	
https://www.mailinator.com/v4/public/inboxes.jsp?msgid=motorphhrteam-1751808929-0124054333012&to=empMPH10008#						
empMPH10008						

## Local Database Setup

1. Create a local MySQL database named `payrollsystem_db_aoop` using the schema/setup SQL approved by the project team. **Team confirmation required:** this README does not identify an approved fresh-database setup process. Confirm which SQL and steps are approved before creating the database; do not assume an existing dump is appropriate.
2. Create or obtain an authorized MySQL account named `motorph_app` for local development. Do not grant global or administrative privileges. Grant only the schema-level privileges required by the application's actual database operations.
3. Copy `config/db.properties.example` to `config/db.local.properties`.
4. In `config/db.local.properties`, enter your own authorized local database username and password. Never add real credentials to this README or other committed files.
5. The application checks `MOTORPH_DB_USER`, `MOTORPH_DB_PASSWORD`, and the optional `MOTORPH_DB_URL` environment variables before reading the matching values from `config/db.local.properties`. Set these variables in the environment used to launch the application if you want them to override the local file.
6. Launch the application with the project root as its working directory. The path to `config/db.local.properties` is relative to the working directory.
7. `config/db.local.properties` is ignored by Git. Keep your local credentials there and do not commit the file.
