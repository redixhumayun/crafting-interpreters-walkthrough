package com.example.basiccompiler;

public class RPNPrinter implements Expr.Visitor<String> {
    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Binary(
                        new Expr.Literal(1),
                        new Token(TokenType.PLUS, "+", null, 1),
                        new Expr.Literal(2)

                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(
                        new Expr.Literal(4),
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(3)
                )
        );
        System.out.println(new RPNPrinter().print(expression));
    }

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return createNotation(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return null;
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return null;
    }

    @Override
    public String visitAnonymousFunctionExpr(Expr.AnonymousFunction expr) {
        return null;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return null;
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return null;
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return null;
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return createNotation(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return null;
    }

    private String createNotation(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        for (Expr expr : exprs) {
            builder.append(expr.accept(this));
            builder.append(" ");
        }
        builder.append(name);
        return builder.toString();
    }
}
