/**
 * # Генератор кеширующей фабрики:
 *
 * ## Ограничения:
 * 1. Аннотацию можно повесить только на data class-ы
 * 2. Тут обитают драконы
 *
 * ## Модель компиляции:
 * 1. Получаем список конструкторов
 * 2. Все конструкторы делаем приватными
 * 3. Генерируем companion object (если его нет)
 * 4. Добавляем в него `private sealed interface CtorArgs`
 * 5. Добавляем поле `cache: MutableMap<CtorArgs, SomeTransformed<*>>`
 * 6. Для каждого **неприватного** конструктора в исходном дата классе генерируем класс для аргументов.
 *    Пример: `constructor(x: T)` -> `private data class Arguments_SecondaryCtor<T>(val x: T): CtorArgs`
 * 7. Для каждого **неприватного** конструктора в исходном дата классе генерируем метод create.
 * */

// source
@com.soarex.autofactory.annotation.CachingFactory
data class Some<T>(val fieldInt: Int, val fieldString: String, val fieldDefaultInt: Int = 42, val fieldGeneric: T) {
    constructor(x: T) : this(1, "1", 42, x)

    private constructor(x: T, y: Int) : this(1, "1", 42, x)

    fun makeX(): Some<T> = Some(fieldGeneric, fieldInt)
}

// transformed
@com.soarex.autofactory.annotation.CachingFactory
data class SomeTransformed<T> private constructor(val fieldInt: Int, val fieldString: String, val fieldDefaultInt: Int, val fieldGeneric: T) {
    private constructor(x: T) : this(1, "1", 42, x)

    companion object {
        private val cache = mutableMapOf<CtorArgs, SomeTransformed<*>>()

        private sealed class CtorArgs
        private data class Arguments_PrimaryCtor<T>(val fieldInt: Int, val fieldString: String, val fieldDefaultInt: Int, val fieldGeneric: T): CtorArgs()
        private data class Arguments_SecondaryCtor<T>(val x: T): CtorArgs()


        fun <T> create(fieldInt: Int, fieldString: String, fieldDefaultInt: Int = 42, fieldGeneric: T): SomeTransformed<T> {
            return cache.getOrPut(Arguments_PrimaryCtor(fieldInt, fieldString, fieldDefaultInt, fieldGeneric)) { SomeTransformed(fieldInt, fieldString, fieldDefaultInt, fieldGeneric) } as SomeTransformed<T>
        }

        fun <T> create(x: T): SomeTransformed<T> {
            return cache.getOrPut(Arguments_SecondaryCtor(x)) { SomeTransformed(x) } as SomeTransformed<T>
        }
    }
}