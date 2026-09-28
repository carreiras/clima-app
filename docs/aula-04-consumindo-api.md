# Aula 4 — Consumindo a API

## Objetivo

Chegou a hora de o app conversar com a internet de verdade. Vamos buscar
dados de clima na API pública Open-Meteo. Antes de mostrar qualquer coisa
na tela, precisamos resolver um problema conceitual: **o formato que a
API devolve raramente é o formato que faz sentido usar dentro do app.**

## O problema: dois formatos diferentes para a mesma informação

Quando você pergunta pra API "qual é o clima em São Paulo?", ela responde
com um JSON parecido com isto (simplificado):

```json
{
  "current": {
    "temperature_2m": 24.3,
    "weather_code": 2
  }
}
```

Esse formato tem nomes de campo com `_2m` no meio, números de código pro
clima em vez de palavras, e uma estrutura pensada pra ser genérica pra
qualquer cliente que consuma a API — não pra ser conveniente pro nosso
app específico. Se o resto do app trabalhasse diretamente com esse JSON
cru, qualquer tela que precisasse da temperatura teria que saber, de
cor, que o campo se chama `temperature_2m` e está dentro de `current`.

A solução é ter **duas representações separadas** da mesma informação:

1. Uma que espelha exatamente o que a API devolve (chamamos de **DTO** —
   Data Transfer Object). Esse formato só existe pra facilitar a
   conversão do JSON.
2. Uma que representa o que o *nosso app* entende por "previsão do
   tempo" — os **domain models** — sem nenhum resquício de como a API
   está estruturada por fora.

E existe uma etapa entre as duas: uma função que converte de um formato
pro outro (chamamos de **mapper**). Assim, se um dia a API mudar o nome
de um campo, só o mapper precisa mudar — o resto do app nem percebe.

## O que vamos construir

- `City` e `WeatherForecast` — os domain models, o formato que o resto do
  app vai efetivamente usar.
- Os DTOs — o formato cru, espelhando o JSON da API.
- Os mappers — a conversão de um pro outro.
- Dois serviços que sabem buscar dados na internet: um pra buscar
  cidades pelo nome, outro pra buscar a previsão de uma cidade.

Ainda não tem nenhuma tela usando isso — essa camada de dados vai ganhar
vida na Aula 5, quando ligarmos ela a um `ViewModel`.

## Passo a passo

### Os dois formatos, lado a lado

Domain models — o formato "limpo" que o resto do app vai usar:

```kotlin
data class City(
    val id: Long,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

data class WeatherForecast(
    val currentTemperature: Double,
    val weatherCode: Int,
    val dailyMaxTemperatures: List<Double>,
    val dailyMinTemperatures: List<Double>
)
```

DTOs — o formato cru, espelhando exatamente o JSON da API:

```kotlin
data class GeocodingResponseDto(
    val results: List<GeocodingResultDto>?
)

data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

data class ForecastResponseDto(
    val current: CurrentDto,
    val daily: DailyDto
)

data class CurrentDto(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int
)

data class DailyDto(
    @SerializedName("temperature_2m_max") val maxTemperatures: List<Double>,
    @SerializedName("temperature_2m_min") val minTemperatures: List<Double>
)
```

Repare que `CurrentDto` e `DailyDto` têm uma anotação em cada campo,
`@SerializedName("...")`. Isso resolve um problema específico: o campo no
JSON se chama `temperature_2m` (com número e underscore no meio — não é
um nome válido de variável Kotlin do nosso padrão), mas dentro do app
queremos chamar essa propriedade de `temperature`, num estilo mais
limpo. A anotação diz ao Gson (a biblioteca que faz esse parsing):
"quando vir a chave `temperature_2m` no JSON, guarda o valor aqui, numa
propriedade chamada `temperature`". Sem ela, o Gson tentaria casar o
nome do campo do JSON com o nome da propriedade Kotlin exatamente
como estão escritos, e `temperature` não bateria com `temperature_2m`
— o valor simplesmente viria como `null` (ou zero), sem nenhum aviso.

`GeocodingResponseDto`/`GeocodingResultDto`, por outro lado, não
precisam de `@SerializedName` — os nomes dos campos no JSON dessa API
(`id`, `name`, `country`, `latitude`, `longitude`) já batem exatamente
com os nomes que demos às propriedades Kotlin, então o Gson consegue
casar os dois automaticamente.

### Buscando dados pela internet

```kotlin
interface GeocodingApiService {
    @GET("v1/search")
    suspend fun searchCity(@Query("name") name: String): GeocodingResponseDto
}
```

