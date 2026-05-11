package com.paymybuddy.controller;

import com.paymybuddy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/add-connection")
public class AddConnectionController {

    private final UserService userService;

    @GetMapping
    public String page(Model model) {
        return "add-connection";
    }

    @PostMapping
    public String addConnection(@RequestParam String friendEmail,
                                Authentication auth,
                                RedirectAttributes ra) {

        try {
            userService.addConnection(auth.getName(), friendEmail);
            ra.addFlashAttribute("successMsg",
                    "Relation ajoutée avec succès !");

        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/add-connection";
    }
}
