Prototype for a LinkedIn Groups REST API. Includes an experimental REST IDL for
 Groups along with data model definitions. To start:

1. (Optional) Install and start MongoDB on the default port: http://www.mongodb.org/display/DOCS/Quickstart+OS+X .
This is needed for the /groups example. For linux, I'd recommend downloading mongodb from http://www.mongodb.org/downloads
and following the rest of the quickstart for unix. If you want to pull it via yum, install the mongodb-server.
2. gradle startServer (CTRL+C to stop). Server is up when you see "Building > :rest-framework-server-examples:startServer". You can also run RestLiExamplesServer.main() from Eclipse/IntelliJ. Hit any key (or CTRL+c) to stop the server.
3. curl -v http://localhost:1338/greetings/1 to test the deployment
3a. You can also use a visual REST client like https://chrome.google.com/webstore/detail/baedhhmoaooldchehjhlpppaieoglhml
(might have to use chrome to use this)
4. Try running some of the sample HTTP requests under src/test/resources/sample-groups-requests.txt and sample-greetings-requests.txt
