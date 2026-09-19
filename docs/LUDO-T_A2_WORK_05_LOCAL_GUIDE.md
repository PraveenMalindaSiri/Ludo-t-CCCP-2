# Work 05 local MySQL verification guide

Use this guide after extracting the Work 05 ZIP on the Windows development machine.

## One-time setup

1. Install/start MySQL Community Server and optionally MySQL Workbench.
2. From the project root, create the two schemas and apply the tables:

   ```powershell
   Get-Content database\create-databases.sql | mysql -u root -p
   Get-Content database\schema.sql | mysql -u root -p ludot
   Get-Content database\schema.sql | mysql -u root -p ludot_test
   ```

3. Use the commented template in `database/create-databases.sql` to create `ludot_app`; replace
   the example password before running it and do not save the real password in Git.
4. Set the six `LUDOT_DB_*` and `LUDOT_TEST_DB_*` environment variables shown in the handoff.

## Automated checks

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
.\mvnw.cmd -Pmysql-it clean test
```

Expected result: the default suite passes with the MySQL class skipped; the profile runs and
passes all six `JdbcGameRepositoryIT` tests against `ludot_test`.

## Live check

```powershell
Get-Content database\reset-demo.sql | mysql -u ludot_app -p ludot
.\mvnw.cmd clean install
.\mvnw.cmd -pl server-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=server.ServerMain"
```

If the Maven Exec plugin is unavailable, run `server.ServerMain` from IntelliJ. Start two separate
`client.ClientMain` processes with the Work 04 guide, create a session, and exercise the controls.
Use `database/demo-queries.sql` in Workbench or the MySQL CLI to compare the displayed UUID with
the persisted rows.

For the interruption check, leave one session running, stop the server normally, restart it, and
query `game_sessions`. The old row must be `INTERRUPTED`; it must not be restored to the lobby.
