package com.example.basiccompiler;

public class Jump extends RuntimeException {
    final Token token;
    Jump(Token token) {
        this.token = token;
    }
}
