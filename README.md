# Logging @ Toodalu

[CM Lubinski][0]

[CASE 12/16/10][1]

[Github][2]

---

## Problem Statement
API

* RESTful Web Service
* JSON input, JSON output
* Highly scalable

Logs

* Client in development
* Arbitrary queries

---

## Log Structure

* Handler: Path, HTTP method
* Input: JSON for POST/PUT, Query params for GET/DELETE
* Output: JSON body, Status code
* Metrics: Duration, Logged at (milliseconds)
* Client: IP Address, User Id

Examples:

`http://localhost:8080/api/users?first=Tom&first=Bill&first=Sue` becomes

    {
        "_id" : ObjectId("4d0a78f7fac0c9d99ff461a3"),
        "path" : "api/users",
        "method" : "GET",
        "input" : {
                "params" : {
                        "first" : [
                                "Tom",
                                "Bill",
                                "Sue"
                        ]
                }
        },
        "status_code" : 200,
        "output" : {
                "status" : "success",
                "users" : []
        },
        "duration" : 0,
        "logged_at" : 1292531959158,
        "ip" : "127.0.0.1",
        "user_id" : 1
    }

`curl -X POST http://localhost:8080/api/checkin -H "content-type: application/json" -d '{"location_id": 12345}'` becomes


    {
        "_id" : ObjectId("4d0a7b97fac0c9d9a0f461a3"),
        "path" : "api/checkin",
        "method" : "POST",
        "input" : {
                "params" : {
                        
                },
                "json_body" : {
                        "location_id" : 12345
                }
        },
        "status_code" : 200,
        "output" : {
                "status" : "success",
                "checkin_id" : 543
        },
        "duration" : 3,
        "logged_at" : 1292532631491,
        "ip" : "127.0.0.1",
        "user_id" : 1
    }

---

## [Dispatcher][3]

* Handles everything beginning with "api"
* Creates new log actor
* Dispatches to whatever resource
* Messages actor when done w/ status
* Return response to user

---

## [Log Actor][4]

* Listens for specific messages re: outcome
* Compiles fields from the request and the response
* Cleans the generated JObject
* Stores it

---

## Gotchas

* Lazy values
  * Log actor may be in a different thread than the handler
  * Several values of the request are lazy and get data from the thread
* Cleaning
  * MongoDB does not allow periods in field names
  * We don't want to store user passwords ... anywhere


[0]: http://cmlubinski.info "CM Lubinski"
[1]: http://www.meetup.com/chicagoscala/events/14793843/
[2]: https://github.com/cmc333333/loggingexample "Github"
[3]: https://github.com/cmc333333/loggingexample/blob/master/src/main/scala/loggingexample/lib/RestDispatch.scala "Dispatcher"
[4]: https://github.com/cmc333333/loggingexample/blob/master/src/main/scala/loggingexample/lib/RespLogger.scala "Logger"
