package com.assignment.analytics.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards client-side routes to the SPA shell so deep links work when the
 * React build is served from the boot jar.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/login", "/customers", "/customers/**"})
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
