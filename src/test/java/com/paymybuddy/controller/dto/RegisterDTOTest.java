package com.paymybuddy.controller.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisterDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    public static void tearDown() {
        factory.close();
    }

    private RegisterDTO buildValidDto() {
        RegisterDTO dto = new RegisterDTO();

        dto.setUsername("JohnDoe");
        dto.setEmail("john@paymybuddy.com");
        dto.setPassword("secret123");
        dto.setConfirmPassword("secret123");
        return dto;
    }

    @Test
    public void validDtoShouldHaveNoViolations() {
        Set<ConstraintViolation<RegisterDTO>> violations = validator.validate(buildValidDto());
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "username = [{0}] doit être rejeté")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    public void usernameBlankOrNullShouldFail(String invalid) {
        RegisterDTO dto = buildValidDto();
        dto.setUsername(invalid);

        Set<ConstraintViolation<RegisterDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("username");

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Le nom d'utilisateur est obligatoire");
    }

    @Test
    public void usernameTooShortShouldFail() {
        RegisterDTO dto = buildValidDto();
        dto.setUsername("ab");

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("Le nom doit faire entre 3 et 50 caractères");
    }

    @Test
    public void usernameTooLongShouldFail() {
        RegisterDTO dto = buildValidDto();
        dto.setUsername("a".repeat(51));

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("Le nom doit faire entre 3 et 50 caractères");
    }

    @ParameterizedTest(name = "username de {0} caractères (borne) doit être accepté")
    @ValueSource(ints = {3, 50})
    public void usernameBoundaryValuesShouldPass(int length) {
        RegisterDTO dto = buildValidDto();
        dto.setUsername("a".repeat(length));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest(name = "email = [{0}] doit être rejeté")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    public void emailBlankOrNullShouldFail(String invalid) {
        RegisterDTO dto = buildValidDto();
        dto.setEmail(invalid);

        assertThat(validator.validate(dto))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("email");
    }

    @ParameterizedTest(name = "email = [{0}] est invalide")
    @ValueSource(strings = {"not-an-email", "missing@", "@nodomain.com", "no.at.sign"})
    public void emailInvalidFormatShouldFail(String invalid) {
        RegisterDTO dto = buildValidDto();
        dto.setEmail(invalid);

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("Le format d'email est invalide");
    }

    @ParameterizedTest(name = "password = [{0}] doit être rejeté")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    public void passwordBlankOrNullShouldFail(String invalid) {
        RegisterDTO dto = buildValidDto();
        dto.setPassword(invalid);

        assertThat(validator.validate(dto))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    public void passwordTooShortShouldFail() {
        RegisterDTO dto = buildValidDto();
        dto.setPassword("12345");

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("Le mot de passe doit faire au moins 6 caractères");
    }

    @Test
    public void passwordBoundaryMinShouldPass() {
        RegisterDTO dto = buildValidDto();
        dto.setPassword("123456");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest(name = "confirmPassword = [{0}] doit être rejeté")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    public void confirmPasswordBlankOrNullShouldFail(String invalid) {
        RegisterDTO dto = buildValidDto();
        dto.setConfirmPassword(invalid);

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("La confirmation est obligatoire");
    }

    @Test
    public void allFieldsNullShouldAccumulateViolations() {
        Set<ConstraintViolation<RegisterDTO>> violations = validator.validate(new RegisterDTO());
        assertThat(violations).hasSizeGreaterThanOrEqualTo(4);
    }
}
