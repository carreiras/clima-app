# Referência — API Open-Meteo

Documento de consulta rápida sobre os parâmetros e campos da
[Open-Meteo](https://open-meteo.com/en/docs) que este projeto usa. Não é
uma aula — é uma referência pra consultar sempre que aparecer um
parâmetro de API que você não lembra o que significa. As descrições
abaixo vêm da documentação oficial, resumidas e traduzidas.

O projeto usa dois endpoints diferentes, com hosts diferentes — por isso
existem dois serviços Retrofit (`GeocodingApiService` e
`ForecastApiService`), explicados na [Aula 4](aula-04-consumindo-api.md).

## Geocoding API — buscar uma cidade pelo nome

`https://geocoding-api.open-meteo.com/v1/search`

| Parâmetro | O que é |
|---|---|
| `name` | O texto digitado pelo usuário. A busca ignora maiúsculas/minúsculas e acentos, e precisa bater com o **começo** do nome da cidade (não o meio). Com 2 caracteres, busca só resultado exato; com 3 ou mais, já aceita prefixos parecidos. |

A resposta traz uma lista de resultados. A API devolve bem mais campos
do que usamos — este projeto só aproveita estes:

| Campo | O que é |
|---|---|
| `id` | Identificador único daquela cidade no banco da Open-Meteo. |
| `name` | Nome da cidade. |
| `country` | Nome do país. |
| `latitude`, `longitude` | Coordenadas geográficas — é isso que a Forecast API vai usar pra saber de qual lugar buscar o clima. |

A API também devolve `country_code`, `population`, `timezone`,
`admin1`...`admin4` (região/estado) e outros campos que este projeto
ignora por simplicidade (nosso `GeocodingResultDto`, na Aula 4, só lê os
cinco campos da tabela acima).

## Forecast API — buscar a previsão de um lugar

`https://api.open-meteo.com/v1/forecast`

| Parâmetro | O que é | Valor que usamos |
|---|---|---|
| `latitude`, `longitude` | Coordenadas do lugar (vêm do resultado da Geocoding API). | — |
| `current` | Lista de variáveis que você quer "agora" (instante presente), separadas por vírgula. | `temperature_2m,weather_code` |
| `daily` | Lista de variáveis agregadas por dia, separadas por vírgula. | `temperature_2m_max,temperature_2m_min` |
| `timezone` | Fuso horário usado pra calcular "o dia" (importante pro `daily` fazer sentido — sem isso, a API não sabe onde um dia começa e termina). | `auto` (a API descobre sozinha, a partir da latitude/longitude) |

Por que `current` e `daily` são strings com vírgula, e não uma lista de
verdade? Porque é assim que a Open-Meteo espera receber esses parâmetros
na URL — uma URL só carrega texto, então "lista de coisas separadas por
vírgula dentro de uma única string" é a forma comum de mandar uma lista
pra uma API HTTP.

### O que cada variável pedida significa

| Variável | O que é |
|---|---|
| `temperature_2m` | Temperatura do ar a 2 metros do chão (a "temperatura ambiente" que a gente sente), em °C. |
| `weather_code` | Um número que representa a condição do tempo (ver tabela abaixo) — não é a temperatura, é tipo "ensolarado", "chuva fraca", "tempestade", só que como código. |
| `temperature_2m_max` / `temperature_2m_min` | Temperatura máxima e mínima do dia inteiro. |

### Tabela de códigos de clima (WMO)

`weather_code` segue um padrão internacional (WMO — World Meteorological
Organization). Os mais comuns:

| Código | Significado |
|---|---|
| 0 | Céu limpo |
| 1 | Predominantemente limpo |
| 2–3 | Parcialmente nublado a nublado |
| 45 | Neblina |
| 61 | Chuva fraca |
| 71 | Neve fraca |
| 95 | Tempestade |

> Neste projeto, `weatherCode` fica guardado no `WeatherForecast` (Aula 4)
> como número — ainda não convertemos ele pra um texto ou ícone na tela.
> Isso seria uma extensão natural do projeto, mas está fora do escopo
> das 10 aulas atuais.

## Referência completa

Lista completa de parâmetros e campos, incluindo os que este projeto não
usa: [documentação oficial da Open-Meteo](https://open-meteo.com/en/docs).
