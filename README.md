# Bridger SOAP Custom Client

Java 21 SOAP client for the LexisNexis Bridger Insight **12.1** Search API. The project reads search requests from a CSV file, invokes the Search service, writes CSV results, and can serialize SOAP responses as formatted XML for diagnostics.

## Requirements

- JDK 21
- Maven 3.6 or later
- Access to the configured Bridger Insight endpoint and valid API credentials

Verify the local tools:

```powershell
java -version
mvn -version
```

If `mvn` is not recognized on Windows, install Maven and add its `bin` directory to `PATH`, or use IntelliJ IDEA's bundled Maven as described below.

### Installing Maven on Windows

1. Install a JDK 21 distribution, then open a new PowerShell window and verify it:

   ```powershell
   java -version
   ```

2. Download the **Binary zip archive** from the [Apache Maven download page](https://maven.apache.org/download.cgi).
3. Extract the archive to a stable location without spaces, for example:

   ```text
   C:\Tools\apache-maven-<version>
   ```

4. Open **Start → Edit the system environment variables → Environment Variables** and add these system variables:

   | Variable | Value |
   |---|---|
   | `JAVA_HOME` | The JDK installation directory, for example `C:\Program Files\Java\jdk-21` |
   | `MAVEN_HOME` | The extracted Maven directory, for example `C:\Tools\apache-maven-<version>` |

5. Edit the system `Path` variable and add these entries:

   ```text
   %JAVA_HOME%\bin
   %MAVEN_HOME%\bin
   ```

6. Close every Command Prompt, PowerShell, and IntelliJ IDEA window, then open a new PowerShell window and verify:

   ```powershell
   java -version
   mvn -version
   ```

   `mvn -version` must report Java 21 and the expected Maven home directory.

If your organization uses a proxy or private artifact repository, configure it in `%USERPROFILE%\.m2\settings.xml` before the first build. Do not commit that file when it contains repository credentials.

## Project layout

```text
bridger-soap-customclient/
├── pom.xml
├── src/main/
│   ├── java/com/lexisnexis/bridger/
│   │   ├── client/
│   │   │   ├── BridgerSoapClient.java
│   │   │   ├── BridgerSoapClientApp121.java
│   │   │   └── BridgerSoapClientApp120_deprecated.java
│   │   ├── service/SearchRequestBuilder.java
│   │   └── util/
│   │       ├── ApplicationConfiguration.java
│   │       ├── CsvExporter.java
│   │       ├── CsvInputReader.java
│   │       └── XmlFormatter.java
│   └── resources/
│       ├── API12.1.wsdl
│       └── application.properties.example
└── target/generated-sources/cxf/       # Generated during Maven builds; do not edit
```

`BridgerSoapClientApp121` is the active application entry point. `BridgerSoapClientApp120_deprecated` is retained only for legacy 12.0 compatibility.
The runtime `input.csv` and generated output files are local data and are not included in the repository.

## Configuration

After copying `application.properties.example` to `src/main/resources/application.properties`, the client loads configuration in this order, with later files overriding earlier values:

1. The copy bundled on the classpath from `src/main/resources/application.properties`
2. `src/main/resources/application.properties` in the current working directory
3. `application.properties` in the current working directory
4. `BRIDGER_API_CLIENT_ID`, `BRIDGER_API_USER_ID`, and `BRIDGER_API_PASSWORD` environment variables, which override matching properties

Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties` for local defaults. Set credentials in the environment. For PowerShell, set them in the terminal that will run the client:

```powershell
$env:BRIDGER_API_CLIENT_ID = "YOUR_CLIENT_ID"
$env:BRIDGER_API_USER_ID = "YOUR_USER_ID"
$env:BRIDGER_API_PASSWORD = "YOUR_PASSWORD"
```

Never commit live credentials or passwords. Local `application.properties` files are ignored by Git.

## Build

Run the complete Maven build from the project root:

```powershell
mvn clean compile
```

The `cxf-codegen-plugin` runs in Maven's `generate-sources` phase. It creates the JAXB/CXF API model under:

```text
target/generated-sources/cxf
```

These are generated files. Do not add methods or other manual changes there, because Maven replaces them during the next build. XML formatting belongs in `XmlFormatter`, not a generated model class.

To create the executable dependency-inclusive JAR:

```powershell
mvn clean package
```

The resulting artifact is:

```text
target/bridger-soap-customclient-1.0.0-jar-with-dependencies.jar
```

## Run

Place `input.csv` in the application's current working directory, then run either from IntelliJ IDEA or with the packaged JAR:

```powershell
java -jar target\bridger-soap-customclient-1.0.0-jar-with-dependencies.jar
```

The 12.1 application reads `input.csv` from the working directory. With `bridger.output.csv.enabled=true`, it writes a timestamped `output_*.csv` file to that same directory. When CSV output is disabled, formatted XML responses are logged; individual XML response files may also be written below `output\xml_response_*.xml`.

## IntelliJ IDEA setup

1. Open the directory containing `pom.xml`, rather than opening an individual `.java` file.
2. When prompted, import it as a Maven project. Otherwise, open the Maven tool window and select **Reload All Maven Projects**.
3. Set **File → Project Structure → Project SDK** to JDK 21.
4. Under **Settings → Build, Execution, Deployment → Build Tools → Maven**, set **Maven home path** to **Bundled (Maven 3)** when Maven is not installed system-wide.
5. Confirm `src\main\java` is marked as a **Sources Root** and `target\generated-sources\cxf` as a **Generated Sources Root**. Reloading Maven normally configures both automatically.
6. In **Run → Edit Configurations**, run `com.lexisnexis.bridger.client.BridgerSoapClientApp121` with the `bridger-soap-customclient` module selected in **Classpath of module**.

Errors such as `package com.lexisnexis.bridger.service does not exist` or `cannot find symbol: class BridgerSoapClientApp121` generally mean that the current run/build is not using the Maven module or its source roots. Reimport Maven and build the complete project with `mvn clean compile`; do not compile an individual source file with `javac`.

## CSV input

The active application expects a header row followed by columns in this order:

```text
ClientReference,EntityType,FirstName,MiddleName,LastName,FullName,Street,City,State,Country,PostalCode,DOB,Citizenship,IDType,IDNumber
```

Example:

```csv
ClientReference,EntityType,FirstName,MiddleName,LastName,FullName,Street,City,State,Country,PostalCode,DOB,Citizenship,IDType,IDNumber
REF-001,INDIVIDUAL,John,M,Doe,John M Doe,123 Main Street,New York,NY,USA,10001,1980-01-01,USA,Account,ACC123456
```

## Main components

| Component | Responsibility |
|---|---|
| `BridgerSoapClient` | Configures the SOAP service and executes Search requests. |
| `BridgerSoapClientApp121` | Reads `input.csv`, creates requests, calls the API, and exports results. |
| `SearchRequestBuilder` | Builds generated `Search` request objects using a fluent API. |
| `CsvInputReader` | Opens CSV inputs with the expected encoding and parser configuration. |
| `CsvExporter` | Exports single and batch Search responses to CSV. |
| `XmlFormatter` | Marshals `SearchResults` into formatted XML without modifying generated classes. |
| `ApplicationConfiguration` | Loads and validates application properties. |

## Troubleshooting

| Symptom | Resolution |
|---|---|
| `mvn` is not recognized | Install Maven and add its `bin` directory to `PATH`, or configure IntelliJ's bundled Maven. |
| `package ... does not exist` | Reimport the Maven project and run `mvn clean compile`; verify the correct module is selected in the run configuration. |
| Generated classes are missing | Run `mvn clean compile`. Maven creates `target/generated-sources/cxf` from `src/main/resources/API12.1.wsdl`. |
| Required configuration property is missing | Copy `application.properties.example` to `application.properties`; set `BRIDGER_API_CLIENT_ID`, `BRIDGER_API_USER_ID`, and `BRIDGER_API_PASSWORD` for credentials. |
| SOAP request fails | Verify endpoint access, API credentials, and that request values conform to the Bridger API contract. |

## License

This project is intended for development use with the LexisNexis Bridger Insight API. Use remains subject to the applicable LexisNexis service agreement.
