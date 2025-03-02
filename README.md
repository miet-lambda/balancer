# Balancer

This service is responsible for balancing the load between the available instances of the 
[lambda executors](https://github.com/miet-lambda/lambda-executor).

List of available instances is stored in the `instances` table in the DB.

Before routing the request to the lambda executor, the balancer checks if the instance is available.
And also checks if there are sufficient money on the balance of the user.


