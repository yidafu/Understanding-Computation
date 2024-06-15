package dev.yidafu.computation.simple


class Machine(var stat: Node, var env: Environment) {
    fun step() {
        val (stat, env) = stat.reduce(env)
        this.stat = stat
        this.env = env
    }

    fun run() {
        while (stat.reducible()) {
            println("$stat, $env")
            step()
        }
        println("$stat, $env")
    }
}