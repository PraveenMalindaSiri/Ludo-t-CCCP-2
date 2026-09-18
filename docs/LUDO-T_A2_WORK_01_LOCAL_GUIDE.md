# LUDO-T Work 01 - Local Integration and Validation Guide

> Current status: integration is complete on the `foundation` branch. The
> project has 89 passing tests and a clean working tree. Do not repeat the copy,
> movement or commit steps on the already-integrated repository. Keep the
> earlier sections only as a record of how the checkpoint was integrated.

Use this guide to bring the completed Phases 0-2 into the existing
`foundation` branch without replacing the repository's `.git` directory.

## 1. Keep the current repository

Continue using:

```text
D:\1. APIIT\4th (Final) Year\2nd sem\CCCP\Assignment\CB008920_CCCP2
```

Do not create another Git repository. The supplied ZIP is a source checkpoint,
not a replacement for your repository history.

## 2. Confirm the branch is safe

```powershell
Set-Location 'D:\1. APIIT\4th (Final) Year\2nd sem\CCCP\Assignment\CB008920_CCCP2'
git branch --show-current
git status
```

Continue only if the branch is `foundation` and the working tree is clean.

Optional Phase 0 tag:

```powershell
git tag phase-0-baseline
git push origin phase-0-baseline
```

## 3. Extract and copy the checkpoint

1. Extract `CB008920_CCCP2_WORK_01_PHASES_0_TO_2.zip` into a temporary folder.
2. Open the extracted `CB008920_CCCP2` folder.
3. In the existing Git repository, delete only the old top-level `src` folder.
   It has moved unchanged to `game-core/src`.
4. Copy all contents from the extracted `CB008920_CCCP2` folder into the
   existing repository and allow `pom.xml` to be replaced.
5. Do not delete or replace the existing `.git` folder.

The repository should now contain `game-core`, `protocol`, `server-app`,
`client-app`, `test-client`, `.mvn`, `mvnw` and `mvnw.cmd`.

## 4. Inspect before building

```powershell
git status
git diff --stat
git diff -- pom.xml
```

Expected changes include:

- deletion/movement of the old top-level `src` files;
- addition of `game-core/src` containing those same files;
- replacement of the root POM;
- addition of the five module POMs;
- addition of Maven Wrapper files;
- addition of protocol source and tests.

## 5. Reimport in IntelliJ IDEA

1. Open the repository root, not an individual module.
2. Open the Maven tool window.
3. Select **Reload All Maven Projects**.
4. Confirm IntelliJ shows these modules:
   - `game-core`
   - `protocol`
   - `server-app`
   - `client-app`
   - `test-client`
5. Confirm the Project SDK and Maven runner use Java 26.

The old root `Main.java` is now located at:

```text
game-core/src/main/java/Main.java
```

## 6. Run the authoritative build

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean test
```

The first wrapper run may download Maven 3.9.16. The first project build may
also download Jackson and Maven plugin dependencies.

Expected tests:

```text
game-core: 79 passed
protocol: 10 passed
total: 89 passed
failures/errors: 0
BUILD SUCCESS
```

If Maven uses the wrong Java version, check:

```powershell
java -version
javac -version
$env:JAVA_HOME
```

`JAVA_HOME` and the IntelliJ Maven runner should point to JDK 26.

## 7. Run the console regression

After the build:

```powershell
java -cp .\game-core\target\classes Main
```

Alternatively, run `game-core/src/main/java/Main.java` from IntelliJ. Confirm
that the automatic simulation completes and prints final placements.

## 8. Inspect dependency boundaries

```powershell
.\mvnw.cmd -pl game-core dependency:tree
.\mvnw.cmd -pl protocol dependency:tree
```

Confirm:

- `game-core` has no Jackson, Swing, socket or JDBC production dependency.
- `protocol` has Jackson but no dependency on `game-core`.
- `client-app` and `test-client` do not depend on `game-core`.
- only `server-app` depends on both `protocol` and `game-core`.

## 9. Completed Git checkpoint

The integration has already been committed and pushed. The recorded commits
are:

```text
e0a631e refactor: move existing LUDO-T implementation into game-core
e9f0968 add shared Jackson NDJSON protocol contracts
01704ff fix: track moved game-core and Maven module files
```

Do not run the staging commands below again on the completed repository. They
are retained only as historical integration guidance.

### Historical staging procedure

Stage the reactor, wrapper, module skeletons and mechanical source move:

```powershell
git add pom.xml .mvn mvnw mvnw.cmd game-core server-app client-app test-client protocol\pom.xml
git add -u
git commit -m "refactor: move existing LUDO-T implementation into game-core"
```

Then commit the protocol implementation:

```powershell
git add protocol\src
git commit -m "feat: add shared Jackson NDJSON protocol contracts"
```

Verify and push:

```powershell
git status
git log --oneline -3
git push
```

## 10. Final confirmed state

The following evidence was confirmed:

1. All five child modules are recognized by the root reactor.
2. The original game implementation is under `game-core`.
3. The shared DTO and NDJSON code is under the short `protocol` package.
4. `game-core` passed 79 tests.
5. `protocol` passed 10 tests.
6. The combined build passed all 89 tests with no failures or errors.
7. Commit `01704ff` was pushed and the working tree was clean.

Do not begin Work Chat 02 until this checkpoint has been reviewed.
