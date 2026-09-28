# Aula 3 — Navigation Compose

## Objetivo

Nosso app de clima vai precisar de mais de uma tela: uma pra buscar
cidades, uma pra ver a previsão detalhada, e uma pra ver os favoritos.
Antes de construir qualquer uma delas de verdade, vamos resolver um
problema mais básico: **como o app troca de uma tela pra outra?**

## O problema que estamos resolvendo

Pensa em como um site funciona: cada página tem um endereço (uma URL), e
navegar é simplesmente ir para outro endereço. O usuário pode voltar,
pode favoritar um link, o navegador sabe em qual página ele está.

Um app não tem URLs por padrão — telas são só funções que desenham coisa
na tela, sem nenhuma noção de "onde estou" ou "como eu volto". Se você
tentasse resolver isso sozinho, ia precisar controlar manualmente qual
tela mostrar a cada momento, guardar um histórico de onde o usuário já
passou pra o botão "voltar" funcionar, e por aí vai — regras que todo
app com mais de uma tela precisa, e que é fácil errar escrevendo à mão.

O **Navigation Compose** resolve isso dando ao app a mesma ideia de
"endereços" que um site tem: cada tela ganha um nome (uma rota, tipo
`"search"` ou `"favorites"`), existe um componente central que sabe
mostrar a tela certa pra cada rota, e trocar de tela vira só "navegar
para esse endereço" — o histórico, o botão de voltar, tudo isso passa a
ser cuidado automaticamente.

## O que vamos construir

Nesta aula ainda não tem nada pra ver de verdade em cada tela — são só
placeholders, um texto simples em cada uma. O que importa aqui é o
**encanamento**: as três rotas existindo e o app sabendo navegar entre
elas, mesmo que ainda não tenha nenhum botão que dispare essa navegação
(isso vem na Aula 6).

## Passo a passo

### As rotas

```kotlin
sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Details : Screen("details")
}
```

Isso é só uma forma organizada de guardar os "endereços" das nossas três
telas, num lugar só, em vez de espalhar o texto `"search"` (por exemplo)
solto pelo código toda vez que alguém precisar dele. Se um dia
precisarmos mudar o nome de uma rota, mudamos em um lugar só.

### O componente que decide qual tela mostrar

```kotlin
@Composable
fun ClimaNavHost(innerPadding: PaddingValues) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Search.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Search.route) { SearchScreen() }
        composable(Screen.Favorites.route) { FavoritesScreen() }
        composable(Screen.Details.route) { DetailsScreen() }
    }
}
```

> **`rememberNavController()`** → cria o objeto que vai controlar a
> navegação — é ele quem sabe em qual tela o app está agora, e quem vai
> receber, mais pra frente (Aula 6), o pedido "navega pra tal rota".
>
> **`NavHost(...)`** → é o "quadro" onde as telas aparecem. Ele recebe o
> controlador de cima, e uma `startDestination` — qual rota mostrar
> quando o app abre pela primeira vez (aqui, a Busca).
>
> **`composable(rota) { Tela() }`** → cada uma dessas linhas é uma
> "entrada no mapa": associa uma rota a qual função `@Composable` deve
> aparecer na tela quando o app estiver naquele endereço. É basicamente
> um mapa de "nome → tela".

Repare que isso é bem parecido com o exemplo do `Retrofit.Builder()` da
Aula 1 — um bloco de configuração que vai sendo montado peça por peça —
só que aqui cada peça é uma tela do app, em vez de uma parte de uma
requisição HTTP.

### As telas (por enquanto, só um texto)

Cada tela, por enquanto, é só uma função `@Composable` simples com um
texto fixo — o suficiente pra confirmar visualmente que a navegação está
levando ao lugar certo. O conteúdo de verdade de cada uma vai chegar nas
próximas aulas: a Busca na Aula 6, os Detalhes também na Aula 6, e os
Favoritos na Aula 8.

## Critério de aceite

O app compila e abre mostrando o texto "Busca (placeholder)" — a rota
inicial configurada no `NavHost`. Isso confirma que o encanamento de
navegação está funcionando: o app sabe montar a tela certa a partir de
uma rota, mesmo sem nenhum botão ainda pra trocar de rota manualmente.
