/**
 * # Генератор кеширующей фабрики:
 *
 * ## Ограничения:
 * 1. Аннотацию можно повесить только на data class-ы
 * 2. Дефолтные значения конструкторов не поддерживаются
 *
 * ## Модель компиляции:
 * 1. Получаем список конструкторов
 * 2. Все конструкторы делаем приватными
 * 3. Генерируем companion object (если его нет)
 * 4. Добавляем в него `private sealed interface ConstructorArgumentsKey`
 * 5. Добавляем поле `cache: MutableMap<ConstructorArgumentsKey, SomeTransformed<*>>`
 * 6. Для каждого **неприватного** конструктора, а также непомеченного аннотацией @IgnoreInCachingFactory в исходном
 * дата классе генерируем класс для аргументов.
 *    Пример: `constructor(x: T)` -> `private data class Arguments_SecondaryCtor<T>(val x: T): ConstructorArgumentsKey`
 * 7. Для каждого конструктора с прошлого шага генерируем метод create в компаньон исходного дата класса.
 * */

// source
@com.soarex.autofactory.annotations.CachingFactory
data class Some<T>(val fieldInt: Int, val fieldString: String, val fieldDefaultInt: Int = 42, val fieldGeneric: T) {
    constructor(x: T) : this(1, "1", 42, x)

    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    constructor(d: Double, x: T) : this(1, "1", d.toInt(), x)

    private constructor(x: T, y: Int) : this(1, "1", y, x)

    fun makeX(): Some<T> = Some(fieldGeneric, fieldInt)
}

// transformed
@com.soarex.autofactory.annotations.CachingFactory
data class SomeTransformed<T> private constructor(val fieldInt: Int, val fieldString: String, val fieldDefaultInt: Int, val fieldGeneric: T) {
    private constructor(x: T) : this(1, "1", 42, x)

    // we don't touch private constructors and constructors annotated with @IgnoreInCachingFactory
    private constructor(x: T, y: Int) : this(1, "1", y, x)

    @com.soarex.autofactory.annotations.IgnoreInCachingFactory
    constructor(d: Double, x: T) : this(1, "1", d.toInt(), x)

    companion object {
        private val cache = mutableMapOf<ConstructorArgumentsKey, SomeTransformed<*>>()

        private sealed class ConstructorArgumentsKey {
            data class Arguments_PrimaryCtor<T>(val fieldInt: Int, val fieldString: String, val fieldDefaultInt: Int, val fieldGeneric: T): ConstructorArgumentsKey()
            data class Arguments_SecondaryCtor<T> constructor(val x: T): ConstructorArgumentsKey()
        }
        fun <T> create(fieldInt: Int, fieldString: String, fieldDefaultInt: Int = 42, fieldGeneric: T): SomeTransformed<T> {
            return cache.getOrPut(ConstructorArgumentsKey.Arguments_PrimaryCtor(fieldInt, fieldString, fieldDefaultInt, fieldGeneric)) { SomeTransformed(fieldInt, fieldString, fieldDefaultInt, fieldGeneric) } as SomeTransformed<T>
        }

        fun <T> create(x: T): SomeTransformed<T> {
            return cache.getOrPut(ConstructorArgumentsKey.Arguments_SecondaryCtor(x)) { SomeTransformed(x) } as SomeTransformed<T>
        }
    }
}