package dev.yidafu.computation
interface Expression {
    fun inspect(): String {
        return  "«${this}»"
    }

    fun reducible(): Boolean

    fun reduce(env: Environment): Pair<Expression, Environment>
}

interface Statement : Expression


class Bool(val value: Boolean) : Expression{
    override fun reducible(): Boolean = false

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return Bool(value) to env
    }

    override fun toString(): String {
        return value.toString()
    }
}

class Number(val value: Int) : Expression {
    override fun reducible(): Boolean = false
    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return Number(value) to env
    }

    override fun toString(): String {
        return value.toString()
    }
}

class Add(val left: Expression, val right: Expression) : Expression {
    override fun reducible(): Boolean = true
    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return if (left.reducible()) {
            val (leftE, env2) =  left.reduce(env)
            Add(leftE, right) to env2
        } else if (right.reducible()) {
            val (rightE, env2) =  right.reduce(env)
            Add(left, rightE) to env2
        } else{
            return Number((left as Number).value + (right as Number).value) to env
        }
    }

    override fun toString(): String {
        return "$left + $right"
    }
}

class Multiply(val left: Expression, val right: Expression) : Expression{
    override fun reducible(): Boolean = true
    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return if (left.reducible()) {
            val (leftE, env2) =  left.reduce(env)
            Multiply(leftE, right) to env2
        } else if (right.reducible()) {
            val (rightE, env2) =  right.reduce(env)
            Multiply(left, rightE) to env2
        } else{
            return Number((left as Number).value * (right as Number).value) to env
        }
    }

    override fun toString(): String {
        return "$left * $right"
    }
}


class LessThan(val left: Expression, val right: Expression) : Expression{
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return if (left.reducible()) {
            val (leftE, env2) =  left.reduce(env)
            LessThan(leftE, right) to env2
        } else if (right.reducible()) {
            val (rightE, env2) =  right.reduce(env)
            LessThan(left, rightE) to env2
        } else{
            return Bool((left as Number).value < (right as Number).value) to env
        }
    }

    override fun toString(): String {
        return "$left < $right"
    }


}

class Variable(val name: String) :Expression {
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return env[name]!! to env
    }

    override fun toString(): String {
        return name
    }

}

class DoNothing : Expression {
    override fun reducible(): Boolean = false

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return DoNothing() to env
    }

    override fun toString(): String {
        return "do-nothing"
    }
}

class Assign(val name: String, val expr: Expression) : Statement {
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        if (expr.reducible()) {
            val (result, env2) = expr.reduce(env)
            return Assign(name, result) to env2
        } else {
            return DoNothing() to env.merge(name to expr)
        }
    }

    override fun toString(): String {
        return "$name = $expr"
    }
}

class If(val condition: Expression, val consequence: Expression, val alternative: Expression) : Statement {
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        if (condition.reducible()) {
            val result = condition.reduce(env)
            return If(result.first, consequence, alternative) to result.second
        }
        if (condition is Bool) {
            return if (condition.value) {
                consequence to env
            } else {
                alternative to env
            }
        }
        throw Exception("unreachable")
    }

    override fun toString(): String {
        return "if ($condition) { $consequence } else { $alternative }"
    }
}

class Sequence(val first: Expression, val second: Expression): Expression {
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return when (first) {
            is DoNothing -> {
               second to env
            }
            else -> {
                val reduced = first.reduce(env)
                Sequence(reduced.first, second) to reduced.second
            }
        }
    }

    override fun toString(): String {
        return "$first; $second"
    }

}

class While(val condition: Expression, val body: Expression): Statement {
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return If(condition, Sequence(body, this), DoNothing()) to env
    }

    override fun toString(): String {
        return "while ($condition) { $body }"
    }
}