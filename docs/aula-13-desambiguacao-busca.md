# Aula 13 — Desambiguando resultados de busca por região

## Objetivo

Buscar "Campo Grande" no app devolve uma lista de vários lugares
diferentes com o mesmo nome — um bairro em São Paulo, a capital do
Mato Grosso do Sul, cidades no Rio Grande do Norte, em Alagoas, até em
Portugal. Hoje a lista mostra só o nome e o país, então todos esses
resultados aparecem praticamente iguais. Nesta aula, cada resultado
passa a exibir também a região dentro do país, pra dar pra diferenciar
um do outro — tanto na Busca quanto nos Favoritos.

## O problema: nomes repetidos, mesmo país

É como abrir a agenda de contatos do celular e ver cinco pessoas
chamadas "Ana", sem sobrenome, sem foto, sem nada que diferencie uma da
outra — você só descobre qual é qual se tocar em cada uma. A lista de
busca do app tinha exatamente esse problema: dois resultados chamados
"Campo Grande, Brazil" eram visualmente idênticos, mesmo sendo lugares
a mais de 1.000 km de distância um do outro.

A boa notícia é que a API de geocoding já manda a informação que falta
— só não estávamos usando. Além de `name` e `country`, ela devolve
campos de divisão administrativa: `admin1` (o estado), `admin2`
(geralmente o município) e `admin3` (bairro ou distrito, quando existe
— ver [referência da API](open-meteo-api-referencia.md)). "Campo
Grande" o bairro de São Paulo tem `admin3 = "Santo Amaro"`, `admin2 =
"São Paulo"`; "Campo Grande" a capital do Mato Grosso do Sul tem
`admin2 = "Campo Grande"`, `admin1 = "Mato Grosso do Sul"`. É essa
diferença que vamos passar a mostrar.

## O que vamos construir

- `GeocodingResultDto` passa a capturar `admin1`, `admin2` e `admin3`.
- O `City` do domínio ganha um campo `region`, um texto já pronto pra
  exibir, montado a partir desses campos administrativos.
- A lista de resultados da Busca mostra esse texto como subtítulo.
- A tela de Favoritos mostra a mesma informação — o que exigiu guardar
  `region` também no Room, não só de passagem.
- Um bug encontrado testando: a região sumia ao favoritar uma cidade,
  mesmo já aparecendo certinho na Busca.

## Passo a passo

### Capturando os campos administrativos da API

```kotlin
data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val admin1: String?,
    val admin2: String?,
    val admin3: String?
)
```

Os três campos são opcionais (`String?`) porque nem todo resultado da
API os preenche — uma cidade grande costuma ter `admin2`, mas só
lugares dentro de uma cidade maior (como um bairro) têm `admin3`.

### Construindo um texto de região a partir desses campos

```kotlin
fun GeocodingResultDto.toDomain(): City = City(
    id = id,
    name = name,
    country = country ?: "",
    latitude = latitude,
    longitude = longitude,
    region = listOfNotNull(admin3, admin2, admin1).distinct().take(2).joinToString(", ").ifBlank { null }
)
```

Essa linha faz, em ordem: junta os três campos administrativos numa
lista, do mais específico pro mais genérico (`admin3`, depois
`admin2`, depois `admin1`); descarta os que vieram nulos
(`listOfNotNull`); remove duplicado — importante porque `admin1` e
`admin2` às vezes repetem o mesmo texto (uma cidade que é capital do
próprio estado, por exemplo); pega só os dois primeiros
(`take(2)`), pra não deixar o subtítulo comprido demais; e junta com
vírgula. Pro bairro de Campo Grande, o resultado é `"Santo Amaro, São
Paulo"`. Pra a capital do Mato Grosso do Sul, sem `admin3`, o resultado
é `"Campo Grande, Mato Grosso do Sul"`.

### Mostrando a região na lista de busca

```kotlin
supportingContent = {
    val subtitle = city.region?.let { "$it, ${city.country}" } ?: city.country
    Text(text = subtitle)
},
```

Se `region` existir, o subtítulo vira "Santo Amaro, São Paulo, Brazil";
se não existir (resultado sem `admin1`/`admin2`/`admin3`, o que a API
raramente devolve mas é tratado mesmo assim), cai de volta pro
comportamento antigo, só o país. A tela de Favoritos usa exatamente a
mesma lógica.

