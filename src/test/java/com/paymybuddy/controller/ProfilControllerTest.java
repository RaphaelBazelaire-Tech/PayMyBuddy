package com.paymybuddy.controller;

import com.paymybuddy.model.BankAccountModel;
import com.paymybuddy.model.UserModel;
import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProfileController.class)
@TestPropertySource(properties = "spring.thymeleaf.enabled=false")
public class ProfilControllerTest {

    private static final String CURRENT_USER_EMAIL = "alice@paymybuddy.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserModel currentUser;
    private Set<UserModel> connections;
    private List<BankAccountModel> bankAccounts;

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

        BankAccountModel bank = new BankAccountModel();
        bank.setId(1);
        bankAccounts = List.of(bank);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void profilePage_shouldReturnProfileViewWithAllModelAttributes() throws Exception {
        when(userService.findByEmail(CURRENT_USER_EMAIL)).thenReturn(currentUser);
        when(userService.getBankAccounts(CURRENT_USER_EMAIL)).thenReturn(bankAccounts);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", currentUser))
                .andExpect(model().attribute("connections", connections))
                .andExpect(model().attribute("bankAccounts", bankAccounts));

        verify(userService).findByEmail(CURRENT_USER_EMAIL);
        verify(userService).getBankAccounts(CURRENT_USER_EMAIL);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    void updateUsername_success_shouldCallServiceAndFlashSuccess() throws Exception {
        String newUsername = "AliceNouveau";

        mockMvc.perform(post("/profile/update-username")
                        .param("username", newUsername)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMsg", "Pseudonyme mis à jour !"))
                .andExpect(flash().attributeCount(1));

        verify(userService).updateUsername(CURRENT_USER_EMAIL, newUsername);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    void updateUsername_serviceThrowsRuntimeException_shouldFlashError() throws Exception {
        String errorMessage = "Ce pseudonyme est déjà utilisé.";
        doThrow(new RuntimeException(errorMessage))
                .when(userService).updateUsername(CURRENT_USER_EMAIL, "DéjàPris");

        mockMvc.perform(post("/profile/update-username")
                        .param("username", "DéjàPris")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("errorMsg", errorMessage))
                .andExpect(flash().attributeCount(1));

        verify(userService).updateUsername(CURRENT_USER_EMAIL, "DéjàPris");
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    void updateUsername_missingParam_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/profile/update-username")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    void updatePassword_success_shouldCallServiceAndFlashSuccess() throws Exception {
        mockMvc.perform(post("/profile/update-password")
                        .param("currentPassword", "oldPass123")
                        .param("newPassword", "newPass456")
                        .param("confirmPassword", "newPass456")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMsg", "Mot de passe mis à jour !"))
                .andExpect(flash().attributeCount(1));

        verify(userService).updatePassword(CURRENT_USER_EMAIL, "oldPass123", "newPass456", "newPass456");
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    void updatePassword_serviceThrowsRuntimeException_shouldFlashError() throws Exception {
        String errorMessage = "Le mot de passe actuel est incorrect.";
        doThrow(new RuntimeException(errorMessage))
                .when(userService).updatePassword(CURRENT_USER_EMAIL, "wrongOld", "newPass456", "newPass456");

        mockMvc.perform(post("/profile/update-password")
                        .param("currentPassword", "wrongOld")
                        .param("newPassword", "newPass456")
                        .param("confirmPassword", "newPass456")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("errorMsg", errorMessage))
                .andExpect(flash().attributeCount(1));

        verify(userService).updatePassword(CURRENT_USER_EMAIL, "wrongOld", "newPass456", "newPass456");
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    void updatePassword_missingParam_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/profile/update-password")
                        .param("newPassword", "newPass456")
                        .param("confirmPassword", "newPass456")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}
