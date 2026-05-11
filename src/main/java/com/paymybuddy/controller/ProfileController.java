package com.paymybuddy.controller;

import com.paymybuddy.model.BankAccountModel;
import com.paymybuddy.model.UserModel;
import com.paymybuddy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String profilePage(Authentication authentication, Model model) {

        UserModel currentUser = userService.findByEmail(authentication.getName());
        List<BankAccountModel> bankAccounts = userService.getBankAccounts(authentication.getName());

        model.addAttribute("user", currentUser);
        model.addAttribute("connections", currentUser.getConnections());
        model.addAttribute("bankAccounts", bankAccounts);

        return "profile";
    }

    @PostMapping("/update-username")
    public String updateUsername(@RequestParam String username,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        try {
            userService.updateUsername(authentication.getName(), username);
            redirectAttributes.addFlashAttribute("successMsg", "Pseudonyme mis à jour !");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/update-password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        try {
            userService.updatePassword(authentication.getName(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMsg", "Mot de passe mis à jour !");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
    }
}
