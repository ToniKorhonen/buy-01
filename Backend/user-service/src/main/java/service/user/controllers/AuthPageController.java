package service.user.controllers;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
@RestController
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthPageController {
        return "OK";
    @GetMapping("/login")
    public String login() {
        return "login";
    public String registerForm() {
        return "OK";
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
}