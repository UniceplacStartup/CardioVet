package com.cardiovet.auth;

public interface PasswordResetMailer {

    void send(String email, String name, String token);
}
