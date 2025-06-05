# Balancer

This service is responsible for balancing the load between the available instances of the 
[lambda executors](https://github.com/miet-lambda/lambda-executor).

List of available instances is stored in the `instances` table in the DB.

Before routing the request to the lambda executor, the balancer checks if the instance is available.
And also checks if there are sufficient money on the balance of the user.

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
|-------------------------------|----------------------------------------------------------------------|
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

## API Endpoints

The service provides a dynamic routing system for lambda functions with the following URL pattern:

```
/{service}/{lambda}
```

where:
- `service`: The name of the service containing the lambda function
- `lambda`: The specific lambda function name to execute

### Request Format
- Supports any HTTP method (GET, POST, PUT, DELETE, etc.)
- Query parameters are passed through to the lambda function
- Headers are forwarded to the lambda function
- Request body (if any) is forwarded as-is to the lambda function

### Response
The response will depend on various conditions:

- **200-599**: Success response from the lambda function with the original status code, headers, and body
- **400 Bad Request**: When service or lambda parameters are missing
- **404 Not Found**: When the specified lambda function doesn't exist
- **403 Forbidden**: When the user doesn't have sufficient balance
- **500 Internal Server Error**: When there's an error executing the lambda function

### Cost
Each lambda execution costs a fixed amount that will be deducted from the user's balance. The execution will be rejected if the user's balance is insufficient.


