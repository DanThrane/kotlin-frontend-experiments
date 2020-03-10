Thoughts
------------------------------------------------------------------------------------------------------------------------

- Most web applications deliver a database of some sorts
- The application enforces a set of rules on the dataset within it
- Most interesting applications will bridge multiple systems into a single one
- We need to design our applications for _change_
  - All code will become obsolete
- The interesting business logic is almost always a cross-cutting concern
  - Use clean separation when it does not sacrifice user experience or performance
  - We should _never_ sacrifice the user experience
    - Performance counts here too
  - When not possible to cleanly separate code:
    - Clearly mark code which do not belong in the core abstraction
    - Make it easy to delete code which is not part of the core abstraction
- We need fewer ways of doing common tasks
- We need flexibility in code - Avoid over-engineering.
- We need more predictable results
  - Performance: Results should be delivered with predictable performance
  - Errors: If an error is occurring because of the same root cause then it should result in the _same_ error
    - For example: Network failure should not cause exceptions of several different types (timeouts, socket failures,
      application timeouts etc. should all go to the same error).
- Avoid unnecessary complexity
  - Prefer transactions which do not span multiple systems
    - I am only counting the "moving parts" here (Systems which contain state?)
  - __Minimize the amount of layers__
  - __Minimize the number of abstractions__


Common problems in web applications
------------------------------------------------------------------------------------------------------------------------

Authentication
========================================================================================================================

Authentication should be fairly easy to generalize. Almost all systems could in theory share the same underlying
authentication system. The one I have already developed should be sufficient for the most part.

Logging
========================================================================================================================

I would say almost all web applications need two types of logging:

1. Structured audit log
2. Text logs for debugging

The second one is already implemented. We should keep it simple and just write the stdout/stderr. These are both
sufficient for many systems to pick up. Worst case we can implement file-logging. File rotations are probably best left
to some other system.

Networking and RPC
========================================================================================================================

All web applications require networking and RPC. This system should focus on being simple and predictable. It should
provide the following communication patterns:

1. One way requests (fire and forget)
2. Request-Response
3. Request-Subscription (multiple responses)

Load balancing is incredibly hard to get right. We should avoid the need for load balancing. It should be sufficient
with a floating-IP and direct connection to the server. This also makes performance significantly more predictable since
we only have a single layer.

Data Conversion and the RPC Protocol
========================================================================================================================

Most existing web frameworks silently ignore the fact that some payloads might be large. It is extremely common for
applications to silently push parts of a payload into a file and not tell you about the connection until all the data
has arrived. This makes it extremely like for timeouts to occur and is overall bad for performance.

To make the system more predictable we should enforce a maximum size on all RPC messages. This limit should probably
be around 64-256KB. If file uploads or similar are required then the payload should be split in smaller chunks.

The data format used for exchange should focus on being easy to serialize and deserialize. It must be possible for a
client to detect object boundaries even without an up-to-date schema.

The system needs to support automatic serialization and deserialization to and from `data classes`.

Database Access
========================================================================================================================

Almost all web applications could be generalized as an extra layer on top of a database. Database access is extremely
important for all web applications.

To avoid complexity we should pick a database and stick with it. It is extremely important for the user experience and
performance. It is an unrealistic dream to create an abstraction which allows you to switch database with no code
changes. It also seems extremely unlikely that one would ever switch database without changes to existing code.

Database queries should be written in plain SQL. It should always be obvious which queries/commands are executed on the
database server.

Data Conversion (Databases)
========================================================================================================================

The system needs to support automatic serialization and deserialization to and from `data classes`.

Caching
========================================================================================================================

Caching is probably too hard to really generalize.

Authorization
========================================================================================================================

Authorization is probably too hard to really generalize.

Integration with External Systems
========================================================================================================================

Requires good and predictable network code to speak to external systems.

It would also be nice with systems that aid in keeping state in-sync between multiple systems. This is likely too hard
to generalize.

Pagination
========================================================================================================================
