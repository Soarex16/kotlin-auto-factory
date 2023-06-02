# Kotlin auto factory

Kotlin compiler plugin for automatic generation of factory methods with caching.

## Hot to use

Just add `@CachingFactory` annotation to your data class as follows:

```kotlin
@CachingFactory
data class Point(val x: Int, val y: Int)
```

After that you can create instances of this class using `create` method:

```kotlin
val point1 = Point.create(1, 1)
val point2 = Point.create(1, 1)

require(point1 === point2)
```

## Details

Project contains two modules:
- root module is a module for the compiler plugin itself
- `:plugin-annotations` module contains annotations which can be used in user code for interacting with compiler plugin

## Compilation model

Briefly, the code is converted as follows:

```kotlin
// source
@CachingFactory
data class Some(val x: Int, val s: String)

// transformed
@CachingFactory
data class Some private constructor(val x: Int, val s: String) {
    companion object {
        private data class Arguments(val x: Int, val s: String)
        private val cache = mutableMapOf<Arguments, Some>()
        
        fun create(x: Int, s: String): Some {
            return cache.getOrPut(Arguments(x, s)) { Some(x, s) }
        }
    }
}
```

For more details check [compilation model notes](docs/compilationModel.kt).
