# PTAF - Unified Test Automation Framework

## Overview

The **PTAF** is an advanced, unified test automation framework designed for comprehensive end-to-end testing of modern applications. It seamlessly integrates **UI**, **API**, and **Database** testing into a single, cohesive platform.

It leverages industry-standard libraries like [Playwright](https://playwright.dev/) for browser and API testing, and [Java JDBC](https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html) for database connectivity. It uses [Cucumber](https://cucumber.io/) for implementing Behavior-Driven Development (BDD) and [JUnit](https://junit.org/junit5/) for test execution. The framework is built with flexibility, maintainability, and scalability in mind. Detailed reporting and easy-to-understand Gherkin scenarios allow technical and non-technical team members alike to participate in the quality assurance process.

The key aspects of **FNB PTAF** are:

* **Unified Testing**: Write and execute UI, API, and Database tests from a single project.
* **Extensive reporting** using ExtentReports for comprehensive HTML reports.
* **Environment-based configuration** for easy switching between environments without modifying core test logic.
* **Reusable utilities** and a layered architecture that makes test development faster and easier to maintain.

## Key Features

### 1. Web UI Automation

* **Cross-Browser Support**: Test across all major browsers (Chrome, Firefox, WebKit) using Playwright.
* **Advanced Locators**: A powerful "chained locator" strategy allows for precise and stable targeting of complex, nested web elements.
* **Automatic Screenshots**: Captures screenshots on test failure for quick and easy debugging.

### 2. API Automation

* **Full RESTful Support**: Test all HTTP methods (GET, POST, PUT, DELETE).
* **Stateful Request Building**: Construct complex API requests piece-by-piece using simple Gherkin steps.
* **Secure Authentication**: Handles API keys and tokens securely via environment variables.

### 3. Database Automation

* **Direct DB Interaction**: Connect directly to PostgreSQL or MS SQL Server to set up test data or verify application outcomes.
* **Secure & Reusable Queries**: Manages SQL queries in external YAML files and uses `PreparedStatement` to prevent SQL injection.

### 4. Cross-Cutting Features

* **Behavior-Driven Development (BDD)**: Cucumber and Gherkin make tests easy to read for everyone.
* **Environment-Based Configuration**: YAML files allow for easy switching between different environments.
* **Detailed Reporting**: Generates detailed HTML reports with pass/fail summaries.

## Project Structure

The project follows a well-organized directory structure that separates test logic from configuration and reports.

```plaintext
FNB-PTAF/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/ptaf/          # Core framework classes (handlers, performers, etc.)
│   │           ├── api/
│   │           ├── db/
│   │           └── ui/
│   │
│   └── test/
│       ├── java/
│       │   └── com/ptaf/
│       │       ├── runners/         # Test runners for UI, API, DB
│       │       └── stepdefinitions/ # Cucumber step definitions for UI, API, DB
│       └── resources/
│           ├── features/            # Cucumber feature files for all test types
│           ├── elements/            # YAML files for UI element locators
│           ├── queries/             # YAML files for Database queries
│           ├── api_requests/        # YAML files for API request definitions
│           └── config.yml           # Global configuration for URLs, DB, etc.
│
├── target/                          # Output directory for reports and logs
│
└── pom.xml                          # Maven configuration file
```

## Technologies Used

* **Playwright**: For Web UI and API automation.
* **Java JDBC**: For database connectivity.
* **Cucumber & JUnit**: For managing and running tests in a BDD style.
* **YAML & SnakeYAML**: For externalized configuration of locators, queries, and requests.
* **Maven**: For dependency management and build automation.
* **ExtentReports**: For rich, interactive HTML test reports.

## How to Use the Framework

### 1. Setup Your Environment

* Ensure you have Java (JDK 11+) and Maven installed.
* Clone the repository and run `mvn clean install` to download dependencies.
* **Crucially:** Configure your IDE's run configurations to include any necessary environment variables for passwords or API tokens, as defined in `config.yml`.

### 2. Writing UI Tests

This example shows how to click a button within a specific row of a table.

1. **Define Locators:** In a `.yml` file in `src/test/resources/elements/`, define your UI locators. Use the `>` syntax for chaining.
   ```yaml
   user_table:
     delete_jane_doe: "ROW_Jane Doe > BUTTON_Delete"
     login_btn: "BUTTON_Login"
   ```

2. **Create a Feature File:**
   ```gherkin
   @ui
   Feature: User Management
   
     Scenario: Delete a user from the table
       Given I am on the user management page
       When I click on user_table locator delete_jane_doe
       Then I should see a confirmation message
   ```

3. **Implement Step Definitions:** Use the steps already provided in `com.ptaf.stepdefinitions`.

### 3. Writing API Tests

This example demonstrates a full workflow: creating a resource, verifying it, and then cleaning it up.

1. **Configure Service:** In `src/test/resources/config.yml`, define the service.
   ```yaml
   api_services:
     jsonplaceholder:
       base_url: "[https://jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com)"
   ```

2. **Define Requests:** In a `.yml` file in `src/test/resources/api_requests/`, define the endpoints.
   ```yaml
   jsonplaceholder_requests:
     create_post:
       method: "POST"
       endpoint: "/posts"
     get_single_post:
       method: "GET"
       endpoint: "/posts/{postId}"
   ```

3. **Create a Feature File:**
   ```gherkin
   @api
   Feature: Blog Post Management API
   
     Scenario: Create and verify a new blog post
       Given I set the request body to
         """
         { "title": "My Awesome Post", "body": "This is a test." }
         """
       When I send a "jsonplaceholder_requests.create_post" request to the "jsonplaceholder" service
       Then the response code should be 201
       And the value of the JSON path "$.title" should be "My Awesome Post"
   ```

### 4. Writing Database Tests

This example shows a full CRUD (Create, Read, Update, Delete) workflow.

1. **Configure Connection:** In `src/test/resources/config.yml`, define your database connection.

2. **Define Queries:** In a `.yml` file in `src/test/resources/queries/`, write your reusable SQL queries.
   ```yaml
   test_crud:
     insert_test_user: "INSERT INTO test_framework_users (email, status) VALUES (?, ?);"
     get_user_by_email: "SELECT * FROM test_framework_users WHERE email = ?;"
   ```

3. **Create a Feature File:**
   ```gherkin
   @db
   Feature: Database User Management
   
     Scenario: Create and verify a new user
       # Prerequisite: Make sure the user does not exist
       Given the database does not contain a record for query "test_crud.get_user_by_email" with parameters "new.user@test.com"
   
       # 1. CREATE the user
       When I insert a new record using query "test_crud.insert_test_user" with parameters "new.user@test.com, PENDING"
   
       # 2. VERIFY the user was created
       Then I verify the database contains a record for query "test_crud.get_user_by_email" with parameters "new.user@test.com"
   ```

## How to Run Tests

### Prerequisites

* **Java**: Install [Java 11+]
* **Maven**: Install [Maven]
* **Playwright Browsers (for UI tests):** Run `mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"` to download browser binaries.

### Running Test Suites

The framework includes dedicated runner classes in `com.ptaf.runners`. To execute a suite, simply run the appropriate runner class from your IDE or via Maven.

* **`TestRunner.java`**: Runs UI tests (tagged `@ui` or untagged).
* **`ApiTestRunner.java`**: Runs API tests (tagged `@api`).
* **`DatabaseTestRunner.java`**: Runs Database tests (tagged `@db`).

### Running via Maven
```bash
# Run the UI test suite
mvn test -Dcucumber.options="--tags @ui"

# Run the API test suite
mvn test -Dcucumber.options="--tags @api"

# Run the Database test suite
mvn test -Dcucumber.options="--tags @db"
```

## Reporting

The framework integrates with **ExtentReports**, which generates rich, interactive HTML reports after each test execution.

Reports are stored in the `target/` directory:

* **HTML Report**: `target/cucumber-reports.html`, `target/api-cucumber-reports.html`, etc.
* **Screenshots**: Automatically captured for failed UI steps and embedded in the report.

## Conclusion

The **FNB PTAF** is designed to be a flexible, scalable, and easy-to-maintain unified testing solution. By leveraging its layered architecture and reusable components, teams can significantly improve the speed and reliability of testing across all layers of our applications, ensuring a higher standard of quality and faster feedback cycles.

