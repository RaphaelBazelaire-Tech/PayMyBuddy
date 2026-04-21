package com.paymybuddy.controller;

import com.paymybuddy.repository.BankAccountRepository;
import com.paymybuddy.repository.model.BankAccount;
import com.paymybuddy.repository.model.User;
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
    private final BankAccountRepository bankAccountRepository;

    @GetMapping
    public String profilePage(Authentication authentication, Model model) {

        User currentUser = userService.findByEmail(authentication.getName());
        List<BankAccount> bankAccounts = bankAccountRepository.findByUser(currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("connections", currentUser.getConnections());
        model.addAttribute("bankAccounts", bankAccounts);

        return "profile";
    }

    @PostMapping("/add-connection")
    public String addConnection(@RequestParam String friendEmail,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        try {
            userService.addConnection(authentication.getName(), friendEmail);
            redirectAttributes.addFlashAttribute("successMsg", "Contact ajouté avec succès !");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/remove-connection/{id}")
    public String removeConnection(@PathVariable Integer id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

        try {
            userService.removeConnection(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("successMsg", "Contact supprimé.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam BigDecimal amount,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {

        try {
            userService.deposit(authentication.getName(), amount);
            redirectAttributes.addFlashAttribute("successMsg",
                    String.format("%.2f € déposés sur votre solde.", amount));

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping
    public String withdraw(@RequestParam Integer bankAccountId,
                           @RequestParam BigDecimal amount,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {

        try {
            userService.withdrawToBankAccount(authentication.getName(), bankAccountId, amount);
            redirectAttributes.addFlashAttribute("successMsg",
                    String.format("%.2f € virés vers votre compte bancaire.", amount));

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/add-bank-account")
    public String addBankAccount(@RequestParam String iban,
                                 @RequestParam String bic,
                                 @RequestParam String bankName,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        try {
            userService.addBankAccount(authentication.getName(), iban, bic, bankName);
            redirectAttributes.addFlashAttribute("successMsg", "Compte bancaire ajouté !");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/delete-bank-account/{id}")
    public String deleteBankAccount(@PathVariable Integer id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        try {
            userService.deleteBankAccount(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("successMsg", "Compte bancaire supprimé.");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/profile";
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
