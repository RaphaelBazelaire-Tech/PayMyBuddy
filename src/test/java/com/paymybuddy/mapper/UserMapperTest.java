package com.paymybuddy.mapper;

import com.paymybuddy.model.UserModel;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    public void toModelNullInputShouldReturnNull() {
        assertThat(mapper.toModel(null)).isNull();
    }

    @Test
    public void toModelEntityWithNoConnectionsShouldMapAllFieldsWithEmptyConnections() {
        UserEntity entity = createUser(1, "Alice", "alice@paymybuddy.com", "100.00");

        UserModel result = mapper.toModel(entity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getUsername()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@paymybuddy.com");
        assertThat(result.getBalance()).isEqualByComparingTo("100.00");
        assertThat(result.getConnections()).isEmpty();
    }

    @Test
    public void toModelEntityWithConnectionsShouldMapConnectionsAsShallow() {
        UserEntity bob = createUser(2, "Bob", "bob@paymybuddy.com", "50.00");
        UserEntity charlie = createUser(3, "Charlie", "charlie@paymybuddy.com", "75.00");

        UserEntity dave = createUser(99, "Dave", "dave@paymybuddy.com", "10.00");
        bob.getConnections().add(dave);

        UserEntity alice = createUser(1, "Alice", "alice@paymybuddy.com", "100.00");
        alice.setConnections(Set.of(bob, charlie));

        UserModel result = mapper.toModel(alice);

        assertThat(result.getConnections()).hasSize(2);
        assertThat(result.getConnections())
                .extracting(UserModel::getUsername)
                .containsExactlyInAnyOrder("Bob", "Charlie");

        assertThat(result.getConnections())
                .allSatisfy(connection ->
                        assertThat(connection.getConnections()).isEmpty());
    }

    @Test
    public void toModelShallowNullInputShouldReturnNull() {
        assertThat(mapper.toModelShallow(null)).isNull();
    }

    @Test
    public void toModelShallowValidEntityShouldMapSimpleFieldsAndIgnoreConnections() {
        UserEntity entity = createUser(1, "Alice", "alice@paymybuddy.com", "100.00");

        UserEntity bob = createUser(2, "Bob", "bob@paymybuddy.com", "50.00");
        entity.getConnections().add(bob);

        UserModel result = mapper.toModelShallow(entity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getUsername()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@paymybuddy.com");
        assertThat(result.getBalance()).isEqualByComparingTo("100.00");

        assertThat(result.getConnections()).isEmpty();
    }

    private UserEntity createUser(int id, String username, String email, String balance) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setBalance(new BigDecimal(balance));
        return user;
    }
}
