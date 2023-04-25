package foo.bar

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_1(val a: Int, val b: String) {

    private constructor(a: Int) : this(a, a.toString())

    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    constructor(b: String) : this(b.toInt(), b)
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_2(val a: Int, val b: String) {
    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    internal constructor(a: Int) : this(a, a.toString())

    constructor(b: String) : this(b.toInt(), b)
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_3 @com.soarex.autofactory.annotations.IgnoreInCachingFactory constructor(val a: Int, val b: String) {
    internal constructor(a: Int) : this(a, a.toString())

    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    constructor(b: String) : this(b.toInt(), b)
}

@com.soarex.autofactory.annotations.CachingFactory
data class <!NO_SUITABLE_CONSTRUCTORS!>MyDataClass_4<!> @com.soarex.autofactory.annotations.IgnoreInCachingFactory constructor(val a: Int, val b: String)

@com.soarex.autofactory.annotations.CachingFactory
data class <!NO_SUITABLE_CONSTRUCTORS!>MyDataClass_5<!> @com.soarex.autofactory.annotations.IgnoreInCachingFactory constructor(val a: Int, val b: String) {
    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    constructor(a: Int) : this(a, a.toString())

    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    constructor(b: String) : this(b.toInt(), b)
}

@com.soarex.autofactory.annotations.CachingFactory
data class <!NO_SUITABLE_CONSTRUCTORS!>MyDataClass_6<!> private constructor(val a: Int, val b: String) {
    private constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_7(val a: Int, val b: String) {
    private constructor(a: Int) : this(a, a.toString())
}