### Persistindo a região nos favoritos

Mostrar a região na Busca não bastava — o objeto favoritado é salvo no
Room (Aula 7), e a entidade salva lá (`CityEntity`) não tinha onde
guardar esse texto. Adicionar uma coluna nova a uma tabela já existente
é uma **migração de banco**: o Room percebe que a estrutura da tabela
mudou e precisa saber o que fazer com os dados que já existiam antes da
mudança.

```kotlin
@Database(entities = [CityEntity::class], version = 2, exportSchema = false)
```

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "clima-app.db")
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
```

Existem duas formas de migrar: escrever o passo a passo exato de como
transformar a tabela antiga na nova (preservando os dados), ou avisar o
Room pra simplesmente apagar tudo e recriar do zero quando perceber uma
mudança de versão — é isso que `fallbackToDestructiveMigration` faz.
Pra este projeto, sem usuários reais e com favoritos que qualquer um
pode adicionar de novo em segundos, a segunda opção é a escolha certa:
não vale a pena escrever uma migração de verdade pra preservar dados de
teste. Consequência prática: **instalar essa versão do app apaga os
favoritos salvos antes dela** — é esperado, não um bug.

### Bug encontrado depois: a região sumia ao favoritar

Testando no tablet, a região aparecia certinho na lista de busca
("Santo Amaro, São Paulo, Brazil"), mas depois de favoritar aquela
cidade e abrir a aba Favoritos, o mesmo item aparecia só como "Brazil"
— como se a região nunca tivesse existido.

A causa: a navegação entre telas (Aula 6) leva os dados da cidade
escolhida embutidos na própria rota (`details/{cityId}/{name}/...`),
e a rota **não incluía `region`** — só `cityId`, `name`, `country`,
`lat` e `lon`. A tela de Detalhes reconstrói o objeto `City` a partir
desses pedaços da rota, então o `City` reconstruído lá sempre tinha
`region = null`, mesmo vindo de um resultado de busca que tinha região.
E é esse `City` — o reconstruído na tela de Detalhes, não o original da
busca — que é salvo no Room quando o usuário toca em "Favoritar".

A correção foi acrescentar `region` como mais um pedaço da rota:

```kotlin
object Details : Screen("details/{cityId}/{name}/{country}/{lat}/{lon}/{region}") {
    fun createRoute(
        cityId: Long,
        name: String,
        country: String,
        lat: Double,
        lon: Double,
        region: String? = null
    ): String {
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val encodedCountry = URLEncoder.encode(country, "UTF-8")
        val encodedRegion = URLEncoder.encode(region ?: "", "UTF-8")
        return "details/$cityId/$encodedName/$encodedCountry/$lat/$lon/$encodedRegion"
    }
}
```

`region` é opcional no domínio (`String?`), mas uma rota de navegação é
só texto — não existe "pedaço de URL nulo". A saída foi usar uma string
vazia pra representar "sem região" (`region ?: ""`), e do outro lado
tratar string vazia de volta como `null`:

```kotlin
region = URLDecoder.decode(checkNotNull<String>(savedStateHandle["region"]), "UTF-8").ifBlank { null }
```

Esse é o mesmo problema, e a mesma solução, que espaços em nomes de
cidade já tinham exigido na Aula 6 (codificar e decodificar texto
livre) — só que aqui o "texto livre" também podia estar totalmente
ausente.

## Critério de aceite

Testado no dispositivo físico: buscar "Campo Grande" mostra vários
resultados com subtítulos diferentes (`Santo Amaro, São Paulo, Brazil`,
`Campo Grande, Mato Grosso do Sul, Brazil`, etc.), confirmando que a
API já trazia essa distinção e agora ela aparece na tela. Favoritar o
bairro de Santo Amaro e abrir a aba Favoritos mostra o mesmo subtítulo
completo (`Santo Amaro, São Paulo, Brazil`), não só "Brazil" —
confirma que a correção da navegação realmente resolveu o bug, e que a
região sobrevive à viagem completa: busca → detalhes → favoritar →
Room → lista de favoritos.
