package com.jacknie.example.repository.account;

public enum AccountRole {

    ROLE_USER, ROLE_POSTER, ROLE_ADMIN;

    public String withoutRolePrefix() {
        return name().replaceFirst("ROLE_", "");
    }
}
