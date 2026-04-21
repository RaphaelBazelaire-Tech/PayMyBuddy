package com.paymybuddy.controller;

import com.paymybuddy.controller.dto.RegisterDTO;
import com.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {

        if (error != null) {
            model.addAttribute("errorMsg", "Email ou mot de passe incorrect.");
        }

        if (logout != null) {
            model.addAttribute("logoutMsg", "Vous avez été déconnecté avec succès.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.register(dto);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Compte crée ! Vous pouvez maintenant vous connecter.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}
