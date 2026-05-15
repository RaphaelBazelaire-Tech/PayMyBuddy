package com.paymybuddy.controller.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferDTOTest {

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

    private TransferDTO buildValidDto() {
        TransferDTO dto = new TransferDTO();
        dto.setReceiverId(1);
        dto.setAmount(new BigDecimal("10.50"));
        dto.setDescription("Remboursement pizza");
        return dto;
    }

    @Test
    public void validDtoShouldHaveNoViolations() {
        assertThat(validator.validate(buildValidDto())).isEmpty();
    }

    @Test
    public void receiverIdNullShouldFail() {
        TransferDTO dto = buildValidDto();
        dto.setReceiverId(null);

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("Veuillez sélectionner un destinataire");
    }

    @Test
    public void amountNullShouldFail() {
        TransferDTO dto = buildValidDto();
        dto.setAmount(null);

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("Le montant est obligatoire");
    }

    @ParameterizedTest(name = "amount = {0} doit être rejeté")
    @ValueSource(strings = {"0", "0.00", "-0.01", "-1", "-100.50"})
    public void amountBelowMinShouldFail(String value) {
        TransferDTO dto = buildValidDto();
        dto.setAmount(new BigDecimal(value));

        assertThat(validator.validate(dto))
                .extracting(ConstraintViolation::getMessage)
                .contains("Le montant doit être supérieur à 0");
    }

    @ParameterizedTest(name = "amount = {0} doit être accepté")
    @ValueSource(strings = {"0.01", "1", "99.99", "1000000"})
    public void amountValidValuesShouldPass(String value) {
        TransferDTO dto = buildValidDto();
        dto.setAmount(new BigDecimal(value));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest(name = "description = [{0}] doit être accepté (optionnel)")
    @NullSource
    @ValueSource(strings = {"", "Une description", "   "})
    public void descriptionAnyValueShouldPass(String value) {
        TransferDTO dto = buildValidDto();
        dto.setDescription(value);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    public void emptyDtoShouldHaveTwoViolations() {
        Set<ConstraintViolation<TransferDTO>> violations = validator.validate(new TransferDTO());

        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("receiverId", "amount");
    }
}
