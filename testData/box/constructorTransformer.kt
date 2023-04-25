// IGNORE_BACKEND: ANY

package foo.bar

// Data classes

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithAnnotation(val a: Int, val b: String)

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithAnnotationWithPublicAdditionalConstructor(val a: Int, val b: String) {
    constructor(n: Int): this(n, n.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class DataClassWithAnnotationWithPrivateAdditionalConstructor(val a: Int, val b: String) {
    private constructor(n: Int): this(n, n.toString())
}

data class DataClassWithoutAnnotation(val a: Int, val b: String)

data class DataClassWithoutAnnotationWithPublicAdditionalConstructor(val a: Int, val b: String) {
    constructor(n: Int): this(n, n.toString())
}

data class DataClassWithoutAnnotationWithPrivateAdditionalConstructor(val a: Int, val b: String) {
    private constructor(n: Int): this(n, n.toString())
}

// Regular classes

@com.soarex.autofactory.annotations.CachingFactory
class RegularClassWithAnnotation(val a: Int, val b: String)

@com.soarex.autofactory.annotations.CachingFactory
class RegularClassWithAnnotationWithPublicAdditionalConstructor(val a: Int, val b: String) {
    constructor(n: Int): this(n, n.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
class RegularClassWithAnnotationWithPrivateAdditionalConstructor(val a: Int, val b: String) {
    private constructor(n: Int): this(n, n.toString())
}

class RegularClassWithoutAnnotation(val a: Int, val b: String)

class RegularClassWithoutAnnotationWithPublicAdditionalConstructor(val a: Int, val b: String) {
    constructor(n: Int): this(n, n.toString())
}

class RegularClassWithoutAnnotationWithPrivateAdditionalConstructor(val a: Int, val b: String) {
    private constructor(n: Int): this(n, n.toString())
}

fun box(): String {
    val d1 = <!INVISIBLE_REFERENCE!>DataClassWithAnnotation<!>(1, "1")
    val d2_1 = <!NONE_APPLICABLE!>DataClassWithAnnotationWithPublicAdditionalConstructor<!>(1, "1")
    val d2_2 = <!NONE_APPLICABLE!>DataClassWithAnnotationWithPublicAdditionalConstructor<!>(1)
    val d3_1 = <!NONE_APPLICABLE!>DataClassWithAnnotationWithPrivateAdditionalConstructor<!>(1, "1")
    val d3_2 = <!NONE_APPLICABLE!>DataClassWithAnnotationWithPrivateAdditionalConstructor<!>(1)
    val d4 = DataClassWithoutAnnotation(1, "1")
    val d5_1 = DataClassWithoutAnnotationWithPublicAdditionalConstructor(1, "1")
    val d5_2 = DataClassWithoutAnnotationWithPublicAdditionalConstructor(1)
    val d6_1 = DataClassWithoutAnnotationWithPrivateAdditionalConstructor(1, "1")
    val d6_2 = <!INVISIBLE_REFERENCE!>DataClassWithoutAnnotationWithPrivateAdditionalConstructor<!>(1)

    var r1 = RegularClassWithAnnotation(1, "1")
    var r2_1 = RegularClassWithAnnotationWithPublicAdditionalConstructor(1, "1")
    var r2_2 = RegularClassWithAnnotationWithPublicAdditionalConstructor(1)
    var r3_1 = RegularClassWithAnnotationWithPrivateAdditionalConstructor(1, "1")
    var r3_2 = <!INVISIBLE_REFERENCE!>RegularClassWithAnnotationWithPrivateAdditionalConstructor<!>(1)
    var r4 = RegularClassWithoutAnnotation(1, "1")
    var r5_1 = RegularClassWithoutAnnotationWithPublicAdditionalConstructor(1, "1")
    var r5_2 = RegularClassWithoutAnnotationWithPublicAdditionalConstructor(1)
    var r6_1 = RegularClassWithoutAnnotationWithPrivateAdditionalConstructor(1, "1")
    var r6_2 = <!INVISIBLE_REFERENCE!>RegularClassWithoutAnnotationWithPrivateAdditionalConstructor<!>(1)

    return "OK"
}
