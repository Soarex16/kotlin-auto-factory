// WITH_STDLIB
package foo.bar

data class DataClassWithoutAnnotation(val a: Int, val b: String) {
    companion object {
        val x = 100
    }
}

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
data class DataClassWithTypeParams<K : Number, V>(val a: Int, val b: K, val c: V)

fun <T> checkIsSame(factory: () -> T): Boolean = factory() === factory()

fun <T> checkIsNotSame(factory: () -> T): Boolean = factory() !== factory()

fun box(): String {
    var i = 1
    val testCases = listOf(
        checkIsSame { DataClassWithoutCompanion.create(1, "foo") },
        checkIsSame { DataClassWithCompanion.create(1, "foo") },
        checkIsSame { DataClassWithNamedCompanion.create(1, "foo") },
        checkIsSame { DataClassWithAdditionalConstructor.create(1, "foo") },
        checkIsSame { DataClassWithIgnoredConstructor.create(1, "foo") },
        checkIsSame { DataClassWithAdditionalConstructor.create(1) },
        checkIsSame { DataClassWithPublicAndPrivateAdditionalConstructor.create(1, "foo") },
        checkIsSame { DataClassWithPublicAndPrivateAdditionalConstructor.create(1) },
        checkIsSame { DataClassWithTypeParams.create<Int, String>(1, 1, "1") },

        checkIsNotSame { DataClassWithoutCompanion.create(i++, "foo") },
        checkIsNotSame { DataClassWithCompanion.create(i++, "foo") },
        checkIsNotSame { DataClassWithNamedCompanion.create(i++, "foo") },
        checkIsNotSame { DataClassWithAdditionalConstructor.create(i++, "foo") },
        checkIsNotSame { DataClassWithIgnoredConstructor.create(i++, "foo") },
        checkIsNotSame { DataClassWithAdditionalConstructor.create(i++) },
        checkIsNotSame { DataClassWithPublicAndPrivateAdditionalConstructor.create(i++, "foo") },
        checkIsNotSame { DataClassWithPublicAndPrivateAdditionalConstructor.create(i++) },
        checkIsNotSame { DataClassWithTypeParams.create<Int, String>(i++, 1, "1") },
    )
    return if (testCases.all { it }) {
        "OK"
    } else {
        "FAIL"
    }
}