package com.paymybuddy.controller;

import com.paymybuddy.controller.dto.RegisterDTO;
import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser
    public void loginPageNoParamsShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("errorMsg"))
                .andExpect(model().attributeDoesNotExist("logoutMsg"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser
    public void loginPageWithErrorParamShouldAddErrorMsg() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("errorMsg", "Email ou mot de passe incorrect."))
                .andExpect(model().attributeDoesNotExist("logoutMsg"));
    }

    @Test
    @WithMockUser
    public void loginPageWithLogoutParamShouldAddLogoutMsg() throws Exception {
        mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("logoutMsg", "Vous avez été déconnecté avec succès."))
                .andExpect(model().attributeDoesNotExist("errorMsg"));
    }

    @Test
    @WithMockUser
    public void loginPageWithBothParamsShouldAddBothMessages() throws Exception {
        mockMvc.perform(get("/login")
                        .param("error", "")
                        .param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("errorMsg", "Email ou mot de passe incorrect."))
                .andExpect(model().attribute("logoutMsg", "Vous avez été déconnecté avec succès."));
    }

    @Test
    @WithMockUser
    public void registerPageShouldReturnRegisterViewWithEmptyDto() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerDTO"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser
    public void registerValidDtoShouldCallServiceAndRedirectToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "JohnDoe")
                        .param("email", "john@paymybuddy.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMsg",
                        "Compte crée ! Vous pouvez maintenant vous connecter."));

        ArgumentCaptor<RegisterDTO> captor = ArgumentCaptor.forClass(RegisterDTO.class);
        verify(userService).register(captor.capture());

        RegisterDTO captured = captor.getValue();
        assertThat(captured.getUsername()).isEqualTo("JohnDoe");
        assertThat(captured.getEmail()).isEqualTo("john@paymybuddy.com");
        assertThat(captured.getPassword()).isEqualTo("secret123");
        assertThat(captured.getConfirmPassword()).isEqualTo("secret123");
    }

    @Test
    @WithMockUser
    public void registerInvalidDtoShouldStayOnRegisterAndNotCallService() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "")
                        .param("email", "not-an-email")
                        .param("password", "abc")
                        .param("confirmPassword", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerDTO",
                        "username", "email", "password", "confirmPassword"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser
    public void registerServiceThrowsRuntimeExceptionShouldStayOnRegisterWithErrorMsg() throws Exception {
        String errorMessage = "Cet email est déjà utilisé.";

        doThrow(new RuntimeException(errorMessage))
                .when(userService).register(any(RegisterDTO.class));

        mockMvc.perform(post("/register")
                        .param("username", "JohnDoe")
                        .param("email", "john@paymybuddy.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("errorMsg", errorMessage));

        verify(userService).register(any(RegisterDTO.class));
    }

    @Test
    @WithMockUser
    public void rootShouldRedirectToHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        verifyNoInteractions(userService);
    }
}
