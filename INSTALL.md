# Install and run

## Requirements

- Java Development Kit (JDK) 21
- Apache Maven 3.9 or later
- Apache Tomcat 11

Confirm Java and Maven are available:

```powershell
java -version
mvn -version
```

## Build and test

Open a terminal in the project directory (the directory containing `pom.xml`) and run:

```powershell
mvn clean test
mvn clean package
```

The deployable WAR is created at `target/lost-found.war`.

## Deploy to Tomcat

1. Copy `target/lost-found.war` into Tomcat's `webapps` directory.
2. Start Tomcat using its `bin\startup.bat` script.
3. Open [http://localhost:8080/lost-found/](http://localhost:8080/lost-found/) in a browser.
4. Register a student account and sign in.

Tomcat deploys the WAR under the `/lost-found` context path. Stop Tomcat with `bin\shutdown.bat`.

## Data storage

The application creates `users.json`, `lost_items.json`, and `found_items.json` in `%USERPROFILE%\.campus-lost-found` by default (the Java user's home directory). To use another directory, set `LOST_FOUND_DATA_DIR` in Tomcat's environment before starting the server. For example, in PowerShell:

```powershell
$env:LOST_FOUND_DATA_DIR = 'C:\campus-lost-found-data'
& 'C:\path\to\tomcat\bin\startup.bat'
```

Ensure the account running Tomcat can write to the selected directory. Back up the data directory while Tomcat is stopped. Do not place live data in the public web root.

## Troubleshooting

- If Maven cannot find Java, install JDK 21 and configure `JAVA_HOME` and `PATH`.
- If the app does not load, check Tomcat's logs and confirm that the WAR deployed without errors.
- Do not expect existing MySQL records to be imported; this project uses JSON-file storage.
