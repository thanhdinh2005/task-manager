package com.thanh.taskmanager.fixture;

import com.thanh.taskmanager.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.thanh.taskmanager.fixture.TestConstants.*;

public class UserFixture {
    private Long id = 1L;
    private String email = DEFAULT_EMAIL;
    private String password = DEFAULT_PASSWORD;
    private String fullName = DEFAULT_FULL_NAME;
    private String hashPassword = DEFAULT_ENCODED_PASSWORD;

    private PasswordEncoder passwordEncoder = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return rawPassword.toString();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword.toString().equals(encodedPassword);
        }
    };

    public static UserFixture aUser() {
        return new UserFixture();
    }

    public UserFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public UserFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserFixture withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserFixture withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public UserFixture withHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
        return this;
    }

    public UserFixture withPasswordEncoder(PasswordEncoder encoder) {
        this.passwordEncoder = encoder;
        return this;
    }

    public User build() {
        User user = User.register(email, password, fullName, passwordEncoder);

        if (id != null) {
            EntityTestUtils.withId(user, id);
        }

        if (hashPassword != null) {
            EntityTestUtils.withFileld(user,"hashPassword" ,hashPassword);
        }

        return user;
    }
}