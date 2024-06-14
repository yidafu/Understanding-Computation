package dev.yidafu.computation


class Machine(var expr: Expression, var env: Environment) {
    fun step() {
        val (expr, env) = expr.reduce(env)
        this.expr = expr
        this.env = env
    }

    fun run() {
        while (expr.reducible()) {
            println("$expr, $env")
            step()
        }
        println("$expr, $env")
    }
}