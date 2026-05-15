package com.paymybuddy.controller;

import com.paymybuddy.model.TransactionModel;
import com.paymybuddy.model.UserModel;
import com.paymybuddy.service.TransactionService;
import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@TestPropertySource(properties = "spring.thymeleaf.enabled=false")
public class HomeControllerTest {

    private static final String CURRENT_USER_EMAIL = "alice@paymybuddy.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionService transactionService;

    private UserModel currentUser;
    private Set<UserModel> connections;
    private List<TransactionModel> transactions;

    @BeforeEach
    public void setUp() {
        UserModel friend = new UserModel();
        friend.setId(2);
        friend.setUsername("Bob");
        friend.setEmail("bob@paymybuddy.com");

        currentUser = new UserModel();
        currentUser.setId(1);
        currentUser.setUsername("Alice");
        currentUser.setEmail(CURRENT_USER_EMAIL);
        currentUser.setBalance(new BigDecimal("100.00"));
        currentUser.setConnections(Set.of(friend));

        connections = currentUser.getConnections();

        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setId(10);
        transactionModel.setAmount(new BigDecimal("15.00"));
        transactionModel.setDescription("Pizza");
        transactionModel.setSender(currentUser);
        transactionModel.setReceiver(friend);
        transactions = List.of(transactionModel);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void homePageShouldReturnHomeViewWithAllModelAttributes() throws Exception {
        when(userService.findByEmail(CURRENT_USER_EMAIL)).thenReturn(currentUser);
        when(transactionService.getTransactionsBySender(CURRENT_USER_EMAIL)).thenReturn(transactions);

        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("user", currentUser))
                .andExpect(model().attribute("connections", connections))
                .andExpect(model().attribute("transactions", transactions))
                .andExpect(model().attributeExists("transferDTO"));

        verify(userService).findByEmail(CURRENT_USER_EMAIL);
        verify(transactionService).getTransactionsBySender(CURRENT_USER_EMAIL);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void transferValidDtoShouldCallServiceAndRedirect() throws Exception {
        mockMvc.perform(post("/home/transfer")
                        .param("receiverId", "2")
                        .param("amount", "25.50")
                        .param("description", "Remboursement")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("successMsg", "Virement effectué avec succès !"));

        verify(transactionService).transfer(
                eq(CURRENT_USER_EMAIL),
                eq(2),
                eq(new BigDecimal("25.50")),
                eq("Remboursement")
        );

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void transferWithoutDescriptionShouldCallServiceWithNullDescription() throws Exception {
        mockMvc.perform(post("/home/transfer")
                        .param("receiverId", "2")
                        .param("amount", "25.50")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("successMsg", "Virement effectué avec succès !"));

        verify(transactionService).transfer(
                eq(CURRENT_USER_EMAIL),
                eq(2),
                eq(new BigDecimal("25.50")),
                eq(null)
        );
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void transferInvalidDtoShouldStayOnHomeAndRehydrateModel() throws Exception {
        when(userService.findByEmail(CURRENT_USER_EMAIL)).thenReturn(currentUser);
        when(transactionService.getTransactionsBySender(CURRENT_USER_EMAIL)).thenReturn(transactions);

        mockMvc.perform(post("/home/transfer")
                        .param("receiverId", "")
                        .param("amount", "0")
                        .param("description", "Test")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeHasFieldErrors("transferDTO", "receiverId", "amount"))
                .andExpect(model().attribute("user", currentUser))
                .andExpect(model().attribute("connections", connections))
                .andExpect(model().attribute("transactions", transactions));

        verify(transactionService, Mockito.never())
                .transfer(ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyInt(),
                        ArgumentMatchers.any(BigDecimal.class),
                        ArgumentMatchers.any());

        verify(userService).findByEmail(CURRENT_USER_EMAIL);
        verify(transactionService).getTransactionsBySender(CURRENT_USER_EMAIL);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void transferServiceThrowsRuntimeExceptionShouldFlashErrorAndRedirect() throws Exception {
        String errorMessage = "Solde insuffisant.";
        doThrow(new RuntimeException(errorMessage))
                .when(transactionService).transfer(
                        eq(CURRENT_USER_EMAIL),
                        eq(2),
                        eq(new BigDecimal("999.99")),
                        eq("Trop cher")
                );

        mockMvc.perform(post("/home/transfer")
                        .param("receiverId", "2")
                        .param("amount", "999.99")
                        .param("description", "Trop cher")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("errorMsg", errorMessage));

        verify(transactionService).transfer(
                eq(CURRENT_USER_EMAIL),
                eq(2),
                eq(new BigDecimal("999.99")),
                eq("Trop cher")
        );

        verifyNoInteractions(userService);
    }
}
