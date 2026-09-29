# Aula 12 — Menu, tipografia e tema visual

## Objetivo

O app funciona desde a Aula 10, mas visualmente sempre foi o mínimo: um
campo de texto, uma lista, sem hierarquia nem identidade. Nesta aula não
vamos mudar nenhuma lógica de negócio — nenhum Repository, nenhum
ViewModel novo — só a camada visual: um menu de verdade, tipografia com
personalidade, e navegação que se comporta como um app de verdade.

## O problema: Material3 sem escolhas parece qualquer app genérico

Todo projeto novo criado no Android Studio com Compose vem com o mesmo
roxo padrão e a fonte do sistema. Isso não é uma escolha de design — é
só o que sobra quando ninguém decide nada. Se você não escolhe uma
paleta e uma tipografia própria, o Material3 escolhe por você, e o
resultado é indistinguível de qualquer outro app template.

O objetivo aqui não é "deixar bonito" de forma vaga — é fazer escolhas
deliberadas: uma paleta com um conceito por trás dela (neste caso, céu
ao entardecer: azul profundo + coral), e uma tipografia que tenha
personalidade nos títulos sem sacrificar legibilidade no corpo de
texto.

## O que vamos construir

- Uma barra de navegação inferior com 3 abas (Buscar, Favoritos,
  Configurações), substituindo os botões de texto avulsos que existiam
  antes.
- Um `TopAppBar` que muda de título conforme a tela, e só mostra botão
  de voltar na tela de Detalhes — as 3 abas do menu não precisam de
  voltar, porque são o topo da navegação.
- Duas fontes baixadas dinamicamente do Google Fonts (não empacotadas
  no projeto).
- Uma paleta de cores própria, sem nenhum roxo do Material padrão.

## Passo a passo

### Um único Scaffold, que sabe em qual tela o app está

Antes, cada tela cuidava do próprio layout, e a `MainActivity` tinha um
`Scaffold` vazio só de fachada. Agora o `ClimaNavHost` inteiro vive
dentro de um único `Scaffold`, que consegue reagir a qual rota está
ativa:

```kotlin
val navController = rememberNavController()
val backStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = backStackEntry?.destination?.route
val isTopLevel = topLevelScreens.any { it.route == currentRoute }
```

`currentBackStackEntryAsState()` transforma "qual é a rota atual" (algo
que muda toda hora, conforme o usuário navega) numa variável de estado
Compose — sempre que a rota muda, esse valor muda, e tudo que depende
dele (o título da barra, se mostra ou não o botão de voltar, qual aba
do menu está destacada) recalcula sozinho.

### A barra de navegação sabe qual aba está ativa

```kotlin
NavigationBar {
    topLevelScreens.forEach { screen ->
        NavigationBarItem(
            selected = currentRoute == screen.route,
            onClick = {
                if (currentRoute != screen.route) {
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Search.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(screenIcon(screen), contentDescription = null) },
            label = { Text(screenLabel(screen)) }
        )
    }
}
```

> **`selected = currentRoute == screen.route`** → cada aba compara a
> própria rota com a rota atual; é assim que o Material3 sabe qual
> ícone pintar destacado.
>
> **`popUpTo(Screen.Search.route) { saveState = true }`** → sem isso,
> trocar de aba repetidamente empilharia uma tela em cima da outra (e o
> botão de voltar do sistema teria que passar por todas elas até
> sair do app). Isso diz "ao trocar de aba, esvazia o histórico de
> volta até a Busca" — mas guardando o estado de cada aba antes de
> esvaziar.
>
> **`launchSingleTop = true`** → evita abrir a mesma aba duas vezes
> empilhada, se o usuário tocar duas vezes seguidas na mesma aba.
>
> **`restoreState = true`** → junto com o `saveState` de cima, faz o
> usuário voltar pra aba de Favoritos exatamente onde parou (por
> exemplo, com a lista já rolada), em vez dela recarregar do zero toda
> vez que ele troca de aba e volta.

Esse trio (`popUpTo` + `launchSingleTop` + `restoreState`) é o padrão
oficial do Navigation Compose pra navegação por abas — não é algo que a
gente inventou, é a receita recomendada sempre que existe uma barra
inferior.

### O título e o botão de voltar mudam com a tela

