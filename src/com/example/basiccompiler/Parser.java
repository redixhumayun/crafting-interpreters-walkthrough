package com.example.basiccompiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.basiccompiler.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {};
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(FOR)) {
            return forStatement();
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'");
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }

        consume(RIGHT_PAREN, "Expect ')' after for clauses");

        Stmt body = statement();
        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)
                    )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition");
        Stmt thenStatement = statement();
        Stmt elseStatement = null;
        if (match(ELSE)) {
            elseStatement = statement();
        }
        return new Stmt.If(condition, thenStatement, elseStatement);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ; after statement");
        return new Stmt.Print(value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after while condition");
        Stmt whileBody = statement();
        return new Stmt.While(condition, whileBody);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expression = expression();
        consume(SEMICOLON, "Expect ; after expression.");
        return new Stmt.Expression(expression);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = equality();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target");
        }
        return expr;
    }

    private Expr or() {
        Expr left = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            return new Expr.Logical(left, operator, right);
        }
        return left;
    }

    private Expr and() {
        Expr left = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            return new Expr.Logical(left, operator, right);
        }
        return left;
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
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
