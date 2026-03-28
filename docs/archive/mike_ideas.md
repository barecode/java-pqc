PQC  (clock) time service

First Bob prompt:
Build a plan to update the project to introduce a very simple REST/JSON client and REST/JSON server. The client should query the server with an HTTP GET and expect back a 200 response with JSON data. The server should respond with JSON data that indiciates the configured SSL cipher that its HTTPS port is listening on. The HTTPS port should be configurable via a main method argument which is passed in via a simple start server script.