package com.example.basiccompiler;

public class Token {
    /**
     * Example:
     * ==
     * TokenType -> EQUAL_EQUAL
     * String -> "=="
     * literal -> null
     * line -> 1
     */
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal + " " + line;
    }
}