```kotlin
TopAppBar(
    title = { Text(text = screenTitle(currentRoute, backStackEntry)) },
    navigationIcon = {
        if (!isTopLevel && navController.previousBackStackEntry != null) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        }
    }
)
```

```kotlin
private fun screenTitle(route: String?, backStackEntry: NavBackStackEntry?): String = when (route) {
    Screen.Search.route -> "Clima"
    Screen.Favorites.route -> "Favoritos"
    Screen.Settings.route -> "Configurações"
    Screen.Details.route -> backStackEntry?.arguments?.getString("name")
        ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Detalhes"
    else -> "Clima"
}
```

O botão de voltar só aparece quando **as duas** condições são
verdadeiras: a tela não é uma das 3 abas do menu (`!isTopLevel`), e
existe de fato uma tela anterior pra voltar
(`previousBackStackEntry != null`). Repare também que o título da tela
de Detalhes não é fixo — ele lê o argumento `name` que já viaja pela
rota desde a Aula 6 (o mesmo `savedStateHandle["name"]` que o
`DetailsViewModel` usa), então a barra mostra o nome da cidade de
verdade, não um texto genérico como "Detalhes".

### Fontes baixadas em vez de empacotadas no projeto

Em vez de baixar arquivos `.ttf` e colocar dentro do projeto (o que
deixaria o repositório mais pesado, com arquivos binários), usamos o
provedor de fontes do Google Play Services — o Android baixa a fonte
uma vez, na primeira vez que o app precisa dela, e guarda em cache no
próprio sistema:

```kotlin
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFamily = FontFamily(
    Font(GoogleFont("Outfit"), fontProvider, FontWeight.Normal),
    Font(GoogleFont("Outfit"), fontProvider, FontWeight.SemiBold)
)
```

O `certificates` aponta pra um arquivo de recurso (`font_certs.xml`)
com certificados públicos do Google — é assim que o Android confirma
que está realmente falando com o provedor oficial de fontes do Google
Play Services, e não com algo malicioso se passando por ele. Esse
arquivo é padrão, documentado publicamente pelo Google, igual pra
qualquer app que use esse recurso — não é nada específico deste
projeto.

Escolhemos duas fontes com papéis diferentes: **Outfit**, uma fonte
geométrica com mais personalidade, usada em títulos e no número grande
da temperatura; e **Inter**, otimizada pra legibilidade, usada no
corpo de texto e nas listas.

### Uma paleta própria, não o roxo padrão

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = DuskBlue,
    secondary = SunsetCoral,
    secondaryContainer = CoralPale,
    background = SkyMist,
    surface = SkySurface,
    onSurfaceVariant = MutedNavy,
    // ...
)
```

`lightColorScheme(...)` é uma função do Material3 que recebe as cores
principais e devolve um `ColorScheme` completo — só que qualquer
parâmetro que a gente **não** especifica continua com o valor padrão
do Material (o roxo de sempre). Foi exatamente isso que aconteceu numa
primeira tentativa: definimos `primary` e `secondary`, mas esquecemos
de `secondaryContainer` (a cor de fundo por trás do ícone selecionado
no menu) — resultado, o "chip" atrás do ícone continuava roxo, mesmo
com o resto da tela já na paleta nova. A correção foi simplesmente
completar os pares que faltavam (`primaryContainer`,
`secondaryContainer`, `surfaceVariant`, etc.) com tons derivados das
mesmas cores.

Um ajuste separado, também necessário: o Compose tem um recurso chamado
"cor dinâmica" (Material You), que no Android 12+ pega a paleta do
papel de parede do usuário e a aplica no app automaticamente — isso
teria sobrescrito toda a nossa paleta escolhida a dedo. Desligamos esse
comportamento (`dynamicColor = false`) de propósito, porque aqui a
identidade visual é uma escolha do app, não do papel de parede de quem
está usando.

## Critério de aceite

Testado no dispositivo físico: navegação entre as 3 abas pela barra
inferior, com o ícone da aba ativa destacado na cor certa; tela de
Detalhes mostrando o nome da cidade no título e um botão de voltar
funcional; as fontes Outfit e Inter carregando (visível pela forma
geométrica dos títulos e do número da temperatura); nenhum vestígio da
paleta roxa padrão do Material em nenhuma tela.
