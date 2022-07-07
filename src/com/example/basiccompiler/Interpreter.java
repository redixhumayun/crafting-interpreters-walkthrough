package com.example.basiccompiler;

public class Interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError err) {
            Lox.runtimeError(err);
        }
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;

            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                } else if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                else if (left instanceof String && right instanceof Double) {
                    return (String)left + right.toString();
                } else if (left instanceof Double && right instanceof String) {
                    return left.toString() + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");

            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0.0) {
                    throw new RuntimeError(expr.operator, "Cannot divide by zero on the RHS");
                }
                return (double)left / (double)right;

            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;

            case CONDITIONAL:
                //  assuming that in this situation, the left and right will be strings separated by
                //  __ because that's what the colon conditional case does
                String[] possibilities = right.toString().split("__");
                if ((Boolean)left == true) {
                    return possibilities[0];
                } return possibilities[1];

            case COLON:
                return String.valueOf(left) + "__" + String.valueOf(right);

        }
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG -> {
                return !isTruthy(right);
            }
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            }
        }
        return null;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private Boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers");
    }
}
