Prototype for a LinkedIn Groups REST API. Includes an experimental REST IDL for
 Groups along with data model definitions. To start:

1. gradle startServer (CTRL+C to stop). Server is up when you see "Building > :restli-example-server:startServer". You can also run RestLiExamplesServer.main() from Eclipse/IntelliJ. Hit any key (or CTRL+c) to stop the server.
2. curl -v http://localhost:7279/photos/1 to test the deployment
2a. You can also use a visual REST client like https://chrome.google.com/webstore/detail/baedhhmoaooldchehjhlpppaieoglhml
(might have to use chrome to use this)
