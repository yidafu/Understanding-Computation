import dev.yidafu.computation.*
import dev.yidafu.computation.Number
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.system.captureStandardOut
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ExpressionText : ShouldSpec({
    context("Manual Build AST") {
        should("Number(5) inspect result is «5»") {
            Number(5).inspect() shouldBe "«5»"
        }

        should("build ast of 1 * 2 + 3 * 4") {
            val expr = Add(
                Multiply(Number(1), Number(2)),
                Multiply(Number(3), Number(4)),
            )
            expr.inspect() shouldBe  "«1 * 2 + 3 * 4»"
        }

    }

    context("expression 1 * 2 + 3 * 4") {
        should("«1 * 2 + 3 * 4», when inspect expression") {
            val expr = Add(
                Multiply(Number(1), Number(2)),
                Multiply(Number(3), Number(4)),
            )
            expr.inspect() shouldBe  "«1 * 2 + 3 * 4»"
        }

        should("reduce to 14") {
            val expr: Expression = Add(
                Multiply(Number(1), Number(2)),
                Multiply(Number(3), Number(4)),
            )
            val env = Environment();
            expr.inspect() shouldBe  "«1 * 2 + 3 * 4»"
            var res = expr.reduce(env)
            res.first.inspect() shouldBe "«2 + 3 * 4»"

            res = res.first.reduce(res.second)
            res.first.inspect() shouldBe "«2 + 12»"

            res = res.first.reduce(res.second)
            res.first.inspect() shouldBe "«14»"

            res.first.reducible().shouldBeFalse()
        }

        should("machine run expression") {

        }
    }

    context("Machine execute expression") {
        should("reduce to 14") {
            val expr: Expression = Add(
                Multiply(Number(1), Number(2)),
                Multiply(Number(3), Number(4)),
            )
            val out = captureStandardOut {
                Machine(expr, Environment()).run()
            }
            out.trim() shouldBe """
                |1 * 2 + 3 * 4, {}
                |2 + 3 * 4, {}
                |2 + 12, {}
                |14, {}
            """.trimMargin()
        }

        should("reduce to false") {
            val out = captureStandardOut {
                Machine(
                    LessThan(Number(5), Add(Number(2), Number(2))),
                    Environment(),
                ).run()
            }
            out.trim() shouldBe """
                |5 < 2 + 2, {}
                |5 < 4, {}
                |false, {}
            """.trimMargin()
        }

        should("reduce variable in environment") {
            val output = captureStandardOut {
                Machine(
                    Add(Variable("x"), Variable("y")),
                    env("x" to Number(3), "y" to Number(4))
                ).run()
            }

            output.trim() shouldBe """
                |x + y, {:x => «3», :y => «4»}
                |3 + y, {:x => «3», :y => «4»}
                |3 + 4, {:x => «3», :y => «4»}
                |7, {:x => «3», :y => «4»}
            """.trimMargin()
        }

        should("reduce assign statement") {


            val output = captureStandardOut {
                Machine(
                    Assign("x", Add(Variable("x"), Number(1))),
                    env("x" to Number(2))
                ).run()
            }

            output.trim() shouldBe """
              |x = x + 1, {:x => «2»}
              |x = 2 + 1, {:x => «2»}
              |x = 3, {:x => «2»}
              |do-nothing, {:x => «3»}
            """.trimMargin()
        }

        should("reduce if statement") {
            val output = captureStandardOut {
                Machine(
                    If(
                        Variable("x"),
                        Assign("y", Number(1)),
                        Assign("y", Number(2))
                    ),
                    env("x" to Bool(true))
                ).run()
            }

            output.trim() shouldBe """
                |if (x) { y = 1 } else { y = 2 }, {:x => «true»}
                |if (true) { y = 1 } else { y = 2 }, {:x => «true»}
                |y = 1, {:x => «true»}
                |do-nothing, {:x => «true», :y => «1»}
            """.trimMargin()
        }

        should("reduce sequence") {
            val output = captureStandardOut {
                Machine(
                    Sequence(
                        Assign("x", Add(Number(1), Number(1))),
                        Assign("y", Add(Variable("x"), Number(3)))
                    ),
                    env()
                ).run()
            }
            output.trim() shouldBe """
                |x = 1 + 1; y = x + 3, {}
                |x = 2; y = x + 3, {}
                |do-nothing; y = x + 3, {:x => «2»}
                |y = x + 3, {:x => «2»}
                |y = 2 + 3, {:x => «2»}
                |y = 5, {:x => «2»}
                |do-nothing, {:x => «2», :y => «5»}
            """.trimMargin()
        }

        should("reduce while statement") {

            val output = captureStandardOut {
                Machine(
                    While(
                        LessThan(Variable("x"), Number(5)),
                        Assign("x", Multiply(Variable("x"), Number(3)))
                    ),
                    env("x" to Number(1))
                ).run()
            }

            output.trim() shouldBe """
                |while (x < 5) { x = x * 3 }, {:x => «1»}
                |if (x < 5) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «1»}
                |if (1 < 5) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «1»}
                |if (true) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «1»}
                |x = x * 3; while (x < 5) { x = x * 3 }, {:x => «1»}
                |x = 1 * 3; while (x < 5) { x = x * 3 }, {:x => «1»}
                |x = 3; while (x < 5) { x = x * 3 }, {:x => «1»}
                |do-nothing; while (x < 5) { x = x * 3 }, {:x => «3»}
                |while (x < 5) { x = x * 3 }, {:x => «3»}
                |if (x < 5) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «3»}
                |if (3 < 5) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «3»}
                |if (true) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «3»}
                |x = x * 3; while (x < 5) { x = x * 3 }, {:x => «3»}
                |x = 3 * 3; while (x < 5) { x = x * 3 }, {:x => «3»}
                |x = 9; while (x < 5) { x = x * 3 }, {:x => «3»}
                |do-nothing; while (x < 5) { x = x * 3 }, {:x => «9»}
                |while (x < 5) { x = x * 3 }, {:x => «9»}
                |if (x < 5) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «9»}
                |if (9 < 5) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «9»}
                |if (false) { x = x * 3; while (x < 5) { x = x * 3 } } else { do-nothing }, {:x => «9»}
                |do-nothing, {:x => «9»}
            """.trimMargin()
        }
    }

    context("Big Step") {
        should("execute Number") {
            val res = Number(23).evaluate(env())
            (res as Number).value shouldBe 23
        }

        should("execute variable expression") {
            val expr = Variable("x", ).evaluate(env("x" to Number(23)))
            expr.shouldBeInstanceOf<Number>()
            expr.value shouldBe 23
        }

        should("execute lessthan expression") {
            val expr = LessThan(
                Add(Variable("x"), Number(2)),
                Variable("y")
            ).evaluate(env("x" to Number(2), "y" to Number(5)))

            expr.shouldBeInstanceOf<Bool>()
            expr.value.shouldBeTrue()
        }

        should("execute statement") {
            val stat = Sequence(
                Assign("x", Add(Number(1), Number(1))),
                Assign("y", Add(Variable("x"), Number(3)))
            )
            val e = stat.evaluate(env())
            e shouldContainKey "x"
            val x = e["x"]
            x.shouldBeInstanceOf<Number>()
            x.value shouldBe 2

            e shouldContainKey "y"
            val y = e["y"]
            y.shouldBeInstanceOf<Number>()
            y.value shouldBe 5
        }

        should("execute while statement") {
            val stat = While(
                LessThan(Variable("x"), Number(5)),
                Assign("x", Multiply(Variable("x"), Number(3)))
            )

            val e = stat.evaluate(env("x" to Number(1)))

            e shouldContainKey "x"
            val x = e["x"]
            x.shouldBeInstanceOf<Number>()
            x.value shouldBe 9
        }
    }
})