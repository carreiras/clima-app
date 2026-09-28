# Aula 1 — Setup: Gradle, Hilt e KSP

## Objetivo

Toda aula de programação começa com a tentação de já sair escrevendo
tela. Vamos resistir a ela por um instante. Antes de qualquer botão ou
qualquer chamada de API, o projeto precisa "conhecer" as ferramentas que
vamos usar — como quando você monta a bancada de trabalho antes de
começar a construir alguma coisa.

Nesta aula você não vai ver nada de novo rodando no celular. Isso é
normal e esperado! O objetivo aqui é só entender **como uma ferramenta
entra no projeto**, porque esse mesmo processo vai se repetir, quase
igualzinho, em quase toda aula daqui pra frente.

## Antes de tudo: o que são Hilt e KSP?

### O problema que o Hilt resolve

Imagine o app de clima quase pronto. Ele vai ter, entre outras coisas:

- Uma tela de busca, que precisa de...
- ...um `Repository`, que sabe buscar dados de clima, que precisa de...
- ...um cliente HTTP, que sabe conversar com a internet.

Sem nenhuma ferramenta de apoio, alguém precisa **montar essa corrente à
mão**, toda vez que uma dessas peças for usada:

```kotlin
val retrofit = Retrofit.Builder().baseUrl("...").build()
val api = retrofit.create(WeatherApiService::class.java)
val repository = WeatherRepositoryImpl(api)
val viewModel = SearchViewModel(repository)
```

Repare que, pra criar a última linha (o que a tela realmente quer usar),
tivemos que passar por três outras linhas antes. Fazer isso à mão em
todo canto do app que precisa dessas peças traz dois problemas:
esse bloco de código se repete várias vezes pelo projeto, e fica fácil
esquecer de reaproveitar a mesma instância onde deveria — por exemplo,
duas partes do app acabarem criando duas conexões de banco de dados
diferentes, quando deveriam compartilhar a mesma.

**O Hilt existe pra resolver isso.** Em vez de montar essa corrente à
mão em cada lugar do código, você só sinaliza "essa classe aqui precisa
daquilo ali", e o Hilt monta a corrente sozinho, na hora certa,
garantindo também que as peças que devem existir uma única vez no app
inteiro realmente existam uma única vez.

> Pense no Hilt como um funcionário de fábrica que já sabe, de cor, a
> ordem de montagem de qualquer peça do catálogo. Você só faz o "pedido"
> e ele entrega o produto montado — sem você precisar escrever, à mão,
> cada etapa da montagem.

### O que o KSP tem a ver com isso

Uma pergunta justa: **quando**, exatamente, o Hilt monta essa corrente?
Não é "ao vivo", enquanto o app já está rodando no celular do usuário —
isso seria lento e mais arriscado. Em vez disso, essa montagem acontece
durante o processo de build (quando você aperta "Run", ou roda um
comando de build pelo terminal): uma ferramenta lê o projeto inteiro,
entende quais peças cada classe precisa, e escreve, automaticamente, o
código que faz essa montagem — o mesmo tipo de código que vimos acima,
só que gerado por uma máquina, não digitado por uma pessoa.

Essa ferramenta se chama **KSP (Kotlin Symbol Processing)**. Ela nunca
aparece na tela, você nunca abre os arquivos que ela gera — eles ficam
escondidos numa pasta interna do projeto, só para o compilador usar.

> Analogia: se o pedido que você faz ao Hilt é a **receita** (o que cada
> prato precisa), o KSP é o **cozinheiro** que lê a receita e prepara o
> prato antes de servir — "servir", aqui, significa "o app estar pronto
> pra rodar".

Vale guardar esse nome porque ele volta na Aula 7: o Room (o banco de
dados local) usa exatamente o mesmo tipo de mecanismo automático.

## O que vamos construir

Recapitulando: nada de tela nesta aula. Ao final dela, o projeto vai ter
as duas ferramentas acima disponíveis — prontas pra serem usadas de
verdade na Aula 2 — e vai continuar compilando normalmente.

## Passo a passo

### 1. A lista de compras do projeto (`gradle/libs.versions.toml`)

Em vez de cada parte do projeto escrever a versão exata de cada
ferramenta espalhada por aí (o que faria você caçar e trocar o mesmo
número em vários lugares diferentes se precisasse atualizar algo),
existe um único arquivo central com tudo:

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

Esse arquivo é dividido em três partes, cada uma guardando um tipo
diferente de informação: uma lista de números de versão (com um apelido
pra cada um, pra não repetir o número várias vezes), uma lista de
plugins (programas que ajudam a controlar *como* o projeto é compilado),
e uma lista de bibliotecas de verdade, que viram parte do app que roda
no celular do usuário.

Um detalhe que costuma confundir no começo: repare que a palavra `hilt`
aparece duas vezes, em listas diferentes. São duas coisas diferentes que
só têm o mesmo nome por coincidência — uma ajuda a *compilar* o projeto,
a outra é código que o app *usa* de verdade. Isso é comum: muitas
ferramentas têm as duas partes.

### 2. Avisando o projeto pra usar essas ferramentas (`app/build.gradle.kts`)

Agora que a lista de compras tem KSP e Hilt nela, precisamos avisar o
módulo do app que ele quer usar essas duas ferramentas durante a
compilação:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
```

Cada linha desse bloco liga um "programa auxiliar" diferente ao processo
de build. As duas últimas linhas são as que acabamos de adicionar — sem
elas, ter o KSP e o Hilt só listados no arquivo de versões não teria
efeito nenhum; é essa etapa que efetivamente os coloca pra funcionar.

E a parte do Hilt que o código do app realmente vai importar e usar,
dentro do bloco `dependencies` (mais abaixo, no mesmo arquivo):

```kotlin
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
```

A primeira linha traz o Hilt "de verdade" — as ferramentas que o nosso
código Kotlin vai usar diretamente a partir da próxima aula. A segunda
linha é o que liga o Hilt ao KSP: é ela que faz o "cozinheiro" (KSP)
saber preparar especificamente a "receita" (o pedido) do Hilt.

### 3. Um obstáculo real que apareceu: `gradle.properties`

Vale mostrar isso porque é exatamente o tipo de coisa que acontece num
projeto real: seguimos os passos acima e, na primeira tentativa de
build, o processo foi interrompido com um aviso de incompatibilidade
entre a forma como o KSP registra o código que ele gera e uma mudança
recente e mais rápida de como este projeto compila Kotlin.

A solução foi permitir explicitamente essa combinação:

```properties
android.disallowKotlinSourceSets=false
```

Essa linha não desliga a parte nova e mais rápida de compilação (ela
continua ativa para todo o resto do projeto) — só abre uma exceção
pontual pra ferramenta que ainda precisa do jeito antigo de registrar o
que gera. É um ajuste de compatibilidade entre duas ferramentas, não uma
decisão sobre como o nosso app funciona.

## Critério de aceite

Rodar `./gradlew.bat assembleDebug` termina com sucesso. Mesmo sem
nenhuma tela nova pra ver no celular, isso já confirma uma coisa
importante: as ferramentas foram baixadas, resolvidas e configuradas
sem conflito — a bancada de trabalho está pronta pras próximas aulas.
