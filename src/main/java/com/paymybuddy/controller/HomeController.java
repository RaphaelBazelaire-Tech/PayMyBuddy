package com.paymybuddy.controller;

import com.paymybuddy.controller.dto.TransferDTO;
import com.paymybuddy.repository.model.Transaction;
import com.paymybuddy.repository.model.User;
import com.paymybuddy.service.TransactionService;
import com.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final TransactionService transactionService;

    @GetMapping
    public String homePage(Authentication authentication, Model model) {

        User currentUser = userService.findByEmail(authentication.getName());
        List<Transaction> transactions = transactionService.getTransactionsBySender(currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("connections", currentUser.getConnections());
        model.addAttribute("transactions", transactions);
        model.addAttribute("transferDTO", new TransferDTO());

        return "home";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferDTO") TransferDTO dto,
                           BindingResult result,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        User currentUser = userService.findByEmail(authentication.getName());

        if (result.hasErrors()) {

            model.addAttribute("user", currentUser);
            model.addAttribute("connections", currentUser.getConnections());
            model.addAttribute("transactions", transactionService.getTransactionsBySender(currentUser));

            return "home";
        }

        try {
            transactionService.transfer(
                    authentication.getName(),
                    dto.getReceiverId(),
                    dto.getAmount(),
                    dto.getDescription()
            );

            redirectAttributes.addFlashAttribute("successMsg", "Virement effectué avec succès !");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/home";
    }
}
