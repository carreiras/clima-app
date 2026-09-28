# Aula 1 — Setup: Gradle, Hilt e KSP

## Objetivo

Antes de escrever qualquer tela ou lógica, precisamos preparar o terreno:
adicionar ao projeto as ferramentas que as próximas aulas vão usar (Hilt,
pra injeção de dependência). O objetivo desta aula é puramente de
configuração — entender **como** uma dependência entra no projeto, pra que
as próximas aulas só precisem repetir esse mesmo padrão.

## O que são Hilt e KSP, antes de mexer em qualquer arquivo

### O problema que o Hilt resolve

Sem Hilt, quando uma classe precisa de outra (por exemplo, um `ViewModel`
que precisa de um `Repository`, que precisa de um cliente HTTP), alguém
tem que **montar** essa cadeia manualmente:

```kotlin
val retrofit = Retrofit.Builder().baseUrl("...").build()
val api = retrofit.create(WeatherApiService::class.java)
val repository = WeatherRepositoryImpl(api)
val viewModel = SearchViewModel(repository)
```

Isso é "injeção de dependência" feita à mão: cada objeto recebe (é
"injetado com") as peças de que precisa, em vez de criá-las sozinho por
dentro. Funciona, mas em um app real esse encadeamento cresce rápido, se
repete em vários lugares, e fica fácil esquecer de reaproveitar a mesma
instância onde deveria (por exemplo, dois `ViewModel`s que deveriam
compartilhar o mesmo banco de dados, mas cada um cria o seu).

**Hilt é uma biblioteca que monta essa cadeia pra você, automaticamente.**
Em vez de escrever `SearchViewModel(WeatherRepositoryImpl(retrofit.create(...)))`
à mão, você anota as classes (`@Inject`, `@HiltViewModel`, etc.) dizendo
"isso aqui precisa daquilo ali", e o Hilt descobre a ordem certa de
construção sozinho, garantindo também que peças marcadas como "únicas"
(`@Singleton`) realmente só existam uma vez no app inteiro.

### O que o KSP tem a ver com isso

O Hilt não faz essa mágica em tempo de execução (não fica "adivinhando"
tipos enquanto o app roda — isso seria lento e propenso a erro). Em vez
disso, ele **gera código Kotlin de verdade**, durante o build, que faz
esse encadeamento manual que vimos acima — só que automaticamente, a
partir das suas anotações.

**KSP (Kotlin Symbol Processing)** é a ferramenta que lê seu código-fonte
Kotlin durante o build, encontra essas anotações, e gera esses arquivos
`.kt` extras (você nunca edita esses arquivos gerados — eles ficam em
`app/build/generated/`). O Hilt é o exemplo desta aula, mas o Room
(Aula 7) usa exatamente o mesmo mecanismo: anota uma interface de DAO, e o
KSP gera a implementação que conversa com o SQLite de verdade.

> Analogia: se o Hilt é a "receita" (as anotações dizendo o que cada
> classe precisa), o KSP é o "cozinheiro" que lê a receita e prepara o
> prato (gera o código) antes do app rodar.

## O que vamos construir

Nada de tela ainda. Ao final desta aula, o projeto:

- Tem o plugin **KSP** (Kotlin Symbol Processing) aplicado — é o motor que
  o Hilt (e depois o Room) usa para gerar código automaticamente a partir
  de anotações.
- Tem o **Hilt** aplicado e pronto para ser usado na Aula 2.
- Continua compilando normalmente, mesmo sem nenhuma anotação Hilt em
  lugar nenhum ainda.

## Passo a passo com código explicado

### 1. O catálogo de versões (`gradle/libs.versions.toml`)

Este projeto usa o **Gradle Version Catalog** — em vez de escrever a
versão de cada biblioteca espalhada pelos arquivos `build.gradle.kts`, tudo
fica centralizado num único arquivo TOML. Adicionamos:

```toml
[versions]
ksp = "2.2.10-2.0.2"
hilt = "2.60.1"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
```

> **`[versions]`** → só guarda números de versão, com um nome (`ksp`,
> `hilt`) que os outros blocos vão referenciar com `version.ref`. Trocar a
> versão de uma dependência usada em vários lugares vira uma mudança de
> uma linha só.
>
> **`[plugins]`** → cada entrada aqui é um **plugin do Gradle** (código que
> roda durante o build, não uma biblioteca que vira parte do app). O `id`
> é o identificador único do plugin no repositório de plugins do Gradle.
>
> **`[libraries]`** → cada entrada é uma **dependência Maven** de verdade
> (código que é compilado junto com o app). `group` + `name` formam as
> duas primeiras partes das coordenadas Maven (`group:name:version` —
> a versão vem do `version.ref`).

Repare que `hilt` aparece duas vezes com propósitos diferentes: uma vez em
`[plugins]` (o plugin que configura o projeto) e outra em `[libraries]`
(o código do Hilt que o app efetivamente usa em tempo de execução). São
duas coisas distintas que, por coincidência, têm o mesmo nome de projeto.

### 2. Aplicando os plugins (`app/build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
```

> **`alias(libs.plugins.x)`** → em vez de escrever
> `id("com.google.dagger.hilt.android") version "2.60.1"` diretamente
> aqui, usamos a referência que já existe no catálogo (`libs.plugins.hilt`).
> O Gradle gera esse `libs.plugins.*` automaticamente a partir do
> `libs.versions.toml` — é por isso que o autocomplete da IDE funciona
> nesse bloco.

E a dependência do Hilt em si, dentro do bloco `dependencies`:

```kotlin
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
```

> **`implementation(libs.hilt.android)`** → o código do Hilt que o app usa
> diretamente (anotações como `@HiltAndroidApp`, que a Aula 2 vai usar).
>
> **`ksp(libs.hilt.compiler)`** → não é `implementation`, é `ksp` — esse
> é o **processador de anotações** do Hilt. Ele não vira código que o app
> chama diretamente; ele roda durante o build, lê as anotações do Hilt
> espalhadas pelo código e **gera** as classes de injeção de dependência
> automaticamente. É por isso que precisamos do plugin KSP também.

### 3. Um ajuste necessário: `gradle.properties`

Ao tentar buildar pela primeira vez, o Gradle recusou com este erro:

```
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
```

Isso acontece porque este projeto usa uma versão recente do Android
Gradle Plugin (AGP) que tem suporte **embutido** ao Kotlin — uma forma
nova e mais rápida de compilar Kotlin, sem depender do plugin separado
`org.jetbrains.kotlin.android`. O problema é que o KSP (usado pelo Hilt, e
mais adiante pelo Room) ainda registra as pastas de código gerado usando a
API antiga, que o novo modo "Kotlin embutido" bloqueia por padrão.

A solução é liberar esse comportamento explicitamente:

```properties
android.disallowKotlinSourceSets=false
```

> Isso não desliga o Kotlin embutido — só diz ao AGP "tudo bem, deixa esse
> plugin específico (KSP) registrar código gerado do jeito antigo dele".
> É um ajuste de compatibilidade, não uma escolha de arquitetura.

## Critério de aceite

Rodar `./gradlew.bat assembleDebug` termina com `BUILD SUCCESSFUL` —
mesmo sem nenhuma tela nova, isso confirma que os plugins e dependências
foram resolvidos e configurados corretamente.
