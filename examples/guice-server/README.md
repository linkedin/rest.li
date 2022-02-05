This is an example of using Guice 3 dependency injection with rest.li.

Please execute all commands below in the examples/guice-server folder

To build, use gradle 4.6 or greater.  If you need, you can run `../../gradlew wrapper` to generate a ./gradlew wrapper
in this sample directory that will use gradle 4.6.  If you do this, use `./gradlew` instead of `gradle` for the
remainder of this README.

Next, execute the following at the top level:

```
gradle publishRestliIdl
gradle build
```

The first line is required to initially propagate the pdsc and idl changes. Subsequent builds can be run with only `gradle build`

You can then run the server with:

`gradle JettyRunWar`

Once running, you can send a GET request to the server with:

`curl -v http://localhost:8080/fortunes/1`

or run the client with

`gradle startFortunesClient`