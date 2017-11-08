# Akka HTTP microservice

A WIP akka-http test bed for Akka-Http, Akka-Persistence, Akka-Akpalka(Cassandra sink, Elastic sink), Kafka-Akka and Ehcache&Terracota for cache.

# https://medium.com/studioarmix/learn-restful-api-design-ideals-c5ec915a430f
It makes semantic sense when you request many posts from /posts not post/All
GET api.myservice.com/v1/posts
GET /v1/posts/:id/attachments/:id/comments
// preferred, shorter, no int/str mixing
GET /v1/me
GET /users/:id
“Health-Check” 
GET /v1
Query strings should be used for further filtering results beyond the initial grouping of a logical set offered by a relationship.
GET for fetching data.
POST for adding data.
PUT for updating data (as a whole object).
PATCH for updating data (with partial information for the object,auto-submit/auto-save fields).
DELETE for deleting data.
various vulnerabilities and potential hacks can occur if you do not envelope JSON arrays.

for Data Errors
400 for when the requested information is incomplete or malformed.
422 for when the requested information is okay, but invalid.
404 for when everything is okay, but the resource doesn’t exist.
409 for when a conflict of data exists, even with valid information.
for Auth Errors
401 for when an access token isn’t provided, or is invalid.
403 for when an access token is valid, but requires more privileges.
for Standard Statuses
200 for when everything is okay.
204 for when everything is okay, but there’s no content to return.
500 for when the server throws an error, completely unexpected.
If the email field is missing, return a 400 .
If the password field is too short, return a 422 .
If the email field isn’t a valid email, return a 422 .
If the email is already taken, return a 409 .

//Scaling
Services should be as stateless as possible,
Data access is the scalability bottleneck. 
Relying on other Services applications is another likely area of poor performance
Use “chunky” Web method calls.
Only send data that is required back and forth,to reduce cost of serialization
Use most effective pipeline (WS, HTTP, TCP, and named pipes).
Caching Session data and App data to reduce IO Bottlenecks
Caching data that you feel will not change for at least a few minutes to perhaps a few hours
Using distributed caching in between the service layer and the database, will improve performance and scalability of the service layer dramatically.
Removes server-side session that would have had to be propagated throughout your cluster

Expire cache by guessing or using messaging from dbase transaction.
use idle-time or sliding-time expiration to expire an item if nobody uses it for a given period.
service application is the only one updating the database and it can also easily update the cache, 
you probably don’t need the database-synchronization capability.
partitioned-replicated cache topology is ideal for a combination of scalability and high availability
Cache hit ratio,Cache population(pre-populating,Lazy cache population),

Enterprise Service Bus: Kafka, ESB is a simple and powerful way for multiple applications to share data asynchronously