Essa interface descreve, de forma declarativa, uma chamada HTTP: "existe
um endereço `v1/search`, que recebe um parâmetro `name` na URL, e
devolve um `GeocodingResponseDto`". Não tem nenhuma implementação aqui —
quem lê essa descrição e realmente faz a chamada de rede é o Retrofit,
configurado na próxima seção.

O mesmo padrão se repete pra buscar a previsão do tempo de uma cidade
específica, dado sua latitude e longitude:

```kotlin
interface ForecastApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponseDto
}
```

Repare que três desses parâmetros já vêm com um valor padrão, em vez de
precisar ser informado toda vez que chamarmos essa função. Isso não é só
conveniência — cada um desses valores está dizendo à API, em linguagem
dela, exatamente o que queremos de volta:

- `current = "temperature_2m,weather_code"` pede a temperatura **agora**
  e um código representando a condição do tempo agora (sol, chuva,
  nublado...).
- `daily = "temperature_2m_max,temperature_2m_min"` pede a temperatura
  máxima e mínima **do dia**.
- `timezone = "auto"` diz pra API descobrir sozinha o fuso horário do
  lugar, a partir da latitude/longitude — importante pra ela saber onde
  um "dia" começa e termina, o que afeta o cálculo de máxima/mínima.

Cada um desses parâmetros aceita uma lista de valores separados por
vírgula (é por isso que `current` e `daily` são uma única string com
vírgula no meio, não uma lista Kotlin de verdade — é assim que a API
espera receber esse tipo de parâmetro numa URL). Neste projeto sempre
pedimos a mesma coisa, por isso faz sentido esses três já virem com um
valor padrão, em vez de o chamador ter que repetir isso toda vez.

> Guia de referência com todos os parâmetros e campos dessa API (inclusive
> os que este projeto não usa, e a tabela de códigos de clima):
> [`docs/open-meteo-api-referencia.md`](open-meteo-api-referencia.md).

### Configurando o cliente HTTP

```kotlin
@Provides
@Singleton
@GeocodingRetrofit
fun provideGeocodingRetrofit(gson: Gson): Retrofit = Retrofit.Builder()
    .baseUrl("https://geocoding-api.open-meteo.com/")
    .addConverterFactory(GsonConverterFactory.create(gson))
    .build()
```

> **`Retrofit.Builder()`** → começa a montar o cliente HTTP peça por
> peça.
>
> **`.baseUrl(...)`** → define o endereço base; toda chamada declarada
> nas interfaces de serviço completa esse prefixo (por exemplo,
> `v1/search` vira
> `https://geocoding-api.open-meteo.com/v1/search`).
>
> **`.addConverterFactory(...)`** → ensina o Retrofit a transformar o
> JSON cru que a API devolve em objetos Kotlin (os DTOs) automaticamente
> — sem isso, teríamos que fazer esse parsing manualmente.
>
> **`.build()`** → finaliza a construção e devolve o cliente pronto.

Um detalhe importante deste projeto: a API do Open-Meteo tem **dois
endereços diferentes** — um pra buscar cidades (`geocoding-api.open-meteo.com`)
e outro pra buscar a previsão (`api.open-meteo.com`). Por isso montamos
dois clientes Retrofit distintos, e usamos anotações próprias
(`@GeocodingRetrofit`, `@ForecastRetrofit`) só pra o Hilt conseguir
diferenciar qual dos dois entregar em cada lugar — sem elas, o Hilt não
teria como saber qual dos dois `Retrofit` idênticos em tipo você quer.

### Convertendo entre os dois formatos

```kotlin
fun ForecastResponseDto.toDomain(): WeatherForecast = WeatherForecast(
    currentTemperature = current.temperature,
    weatherCode = current.weatherCode,
    dailyMaxTemperatures = daily.maxTemperatures,
    dailyMinTemperatures = daily.minTemperatures
)
```

Essa função pega um DTO (o formato cru, espelhando o JSON) e devolve um
domain model (o formato que o app usa). É uma tradução simples, campo a
campo — mas é essa tradução que garante que o resto do app nunca precisa
saber que, na resposta da API, a temperatura mora dentro de um objeto
chamado `current`.

## Critério de aceite

`./gradlew.bat assembleDebug` termina com sucesso. Isso confirma que toda
a camada de dados (modelos, DTOs, mappers, serviços de rede) está
correta e se encaixa — mesmo sem nenhuma tela consumindo ela ainda, o
que só vai acontecer na Aula 5.
