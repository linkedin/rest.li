This server contains a variety of rest.li resources used integration testing.

How to run:

1. gradle startServer (CTRL+C to stop). Server is up when you see "Building > :rest-framework-server-examples:startServer". You can also run RestLiExamplesServer.main() from Eclipse/IntelliJ. Hit any key (or CTRL+c) to stop the server.
2. curl -v http://localhost:1338/greetings/1 to test the deployment
3a. You can also use a visual REST client like https://chrome.google.com/webstore/detail/baedhhmoaooldchehjhlpppaieoglhml
(might have to use chrome to use this)
4. Try running some of the sample HTTP requests under src/test/resources/sample-groups-requests.txt and sample-greetings-requests.txt
