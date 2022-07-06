package com.example.basiccompiler;

import java.util.List;

import static com.example.basiccompiler.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {};
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        Expr conditional = conditional();
        while (match(COMMA)) {
            Token operator = previous();
            Expr right = conditional();
            return right;
        }
        return conditional;
    }

    private Expr conditional() {
        Expr colonConditional = colonConditional();
        while (match(CONDITIONAL)) {
            Token operator = previous();
            Expr right = colonConditional();
            return new Expr.Binary(colonConditional, operator, right);
        }
        return colonConditional;
    }

    private Expr colonConditional() {
        Expr equality = equality();
        while(match(COLON)) {
            Token operator = previous();
            Expr right = equality();
            return new Expr.Binary(equality, operator, right);
        }
        return equality;
    }

    private Expr equality() {
        Expr comparison = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            return new Expr.Binary(comparison, operator, right);
        }
        return comparison;
    }

    private Expr comparison() {
        Expr term = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            return new Expr.Binary(term, operator, right);
        }
        return term;
    }

    private Expr term() {
        Expr factor = factor();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            return new Expr.Binary(factor, operator, right);
        }
        return factor;
    }

    private Expr factor() {
        Expr unary = unary();
        while(match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Binary(unary, operator, right);
        }
        return unary;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}
