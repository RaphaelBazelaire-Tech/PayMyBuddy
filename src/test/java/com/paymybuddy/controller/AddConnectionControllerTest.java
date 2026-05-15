package com.paymybuddy.controller;

import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AddConnectionController.class)
class AddConnectionControllerTest {

    private static final String CURRENT_USER_EMAIL = "alice@paymybuddy.com";
    private static final String FRIEND_EMAIL = "bob@paymybuddy.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void getPageAuthenticatedShouldReturnAddConnectionView() throws Exception {
        mockMvc.perform(get("/add-connection"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-connection"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void postSuccessShouldCallServiceAndFlashSuccess() throws Exception {
        mockMvc.perform(post("/add-connection")
                        .param("friendEmail", FRIEND_EMAIL)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/add-connection"))
                .andExpect(flash().attribute("successMsg", "Relation ajoutée avec succès !"))
                .andExpect(flash().attributeCount(1));

        verify(userService).addConnection(CURRENT_USER_EMAIL, FRIEND_EMAIL);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void postServiceThrowsRuntimeExceptionShouldFlashErrorMessage() throws Exception {
        String errorMessage = "Cet utilisateur est introuvable.";
        doThrow(new RuntimeException(errorMessage))
                .when(userService).addConnection(eq(CURRENT_USER_EMAIL), eq(FRIEND_EMAIL));

        mockMvc.perform(post("/add-connection")
                        .param("friendEmail", FRIEND_EMAIL)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/add-connection"))
                .andExpect(flash().attribute("errorMsg", errorMessage))
                .andExpect(flash().attributeCount(1));

        verify(userService).addConnection(CURRENT_USER_EMAIL, FRIEND_EMAIL);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void postMissingFriendEmailShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/add-connection").with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL)
    public void postWithoutCsrfShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/add-connection")
                        .param("friendEmail", FRIEND_EMAIL))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}
