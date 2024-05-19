package ru.ndobryukha.otus.highload.demo.domain.mapper;


import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.ndobryukha.otus.highload.demo.domain.model.User;

@Mapper
public interface UserMapper {

    @Mapping(target = "password", qualifiedByName = "encodePassword")
    User map(ru.ndobryukha.otus.highload.demo.rest.model.UserRegisterPostRequest request,
             @Context PasswordEncoder encoder);

    @Named("encodePassword")
    default String encodePassword(String password, @Context PasswordEncoder encoder) {
        return encoder.encode(password);
    }

    ru.ndobryukha.otus.highload.demo.rest.model.User map(User user);

}
