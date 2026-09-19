# LUDO-T local MySQL setup

MySQL is a separate application tier. Only `server-app` contains JDBC code; the Swing client has
no database configuration or dependency.

## 1. Create schemas

From PowerShell in the project root, using an administrator account:

```powershell
Get-Content database\create-databases.sql | mysql -u root -p
Get-Content database\schema.sql | mysql -u root -p ludot
Get-Content database\schema.sql | mysql -u root -p ludot_test
```

Create the `ludot_app` account by copying the commented template from
`database/create-databases.sql`, replacing the example password, and running it as an
administrator. Never commit the real password.

## 2. Configure the server process

Set environment variables in the PowerShell window that will launch the server:

```powershell
$env:LUDOT_DB_URL = "jdbc:mysql://127.0.0.1:3306/ludot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:LUDOT_DB_USER = "ludot_app"
$env:LUDOT_DB_PASSWORD = "YOUR_LOCAL_PASSWORD"
```

Equivalent Java system properties are `database.url`, `database.user`, and
`database.password`. System properties take priority over environment variables, which take
priority over the redacted defaults in `server.properties`.

## 3. Reset and inspect demo data

```powershell
Get-Content database\reset-demo.sql | mysql -u ludot_app -p ludot
Get-Content database\demo-queries.sql | mysql -u ludot_app -p ludot
```

Reset removes all LUDO-T history rows and is intentionally a manual development/demo command.

## 4. MySQL integration tests

Prepare test-only environment variables, then enable the opt-in Maven profile:

```powershell
$env:LUDOT_TEST_DB_URL = "jdbc:mysql://127.0.0.1:3306/ludot_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:LUDOT_TEST_DB_USER = "ludot_app"
$env:LUDOT_TEST_DB_PASSWORD = "YOUR_LOCAL_PASSWORD"
Get-Content database\schema.sql | mysql -u ludot_app -p ludot_test
.\mvnw.cmd -Pmysql-it clean test
```

The test class refuses a URL that does not clearly target `ludot_test`. Default `clean test`
does not require a running database and skips only the MySQL integration class.

## Persistence boundary

The in-memory `GameSession` and `GameEngine` remain authoritative while the server is running.
MySQL stores session metadata and completed results only. It does not store individual game
events, request audits, or reconstruct the live object graph. On startup and orderly shutdown, unfinished
`CREATED`, `RUNNING`, or `PAUSED` rows are marked `INTERRUPTED`.
