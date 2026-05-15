package com.paymybuddy.mapper;

import com.paymybuddy.model.BankAccountModel;
import com.paymybuddy.repository.entity.BankAccountEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BankAccountMapperTest {

    private final BankAccountMapper mapper = new BankAccountMapper();

    @Test
    public void toModelNullInputShouldReturnNull() {
        assertThat(mapper.toModel(null)).isNull();
    }

    @Test
    public void toModelValidEntityShouldMapAllFields() {
        UserEntity owner = new UserEntity();
        owner.setId(42);

        BankAccountEntity entity = new BankAccountEntity();
        entity.setId(1);
        entity.setIban("FR7612345678901234567890123");
        entity.setBic("BNPAFRPPXXX");
        entity.setBankName("BNP Paribas");
        entity.setUser(owner);

        BankAccountModel result = mapper.toModel(entity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getIban()).isEqualTo("FR7612345678901234567890123");
        assertThat(result.getBic()).isEqualTo("BNPAFRPPXXX");
        assertThat(result.getBankName()).isEqualTo("BNP Paribas");
    }

    @Test
    public void toModelEntityWithNullFieldsShouldPropagateNulls() {
        BankAccountEntity entity = new BankAccountEntity();

        BankAccountModel result = mapper.toModel(entity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getIban()).isNull();
        assertThat(result.getBic()).isNull();
        assertThat(result.getBankName()).isNull();
    }
}
