package com.thanh.taskmanager.entity;

import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

@Table(name = "users")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String hashPassword;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private boolean active = true;

    //

    public static User register(
            String email,
            String password,
            String fullName,
            PasswordEncoder encoder
    ) {
        Assert.hasText(email, "Email must not be empty");
        Assert.notNull(password, "Password must not be empty");
        Assert.hasText(fullName, "Fullname must not be empty");

        User user = new User();

        user.email = email;
        user.fullName = fullName;
        user.active = true;
        user.hashPassword = encoder.encode(password);

        return user;
    }

    public void changePassword(
            String currentPassword,
            String newPassword,
            PasswordEncoder encoder
    ) {

        if (!encoder.matches(currentPassword, this.hashPassword)) {
            throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        if (encoder.matches(newPassword, this.hashPassword)) {
            throw new AppException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }

        this.hashPassword = encoder.encode(newPassword);
    }
}
