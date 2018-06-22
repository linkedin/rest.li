# Description
File to keep track of the used Error Codes and the relative exception.
Some exceptions can be thrown from multiple places. The stacktrace usually points out to core developers
why an exception was thrown in a specific instance, but users, often don't have knowledge of that
and is much easier for them report the issue or search in a wiki with an error instead of a stacktrace.

Format:
PEGA_XYZJ ExceptionName [, very high level description]
No error code can be repeated, and no error code can be referenced more than once in code

## D2 10
PEGA_1000 to PEGA_1016 ServiceUnavailableException
PEGA_1017 ServiceUnavailableException
PEGA_1030 ServiceUnavailableException

## R2 11

