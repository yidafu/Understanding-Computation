package dev.yidafu.computation.simple

class Environment(vararg pairs: Pair<String, Expression>) : MutableMap<String, Expression> by mutableMapOf(*pairs) {
    constructor(map: Map<String, Expression>) : this() {
        map.entries.forEach { (key, value) ->
            this[key] = value
        }

    }
    override fun toString(): String {
        return "{${entries.joinToString(", ") { (key, value) -> ":${key} => ${value.inspect()}" }}}"
    }

    fun merge(other: Map<String, Expression>): Environment {
        return Environment((this + other))
    }

    fun merge(vararg piars: Pair<String, Expression>): Environment {
        return merge(mapOf(*piars))
    }
}

fun env(vararg pairs: Pair<String, Expression>) =Environment(*pairs)