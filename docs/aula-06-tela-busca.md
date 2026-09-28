# Aula 6 — Tela de Busca real + Detalhes

## Objetivo

Todas as peças que construímos até aqui — API, Repository, ViewModel,
navegação — ainda estavam desconectadas entre si, cada uma testada só
por "compila sem erro". Nesta aula, finalmente, tudo se conecta: você
vai poder digitar o nome de uma cidade, ver os resultados aparecerem, e
tocar em um deles pra ver a temperatura atual.

## O problema: como uma tela "sabe" o que mostrar na próxima?

Na Aula 3 criamos a rota de Detalhes, mas ela não recebia nenhuma
informação — não tinha como saber de qual cidade mostrar a previsão.
Isso levanta uma pergunta: quando você toca em "São Paulo" na lista de
resultados, como é que a tela de Detalhes fica sabendo que era São Paulo
que você escolheu, e não qualquer outra cidade?

A resposta é parecida com como sites passam informação de uma página pra
outra pela própria URL (`produto.com/item/42`, por exemplo — o `42` ali
é um dado que "viaja" junto com a navegação). O Navigation Compose
permite a mesma coisa: a rota de Detalhes passa a aceitar pedaços de
informação junto com o endereço (o id da cidade, o nome, a latitude, a
longitude), e a tela de Detalhes lê essas informações assim que é
aberta.

## O que vamos construir

- A rota de Detalhes passa a aceitar esses dados extras.
- Um clique num resultado de busca dispara a navegação, levando os
  dados da cidade escolhida junto.
- `DetailsViewModel`, que lê esses dados assim que a tela abre, e já
  pede ao Repository a previsão daquela cidade.
- A tela de Busca ganha vida de verdade: campo de texto, lista de
  resultados, estados de carregando/erro.
- A tela de Detalhes mostra a temperatura atual da cidade escolhida.

## Passo a passo

### Levando dados junto com a navegação

```kotlin
Screen.Details.createRoute(
    cityId = city.id,
    name = city.name,
    country = city.country,
    lat = city.latitude,
    lon = city.longitude
)
```

Essa função monta o "endereço completo" de Detalhes, com os dados da
cidade escolhida embutidos nele — algo como
`details/3448439/São Paulo/Brazil/-23.5/-46.6`. Um detalhe que exigiu
atenção: nomes de cidade ou país podem ter espaço (como "São Paulo"), e
como a rota usa `/` pra separar cada pedaço de informação, um espaço no
meio do nome quebraria essa separação. Por isso codificamos o nome e o
país antes de montar a rota (e decodificamos de volta do outro lado,
na tela de Detalhes) — uma técnica padrão sempre que texto livre precisa
viajar dentro de uma URL.

### A tela de Detalhes lendo esses dados assim que abre

```kotlin
val city: City = City(
    id = checkNotNull<Long>(savedStateHandle["cityId"]),
    name = URLDecoder.decode(checkNotNull<String>(savedStateHandle["name"]), "UTF-8"),
    country = URLDecoder.decode(checkNotNull<String>(savedStateHandle["country"]), "UTF-8"),
    latitude = checkNotNull<Float>(savedStateHandle["lat"]).toDouble(),
    longitude = checkNotNull<Float>(savedStateHandle["lon"]).toDouble()
)
```

O `DetailsViewModel` recebe automaticamente os dados que viajaram pela
rota, reconstrói o objeto `City` a partir deles, e — assim que é criado
— já dispara a busca da previsão daquela cidade específica no
Repository, exatamente como o `SearchViewModel` já fazia pra busca de
cidades.

### A tela de Busca reagindo ao que o usuário digita

```kotlin
OutlinedTextField(
    value = query,
    onValueChange = {
        query = it
        viewModel.search(it)
    },
    label = { Text("Buscar cidade") }
)
```

Cada letra digitada dispara uma nova busca. Logo abaixo desse campo, a
tela olha pro estado atual (`Idle`, `Loading`, `Success` ou `Error` —
que vimos na Aula 5) e decide o que desenhar: nada, uma roda de
carregamento, a lista de resultados, ou uma mensagem de erro. Como o
compilador exige que os quatro casos sejam tratados, não tem como a tela
esquecer de mostrar, por exemplo, o estado de erro.

## Critério de aceite

Buscar "São Paulo" retorna uma lista de resultados; tocar num deles
mostra a temperatura atual na tela de Detalhes. Desligar o Wi-Fi e
buscar de novo mostra uma mensagem de erro tratada, sem o app travar ou
fechar sozinho — confirma que o tratamento de erro da Aula 5 realmente
chega até a tela.
