package pt.iscte.strudel.model.util

import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.impl.UnaryExpression

enum class UnaryOperator(val symbol: String) : IUnaryOperator {
    NOT("!") {
        override fun getResultType(exp: IExpression): IType {
            return BOOLEAN
        }

        override fun isValidFor(type: IType): Boolean {
            return type.isBoolean
        }
    },
    MINUS("-") {
        override fun getResultType(exp: IExpression): IType {
            return exp.type
        }

        override fun isValidFor(type: IType): Boolean {
            return type.isNumber
        }
    },
    TRUNCATE("(int)") {
        override fun getResultType(exp: IExpression): IType {
            return INT
        }

        override fun isValidFor(type: IType): Boolean {
            return type.isNumber
        }
    },
    PLUS("+") {
        override fun getResultType(exp: IExpression): IType {
            return exp.type
        }

        override fun isValidFor(type: IType): Boolean {
            return type.isNumber
        }
    };


    override fun on(exp: IExpression): IUnaryExpression {
        return UnaryExpression(this, exp)
    }

    abstract override fun getResultType(exp: IExpression): IType

    override fun setProperty(key: String, value: Any?) {
        check(false) {"unsupported"}
    }

    override fun getProperty(key: String): Any? {
        if(key == ID_PROP)
            return symbol
        check(false) {"unsupported"}
        return null
    }

    override fun cloneProperties(e: IProgramElement) {
        check(false) {"unsupported"}
    }

}