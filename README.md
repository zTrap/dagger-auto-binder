# OBSOLETE. (Anvil)[https://github.com/square/anvil] provides better functionality. Try it out

# Automatic binder for [Dagger 2](https://github.com/google/dagger)

Automatically generated dagger module with `@Binds` methods

[ ![Download](https://api.bintray.com/packages/ztrap/maven/auto-binder-core/images/download.svg) ](https://bintray.com/ztrap/maven/auto-binder-core/_latestVersion)

## Install

```gradle
implementation "ru.ztrap.tools:auto-binder-core:1.0.6"
kapt "ru.ztrap.tools:auto-binder-processor:1.0.6"
```

## Usage

### Basics

In order to allow compiler generate dagger module and `@Binds` methods, define an auto-binder dagger module anywhere in
the same gradle module with `@AutoBindTo` classes:

```kotlin
@Module(includes = [AutoBinder_SampleModule::class])
@AutoBinderModule
object SampleModule
```

### Single binding

```kotlin
interface Contract {
    interface IPresenter
}

@AutoBindTo(Contract.IPresenter::class)
class Presenter : Contract.IPresenter
```

This will generate the following:

```java
@Module
public abstract class AutoBinder_SampleModule {
  private AutoBinder_SampleModule() {
  }

  @Binds
  abstract Contract.IPresenter bindPresenterToContract$IPresenter(Presenter binding);
}
```

### Into set

```kotlin
interface Interceptor {
    fun intercept(pipeline: Pipeline)
}

@AutoBindTo(Interceptor::class, AutoBindTo.Type.INTO_SET)
class FirstInterceptor : Interceptor

@AutoBindTo(Interceptor::class, AutoBindTo.Type.INTO_SET)
class SecondInterceptor : Interceptor
```

This will generate the following:

```java
@Module
public abstract class AutoBinder_SampleModule {
  private AutoBinder_SampleModule() {
  }

  @Binds
  @IntoSet
  abstract Interceptor bindFirstInterceptorToInterceptor(FirstInterceptor binding);

  @Binds
  @IntoSet
  abstract Interceptor bindSecondInterceptorToInterceptor(SecondInterceptor binding);
}
```

### Into map

Default map keys (representations of `dagger.multibindings`-keys)

| Dagger name   | Our name        |
|---------------|-----------------|
| `ClassKey`    | `AutoClassKey`  |
| `IntKey`      | `AutoIntKey`    |
| `LongKey`     | `AutoLongKey`   |
| `StringKey`   | `AutoStringKey` |

#### Example

```kotlin
interface Interceptor {
    fun intercept(pipeline: Pipeline)
}

@AutoStringKey("first")
@AutoBindTo(Interceptor::class, AutoBindTo.Type.INTO_MAP)
class FirstInterceptor : Interceptor

@AutoStringKey("second")
@AutoBindTo(Interceptor::class, AutoBindTo.Type.INTO_MAP)
class SecondInterceptor : Interceptor
```

This will generate the following:

```java
@Module
public abstract class AutoBinder_SampleModule {
  private AutoBinder_SampleModule() {
  }

  @Binds
  @IntoMap
  @StringKey("first")
  abstract Interceptor bindFirstInterceptorToInterceptor(FirstInterceptor binding);

  @Binds
  @IntoMap
  @StringKey("second")
  abstract Interceptor bindSecondInterceptorToInterceptor(SecondInterceptor binding);
}
```

### Qualifiers

Annotation marked with `dagger.Qualifier` annotation will be automatically copied to generated `@Binds` method **only** if parameter `translateQualifier` == `true` in `@AutoBindTo`

#### Example

```kotlin
interface Interceptor {
    fun intercept(pipeline: Pipeline)
}

@Named("first")
@AutoBindTo(Interceptor::class, AutoBindTo.Type.INTO_SET)
class FirstInterceptor : Interceptor

@Named("second")
@AutoBindTo(Interceptor::class, AutoBindTo.Type.INTO_SET)
class SecondInterceptor : Interceptor
```

This will generate the following:

```java
@Module
public abstract class AutoBinder_SampleModule {
  private AutoBinder_SampleModule() {
  }

  @Binds
  @IntoSet
  @Named("first")
  abstract Interceptor bindFirstInterceptorToInterceptor(FirstInterceptor binding);

  @Binds
  @IntoSet
  @Named("second")
  abstract Interceptor bindSecondInterceptorToInterceptor(SecondInterceptor binding);
}
```

## Developed By

 - Peter Gulko
 - ztrap.developer@gmail.com
 - [paypal.me/zTrap](https://www.paypal.me/zTrap)

## License

    Copyright 2019 zTrap.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
