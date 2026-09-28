# Design — App de Previsão do Tempo (clima-app)

**Data:** 2026-09-28
**Status:** aprovado para implementação (spec), aguardando aprovação por aula na prática

## Contexto

Projeto didático em Kotlin/Jetpack Compose para consumo de uma API pública
de clima (Open-Meteo). Baseado no roadmap original de 5 aulas
(`docs/roadmap-app-clima-kotlin.md`), mas reorganizado em aulas mais curtas
e granulares, sem testes unitários.

Turma já viu: consumo de API simples (crypto monitor) e persistência local
básica (to-do list + SQLite). Este projeto adiciona arquitetura em camadas,
DI com Hilt, cache offline-first com Room, WorkManager e notificações.

**Fora de escopo (decisão explícita do usuário):** testes unitários/instrumentados
não fazem parte deste projeto.

**Teto de tempo:** sem teto rígido de 3h — o usuário optou por manter mais
profundidade (Hilt, Room, WorkManager, Navigation Compose) em vez de cortar
features, mesmo que isso exceda o teto inicial.

## API

[Open-Meteo](https://open-meteo.com/en/docs) — gratuita, sem API key.

- Geocoding (cidade → lat/lon): `https://geocoding-api.open-meteo.com/v1/search?name={cidade}`
- Previsão: `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,weather_code&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min&timezone=auto`

## Stack e dependências

Projeto já existente usa `applicationId = "com.example.climaapp"` (não
`com.fiap.weatherapp` como no roadmap original) — todo o código usa esse
pacote base.

Dependências novas a adicionar via `gradle/libs.versions.toml` (uma por
aula, nunca todas de uma vez):

| Dependência | Uso | Aula que introduz |
|---|---|---|
| `androidx.navigation:navigation-compose` | Navegação entre telas | 3 |
| `com.squareup.retrofit2:retrofit` | Cliente HTTP | 4 |
| `com.squareup.retrofit2:converter-gson` | Parse de JSON → objetos Kotlin | 4 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Coroutines/Flow | 4 |
| `androidx.room:room-runtime` + `room-ktx` + `room-compiler` (KSP) | Cache local | 7 |
| `com.google.devtools.ksp` (plugin) | Processador de anotações do Room/Hilt | 2 e 7 |
| `com.google.dagger:hilt-android` + `hilt-android-compiler` (KSP) + plugin `com.google.dagger.hilt.android` | Injeção de dependência | 2 |
| `androidx.hilt:hilt-navigation-compose` | Hilt + ViewModel em Compose | 3 |
| `androidx.work:work-runtime-ktx` | Background sync | 9 |

> **Nota de simplicidade:** optei por `converter-gson` em vez de
> `kotlinx.serialization` (usado no roadmap original) porque não exige
> plugin Gradle extra nem `@Serializable` em cada DTO — menos configuração
> para o aluno entender antes de chegar na parte de rede.

## Estrutura de pacotes

```
com.example.climaapp
├── data
│   ├── local/       (Room: entities, dao, database)
│   ├── remote/      (Retrofit service, DTOs)
│   └── repository/  (WeatherRepositoryImpl)
├── domain
│   ├── model/       (City, WeatherForecast)
│   └── repository/  (WeatherRepository interface)
├── di/              (Hilt modules)
├── ui
│   ├── search/
│   ├── details/
│   ├── favorites/
│   └── theme/       (já existe)
└── work/            (SyncWorker)
```

## Quebra em aulas

Cada aula vira um arquivo `docs/aula-XX-slug.md`. Estrutura interna de
cada arquivo:

1. **Objetivo** (1-2 frases, o "porquê" pedagógico)
2. **O que vamos construir** (lista curta de tarefas)
3. **Passo a passo com código explicado** (ver formato abaixo)
4. **Critério de aceite** (como o aluno confirma que funcionou)

| # | Aula | Conteúdo |
|---|------|----------|
| 1 | Setup & dependências | Cada dependência nova explicada isoladamente no `build.gradle.kts`/`libs.versions.toml`, antes de qualquer uma ser usada |
| 2 | Arquitetura em camadas + Hilt | Estrutura de pacotes vazia, `Application` com `@HiltAndroidApp`, módulos Hilt vazios |
| 3 | Navigation Compose | 3 rotas (`Search`, `Details`, `Favorites`) com telas placeholder |
| 4 | Consumindo a API | `WeatherApiService`, DTOs, mappers para `City`/`WeatherForecast` |
| 5 | Repository + Flow + ViewModel | `WeatherRepository` expondo `Flow<Result<T>>`, `SearchViewModel` com estado sealed class |
| 6 | Tela de Busca real | UI Compose ligada ao ViewModel: campo de busca, lista, card de previsão, tratamento de erro |
| 7 | Room como cache | Entities, DAO, `AppDatabase` |
| 8 | Favoritos offline-first | Repository decide cache vs rede (single source of truth), favoritar/desfavoritar, tela de Favoritos funcionando sem internet |
| 9 | WorkManager | `SyncWorker` (CoroutineWorker), agendamento periódico com constraint de rede |
| 10 | Notificações locais | Canal de notificação (Android 8+), permissão (Android 13+), disparo quando cruza limiar de temperatura |

Critérios de aceite por aula seguem os mesmos do roadmap original
(`docs/roadmap-app-clima-kotlin.md`), adaptados quando a aula for dividida.

## Formato de explicação de código (estilo "bloco a bloco")

Baseado em `img/Captura de tela 2026-09-28 134926.png`: cada chamada
encadeada (builders, DSLs) é mostrada inteira uma vez em bloco de código,
e depois cada trecho ganha sua própria explicação isolada em blockquote,
na ordem em que aparece. Exemplo:

````markdown
```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.open-meteo.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

> **`val retrofit =`** → cria uma variável que vai guardar o cliente HTTP já configurado.
>
> **`Retrofit.Builder()`** → começa a "montar" o Retrofit peça por peça, como um builder.
>
> **`.baseUrl(...)`** → define o endereço base da API; toda chamada completa esse prefixo.
>
> **`.addConverterFactory(...)`** → ensina o Retrofit a transformar o JSON da resposta em objetos Kotlin.
>
> **`.build()`** → finaliza a construção e devolve o `Retrofit` pronto pra uso.
````

**Quando usar a quebra bloco a bloco (regra revisada em 2026-09-28):**
só para sintaxe encadeada genuinamente nova/complexa — builders (como o
exemplo do `Retrofit.Builder()` acima) e DSLs (o corpo de um `NavHost {
}`, por exemplo). **Não usar esse formato para anotações simples**
(`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Module`, `@Composable`,
`@Inject` etc.) — nada de blockquote dissecando cada `@Anotacao`
separadamente. Anotações são explicadas em prosa corrida, como parte da
explicação conceitual do arquivo/classe (ver abaixo), não como sintaxe a
ser decodificada símbolo por símbolo.

**Tom didático (público: alunos).** Cada aula deve:

- Abrir com uma analogia ou situação concreta antes de qualquer sintaxe
  — explicar o **problema** que a ferramenta/padrão resolve antes de
  mostrar **como** ela resolve.
- Explicar o papel de cada classe/arquivo em linguagem corrida e
  conceitual — o que ele faz e por que existe — sem depender de listar
  cada anotação com sua explicação técnica isolada.
- Definir todo termo técnico na primeira vez que aparece (não assumir que
  "injeção de dependência", "processador de anotações" etc. já são
  conhecidos), mesmo que isso já tenha sido explicado numa aula anterior
  — um lembrete curto já basta na 2ª aparição.
- Preferir frases curtas e parágrafos curtos a blocos longos de texto.
- Fechar o "Critério de aceite" relembrando, em uma frase, o que aquele
  resultado prova (não só "compila", mas "compila, e isso confirma que
  X").

Esse padrão vale para todos os `docs/aula-XX-*.md`, retroativo às aulas
já escritas (1 e 2 foram revisadas nesse tom em 2026-09-28, primeiro
para um tom mais didático geral, depois revisadas de novo no mesmo dia
para remover a dissecação de anotações linha a linha).

## Organização de arquivos

- `README.md` (raiz do projeto): índice geral — objetivo do app, stack,
  link para cada `docs/aula-XX-*.md` em ordem.
- `docs/aula-01-setup-dependencias.md` até `docs/aula-10-notificacoes.md`.
- `docs/roadmap-app-clima-kotlin.md`: mantido como documento de origem/
  referência histórica (não é mais o guia ativo).
- `docs/superpowers/specs/2026-09-28-clima-app-design.md`: este arquivo.

## Fluxo de trabalho (obrigatório)

- Implementação **uma aula por vez**: código da aula + arquivo `.md`
  correspondente.
- Após cada aula, o usuário testa no dispositivo físico (tablet, via
  depuração sem fio já configurada — ver `docs/wireless-debugging-setup.md`)
  antes de eu seguir para a próxima aula.
- **Nenhum commit é feito automaticamente.** Tudo fica no working tree.
  Commits só acontecem quando o usuário autorizar explicitamente, sempre
  direto na branch `main`. Nenhum `git push` em nenhuma circunstância,
  salvo pedido explícito futuro.

## Tratamento de erros e casos de borda

- Falha de rede (sem internet, timeout, cidade não encontrada): tratada
  explicitamente via estado `Error` no ViewModel, nunca deixando exceção
  não tratada estourar a UI.
- Sem favoritos ainda: tela de Favoritos mostra estado vazio simples
  (texto), não é uma feature separada.
- Permissão de notificação negada (Android 13+): app continua funcional,
  só não dispara notificações — sem crash nem bloqueio de fluxo.
