package com.paymybuddy.mapper;

import com.paymybuddy.model.UserModel;
import com.paymybuddy.repository.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserModel toModel(UserEntity userEntity) {
        if (userEntity == null) return null;

        Set<UserModel> connectionModels = userEntity.getConnections().stream()
                .map(this::toModelShallow)
                .collect(Collectors.toSet());

        return UserModel.builder()
                .id(userEntity.getId())
                .username(userEntity.getUsername())
                .email(userEntity.getEmail())
                .balance(userEntity.getBalance())
                .connections(connectionModels)
                .build();
    }

    public UserModel toModelShallow(UserEntity userEntity) {
        if (userEntity == null) return null;

        return UserModel.builder()
                .id(userEntity.getId())
                .username(userEntity.getUsername())
                .email(userEntity.getEmail())
                .balance(userEntity.getBalance())
                .build();
    }
}
