# Postman Collection to OpenAPI Converter

This project provides a Spring Boot application that automatically:

1. **Creates** a Postman Collection using the Postman API.
2. **Transforms** the collection into an OpenAPI specification.
3. **Deletes** the newly created Postman collection.
4. **Returns** the resulting OpenAPI specification to the client.

It handles errors gracefully, ensuring temporary Postman collections are always cleaned up, even if something goes wrong.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Prerequisites & Setup](#3-prerequisites--setup)
4. [Configuration](#4-configuration)
5. [Project Structure](#5-project-structure)
6. [Flow Explanation](#6-flow-explanation)
7. [How to Run](#7-how-to-run)
8. [Testing the Endpoints](#8-testing-the-endpoints)
9. [Example Request & Response](#9-example-request--response)

---

## 1.Overview

This application exposes a simple REST endpoint where you send a **Postman Collection** as JSON. The application:

- Creates the collection in Postman via `POST /collections`.
- Converts the collection to an OpenAPI specification using the `/transformations` endpoint.
- Deletes the collection from Postman.
- Returns the resulting **OpenAPI spec** to you.

If any step fails, the process is interrupted, and any successfully created collection is still cleaned up.

---

## 2. Architecture

The application is divided into four layers that work together seamlessly:

- **Controller** – Defines the HTTP endpoint to receive the Postman Collection.
- **Service** – Holds business logic to create, transform, and delete the collection.
- **Configuration** – Manages beans like `RestTemplate` and `ObjectMapper`, reads external properties from `application.properties`.
- **Exception Handling** – Captures and translates exceptions into consistent HTTP responses.

---

## 3. Prerequisites & Setup

- **Java 17 or later** (for Spring Boot compatibility)
- **Maven** to build the project
- **Postman API Key** from your Postman account

---

## 4. Configuration

In `src/main/resources/application.properties`, set:

```properties
postman.api.key=YOUR_POSTMAN_API_KEY
postman.api.baseUrl=https://api.getpostman.com
```
- `postman.api.key:` Your actual Postman API key.
- `postman.api.baseUrl:` The base URL for the Postman API calls.
---

## 5. Project Structure
```
.
├── pom.xml
├── src
│   └── main
│       └── java
│           └── com.example.postmanopenapi
│               ├── PostmanOpenapiApplication.java  # Main Spring Boot Application
│               ├── config
│               │   ├── ApiProperties.java          # Binds 'postman.api.*' properties
│               │   └── BeanConfig.java             # Defines RestTemplate & ObjectMapper beans
│               ├── controller
│               │   └── ApiController.java          # Defines the /api/convert endpoint
│               ├── dto
│               │   └── ResponseDTO.java            # Encapsulates OpenAPI spec in response
│               ├── exception
│               │   ├── ApiException.java           # Custom exception with HTTP status
│               │   └── GlobalExceptionHandler.java # Global error handler
│               └── service
│                   └── ApiService.java             # Core logic for create->transform->delete
└── ...
```
---
## 6. Flow Explanation

1. Client sends a POST request containing a Postman Collection JSON.

2. ApiService is called to create the collection in Postman.

3. ApiService transforms the collection into an OpenAPI spec.

4. ApiService then deletes the collection to avoid orphaned resources.

5. The final OpenAPI spec is returned to the client.

6. If an error occurs at any point, the error is handled by a custom exception (ApiException) and caught by the global exception handler.
---
## 7. How to Run

1. **Set Up:** Ensure your `application.properties` has the correct Postman API key and base URL.

2. **Build:** ```mvn clean install```

3. **Run:**```mvn spring-boot:run```

4. The application will start on port `8080` by default.

---
## 8. Testing the Endpoints

**Use Postman or cURL to send a POST request to:**
```http://localhost:8080/api/convert```

**with a request body containing the Postman Collection JSON. For example:**
```
curl --location --request POST 'http://localhost:8080/api/convert' \
--header 'Content-Type: application/json' \
--data-raw '{
  "collection": {
    "info": {
      "name": "Sample Collection",
      "description": "Convert this to OpenAPI",
      "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
      {
        "name": "Sample GET Request",
        "request": {
          "method": "GET",
          "url": "https://postman-echo/get"
        }
      }
    ]
  }
}'
```
---
## 9. Example Request & Response

**Request Body:**
```
{
  "collection": {
    "info": {
      "name": "Sample Collection",
      "description": "Convert this to OpenAPI",
      "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
      {
        "name": "Sample GET Request",
        "request": {
          "method": "GET",
          "header": [],
          "url": "https://postman-echo/get"
        }
      }
    ]
  }
}
```
**Response:**
```
{
  "openApiSpec": "{\n  \"openapi\": \"3.0.3\",\n  \"info\": { ... },\n  \"paths\": { ... }\n}"
}
```
---
