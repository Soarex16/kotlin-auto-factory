// IGNORE_BACKEND: ANY

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
class RegularClassWithoutCompanion(val a: Int, val b: String)

@com.soarex.autofactory.annotation.CachingFactory
class RegularClassWithCompanion(val a: Int, val b: String) {
    companion object {
        val x = 42
    }
}

@com.soarex.autofactory.annotation.CachingFactory
class RegularClassWithNamedCompanion(val a: Int, val b: String) {
    companion object NamedCompanion {
        val x = 42
    }
}

fun box(): String {
    val d1 = DataClassWithoutCompanion.Companion
    val d2 = DataClassWithCompanion.Companion
    val d3_1 = DataClassWithNamedCompanion.<!UNRESOLVED_REFERENCE!>Companion<!>
    val d3_2 = DataClassWithNamedCompanion.NamedCompanion

    val r1 = RegularClassWithoutCompanion.<!UNRESOLVED_REFERENCE!>Companion<!>
    val r2 = RegularClassWithCompanion.Companion
    val r3_1 = RegularClassWithNamedCompanion.<!UNRESOLVED_REFERENCE!>Companion<!>
    val r3_2 = RegularClassWithNamedCompanion.NamedCompanion
    return "OK"
}
