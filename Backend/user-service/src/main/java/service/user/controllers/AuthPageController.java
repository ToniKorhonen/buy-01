package service.user.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthPageController {

    @GetMapping("/login")
    public String login() {
        return "OK";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "OK";
    }
}