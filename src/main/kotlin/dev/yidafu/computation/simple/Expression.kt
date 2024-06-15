package dev.yidafu.computation.simple

interface Node {

    fun reducible(): Boolean

    fun reduce(env: Environment): Pair<Node, Environment>
}

interface Expression : Node {
    fun inspect(): String {
        return  "«${this}»"
    }

    override fun reduce(env: Environment): Pair<Expression, Environment>

    fun evaluate(env: Environment): Expression
}

interface Statement : Node {
    fun inspect(): String {
        return  "«${this}»"
    }

    fun evaluate(env: Environment): Environment
}


class Bool(val value: Boolean) : Expression {
    override fun reducible(): Boolean = false

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return Bool(value) to env
    }

    override fun evaluate(env: Environment): Expression {
        return this
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

    override fun evaluate(env: Environment): Expression {
        return this
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
            Add(leftE as Expression, right) to env2
        } else if (right.reducible()) {
            val (rightE, env2) =  right.reduce(env)
            Add(left, rightE as Expression) to env2
        } else{
            return Number((left as Number).value + (right as Number).value) to env
        }
    }

    override fun evaluate(env: Environment): Expression {
        return Number((left.evaluate(env) as Number).value + (right.evaluate(env) as Number).value)
    }

    override fun toString(): String {
        return "$left + $right"
    }
}

class Multiply(val left: Expression, val right: Expression) : Expression {
    override fun reducible(): Boolean = true
    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return if (left.reducible()) {
            val (leftE, env2) =  left.reduce(env)
            Multiply(leftE as Expression, right) to env2
        } else if (right.reducible()) {
            val (rightE, env2) =  right.reduce(env)
            Multiply(left, rightE as Expression) to env2
        } else{
            return Number((left as Number).value * (right as Number).value) to env
        }
    }

    override fun evaluate(env: Environment): Expression {
        return Number((left.evaluate(env) as Number).value * (right.evaluate(env) as Number).value)
    }
    override fun toString(): String {
        return "$left * $right"
    }
}


class LessThan(val left: Expression, val right: Expression) : Expression {
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

    override fun evaluate(env: Environment): Expression {
        return Bool((left.evaluate(env) as Number).value < (right.evaluate(env) as Number).value)
    }
}

class Variable(val name: String) : Expression {
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Expression, Environment> {
        return env[name]!! to env
    }

    override fun evaluate(env: Environment): Expression {
        return env[name]!!
    }

    override fun toString(): String {
        return name
    }

}

class DoNothing : Statement {
    override fun reducible(): Boolean = false

    override fun reduce(env: Environment): Pair<Node, Environment> {
        return DoNothing() to env
    }


    override fun evaluate(env: Environment): Environment {
        return env
    }
    override fun toString(): String {
        return "do-nothing"
    }
}

class Assign(val name: String, val expr: Expression) : Statement {
    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Node, Environment> {
        if (expr.reducible()) {
            val (result, env2) = expr.reduce(env)
            return Assign(name, result as Expression) to env2
        } else {
            return DoNothing() to env.merge(name to expr)
        }
    }

    override fun evaluate(env: Environment): Environment {
        return env.merge(name to expr.evaluate(env))
    }

    override fun toString(): String {
        return "$name = $expr"
    }
}

class If(val condition: Expression, val consequence: Statement, val alternative: Statement) : Statement {
    override fun evaluate(env: Environment): Environment {
        val bool = condition.evaluate(env) as Bool
        return if (bool.value) {
            consequence.evaluate(env)
        } else {
            alternative.evaluate(env)
        }
    }

    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Node, Environment> {
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

class Sequence(val first: Statement, val second: Statement): Statement {
    override fun evaluate(env: Environment): Environment {
        return second.evaluate(first.evaluate(env))
    }

    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Node, Environment> {
        return when (first) {
            is DoNothing -> {
               second to env
            }
            else -> {
                val reduced = first.reduce(env)
                Sequence(reduced.first as Statement, second) to reduced.second
            }
        }
    }

    override fun toString(): String {
        return "$first; $second"
    }

}

class While(val condition: Expression, val body: Statement): Statement {
    override fun evaluate(env: Environment): Environment {
        val bool = condition.evaluate(env) as Bool
        return if (bool.value) {
            evaluate(body.evaluate(env))
        } else {
            env
        }

    }

    override fun reducible(): Boolean = true

    override fun reduce(env: Environment): Pair<Node, Environment> {
        return If(condition, Sequence(body, this), DoNothing()) to env
    }

    override fun toString(): String {
        return "while ($condition) { $body }"
    }
}