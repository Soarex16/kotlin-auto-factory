// IGNORED_____IGNORE_BACKEND: ANY

package foo.bar

@com.soarex.autofactory.annotation.CachingFactory
data class DataClassWithoutCompanion(val a: Int, val b: String)

@com.soarex.autofactory.annotation.CachingFactory
data class DataClassWithCompanion(val a: Int, val b: String) {
    companion object {
        val x = 42
    }
}

@com.soarex.autofactory.annotation.CachingFactory
data class DataClassWithNamedCompanion(val a: Int, val b: String) {
    companion object NamedCompanion {
        val x = 42
    }
}

@com.soarex.autofactory.annotation.CachingFactory
data class DataClassWithAdditionalConstructor(val a: Int, val b: String) {
    constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotation.CachingFactory
data class DataClassWithPublicAndPrivateAdditionalConstructor(val a: Int, val b: String) {
    constructor(a: Int) : this(a, a.toString())

    private constructor(a: String) : this(a.toInt(), a)
}

@com.soarex.autofactory.annotation.CachingFactory
data class DataClassWithTypeParams<K, V>(val a: Int, val b: K, val c: V)

fun box(): String {
    val d1 = DataClassWithoutCompanion.Companion.__CachingFactoryGenerated__cache
    val d2 = DataClassWithCompanion.Companion
    val d3 = DataClassWithNamedCompanion.NamedCompanion
    return if (d1.size == 0) {
        "OK"
    } else {
        "FAIL"
    }
}
