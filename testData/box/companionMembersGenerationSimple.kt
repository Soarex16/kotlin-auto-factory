// WITH_STDLIB
package foo.bar

@com.soarex.autofactory.annotations.CachingFactory
data class SomeTransformed(val fieldInt: Int)

fun box(): String {
    val d1 = SomeTransformed.create(1)
    val d2 = SomeTransformed.create(1)
    return if (d1 === d2) {
        "OK"
    } else {
        "FAIL"
    }
}
