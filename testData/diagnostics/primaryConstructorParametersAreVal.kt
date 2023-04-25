package foo.bar

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_1(val a: Int, val b: String) {

    private constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_2(<!PRIMARY_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL!>var<!> a: Int, val b: String) {

    private constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_3(<!PRIMARY_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL!>var<!> a: Int, <!PRIMARY_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL!>var<!> b: String) {

    private constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_4 private (var a: Int, var b: String) {

    constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_5 @com.soarex.autofactory.annotations.IgnoreInCachingFactory constructor(val a: Int, val b: String) {
    constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_6(val a: Int, val b: String) {
    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    internal constructor(a: Int) : this(a, a.toString())
}