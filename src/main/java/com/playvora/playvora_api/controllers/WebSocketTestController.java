package com.playvora.playvora_api.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden  // Hide from Swagger documentation
public class WebSocketTestController {

    /**
     * Serves the WebSocket test page.
     * Access at: /websocket-test
     */
    @GetMapping("/websocket-test")
    public String websocketTest() {
        return "websocket-test";
    }
}

