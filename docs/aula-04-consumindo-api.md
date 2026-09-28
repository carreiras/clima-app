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
específica, dado sua latitude e longitude.

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
