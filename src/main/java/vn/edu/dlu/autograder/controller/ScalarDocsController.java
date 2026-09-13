package vn.edu.dlu.autograder.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScalarDocsController {

    @GetMapping(value = "/docs", produces = MediaType.TEXT_HTML_VALUE)
    public String scalarDocs() {
        return """
            <!doctype html>
            <html>
              <head>
                <title>AutoGrader API Documentation</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
              </head>
              <body>
                <script id="api-reference" data-url="/v3/api-docs"></script>
                <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
              </body>
            </html>
            """;
    }
}