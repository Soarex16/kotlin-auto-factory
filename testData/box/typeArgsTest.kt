// WITH_STDLIB
package foo.bar

@com.soarex.autofactory.annotations.CachingFactory
//data class SomeTransformed<K: Number>(val cmp: Comparator<K>)
data class SomeTransformed<K>(val fieldGeneric: K) where K : Number, K : Comparable<K>

//data class SomeTransformed<K: Number>(val fieldGeneric: K) {
//    companion object {
//        fun <K: Number> create(fieldGeneric: K): SomeTransformed<K> {
//            return { SomeTransformed<K>(fieldGeneric) }.invoke()
//        }
//    }
//}

fun box(): String {
    val i1 = SomeTransformed.create<Int>(1)
    val i2 = SomeTransformed.create<Int>(1)

    val s1 = SomeTransformed.create<Double>(1.2)
    val s2 = SomeTransformed.create<Double>(1.2)
    return if (i1 === i2 && s1 === s2) {
        "OK"
    } else {
        "FAIL"
    }
}
