// IGNORED_____IGNORE_BACKEND: ANY

package foo.bar

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithoutCompanion(val a: Int, val b: String)

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithCompanion(val a: Int, val b: String) {
    companion object {
        val x = 42
    }
}

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithNamedCompanion(val a: Int, val b: String) {
    companion object NamedCompanion {
        val x = 42
    }
}

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithAdditionalConstructor(val a: Int, val b: String) {
    constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithIgnoredConstructor(val a: Int, val b: String) {
    @com.soarex.autofactory.annotations.IgnoreInCachingFactory constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithPublicAndPrivateAdditionalConstructor(val a: Int, val b: String) {
    constructor(a: Int) : this(a, a.toString())

    private constructor(a: String) : this(a.toInt(), a)
}

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithTypeParams<K : Comparable<K>, V>(val a: Int, val b: K, val c: V)

fun box(): String = "OK"
