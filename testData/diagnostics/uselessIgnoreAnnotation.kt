package foo.bar

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_1(val a: Int, val b: String) {

    private constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_2(val a: Int, val b: String) {
    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_3(val a: Int, val b: String) {
    <!USELESS_IGNORE_ANNOTATION!>@com.soarex.autofactory.annotations.IgnoreInCachingFactory<!>
    private constructor(a: Int) : this(a, a.toString())
}

@com.soarex.autofactory.annotations.CachingFactory
data class MyDataClass_4(val a: Int, val b: String) {
    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    internal constructor(a: Int) : this(a, a.toString())
}