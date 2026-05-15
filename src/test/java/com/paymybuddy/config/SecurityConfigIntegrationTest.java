package com.paymybuddy.config;

import com.paymybuddy.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityConfigIntegrationTest {

    private static final String TEST_EMAIL = "user@paymybuddy.com";
    private static final String TEST_PASSWORD = "ValidPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void loginPageShouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(302));
    }

    @Test
    public void registerPageShouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(302));
    }

    @Test
    public void staticResourcesShouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(result -> {

                    int status = result.getResponse().getStatus();
                    String redirect = result.getResponse().getRedirectedUrl();

                    assertThat(status).isNotEqualTo(302);
                    assertThat(redirect == null || !redirect.contains("/login"))
                            .isTrue();
                });
    }

    @Test
    public void protectedEndpointShouldRedirectAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser
    public void protectedEndpointShouldNotRedirectAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/secured-no-controller-test"))
                .andExpect(status().isNotFound())
                .andExpect(authenticated());
    }

    @Test
    public void loginWithValidCredentialsShouldRedirectToHome() throws Exception {
        UserDetails user = User.withUsername(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .roles("USER")
                .build();

        when(userDetailsService.loadUserByUsername(TEST_EMAIL))
                .thenReturn(user);

        mockMvc.perform(post("/login")
                        .param("email", TEST_EMAIL)
                        .param("password", TEST_PASSWORD)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @Test
    public void loginWithWrongPasswordShouldRedirectToError() throws Exception {
        UserDetails user = User.withUsername(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .roles("USER")
                .build();

        when(userDetailsService.loadUserByUsername(TEST_EMAIL))
                .thenReturn(user);

        mockMvc.perform(post("/login")
                        .param("email", TEST_EMAIL)
                        .param("password", "WrongPassword")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    public void loginWithUnknownEmailShouldRedirectToError() throws Exception {

        when(userDetailsService.loadUserByUsername("ghost@nowhere.com"))
                .thenThrow(new UsernameNotFoundException("Utilisateur introuvable"));

        mockMvc.perform(post("/login")
                        .param("email", "ghost@nowhere.com")
                        .param("password", "irrelevant")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser
    void logout_shouldInvalidateSessionAndRedirect() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
    }
}
