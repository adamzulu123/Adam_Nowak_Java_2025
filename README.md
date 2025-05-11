# Payment Optimizer

### Author: Adam Nowak

This application optimizes the way orders are paid using available payment methods and discounts.

---

## Technology Stack

- Java 21
- Maven
- Jackson (JSON processing)
- JUnit 5 (unit testing)
- Lombok (boilerplate reduction)
- Maven Assembly Plugin (for fat-jar packaging)

---

## Building the Application

> A pre-built fat JAR file is included in the provided `.zip` or `.tar.gz` archive. You do not need to build it yourself unless you want to modify the source code.

If you want to build the application manually, navigate to the project root and run:
```bash
mvn clean package
```


After the build completes, the output JAR file will be located in the `target/` directory:

target/Adam_Nowak_Java_2025-1.0-SNAPSHOT-jar-with-dependencies.jar


This JAR contains all necessary dependencies and is ready to run.

---

## Running the Application

The application requires two input JSON files:

1. A file containing a list of orders (e.g., `orders.json`)
2. A file containing a list of payment methods (e.g., `paymentmethods.json`)

To run the application manually from the command line:

#### java -jar target/Adam_Nowak_Java_2025-1.0-SNAPSHOT-jar-with-dependencies.jar <orders.json> <paymentmethods.json>


The result will be printed to the console.

---

## Running Tests

To execute all unit tests:

```bash
mvn test
```


Test coverage includes core optimization logic (`Optimizer`) and input parsing logic (`InputReader`), ensuring correctness and reliability.

---

## Directory Structure

- `src/main/java` – Application source code
- `src/test/java` – Unit tests
- `target/` – Build output (including the final JAR)

---

## Requirements

- Java 17 or Java 21
- Maven 3.6 or newer (optional if u want to recompile)

---


