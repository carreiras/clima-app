# Aula 11 — Tela de configurações (intervalo e limiar)

## Objetivo

Até aqui, duas decisões importantes do app estavam fixas em código: de
quanto em quanto tempo o `SyncWorker` roda (6 horas) e qual temperatura
dispara notificação (30°C). Nesta aula, o usuário passa a poder ajustar
os dois, numa tela de configurações — e essa mudança precisa realmente
valer, mesmo sem reabrir o app.

## O problema: onde guardar duas preferências simples, e como aplicá-las de verdade

Guardar dois valores (um número de minutos, uma temperatura) não
justifica um banco de dados como o Room — seria complexidade demais
para pouca coisa. O Android já tem uma ferramenta pronta pra isso:
**SharedPreferences**, um armazenamento simples de chave-valor que
persiste sozinho, sem precisar de tabelas, DAOs ou migrations.

Só guardar o valor não é suficiente, porém. O intervalo do
`SyncWorker` já tinha sido **agendado** no WorkManager (Aula 9) — só
mudar o número salvo não faz o agendamento existente mudar sozinho. É
preciso, explicitamente, pedir pro WorkManager substituir o
agendamento antigo pelo novo assim que o usuário mexer na
configuração.

## O que vamos construir

- `SettingsRepository` — lê e escreve as duas preferências.
- `SyncScheduler` — o agendamento do `SyncWorker`, que antes vivia
  direto na `MainActivity`, agora é reutilizável: tanto a abertura do
  app quanto a tela de configurações podem chamar ele.
- `SettingsScreen` — lista de intervalos fixos (`RadioButton`), um
  `Slider` pro limiar, e um botão "Sincronizar agora".
- O `NotificationHelper` e o `SyncWorker` passam a usar o limiar
  salvo, em vez do valor fixo de 30°C.
- Um botão "Configurações" na tela de Busca, no mesmo padrão do botão
  "Favoritos" da Aula 8 (`TextButton` + nova rota no `NavHost`), pra
  chegar na tela nova.

## Passo a passo

### Guardando as preferências

```kotlin
fun getSyncIntervalMinutes(): Long =
    preferences.getLong(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES)

fun setSyncIntervalMinutes(minutes: Long) {
    preferences.edit().putLong(KEY_SYNC_INTERVAL_MINUTES, minutes).apply()
}
```

`SharedPreferences` funciona como um dicionário: cada valor tem uma
chave (string) e um valor padrão, usado caso nada tenha sido salvo
ainda — é assim que garantimos que o app, na primeira vez que abre,
já se comporta com os mesmos valores que sempre teve (6 horas, 30°C),
sem precisar de nenhuma migração de dados.

Como toda peça que o Hilt precisa entregar pra alguém, o
`SharedPreferences` também precisa de uma receita numa gaveta — o
mesmo padrão `@Provides`/`@Singleton` já usado no `NetworkModule`
(Aula 4) e no `DatabaseModule` (Aula 7):

```kotlin
@Provides
@Singleton
fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
    context.getSharedPreferences("clima_app_settings", Context.MODE_PRIVATE)
```

### Reagendando de verdade quando a configuração muda

```kotlin
fun schedule(
    intervalMinutes: Long,
    policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
) {
    val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
        .setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        )
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "sync-favorites",
        policy,
        syncRequest
    )
}
```

Essa função tem um parâmetro de política com valor padrão `KEEP` — o
mesmo comportamento da Aula 9: ao abrir o app normalmente, se já existe
um agendamento, não mexe nele. Mas a tela de configurações chama essa
mesma função passando `ExistingPeriodicWorkPolicy.REPLACE`
explicitamente, forçando a troca do agendamento antigo pelo novo,
imediatamente:

```kotlin
fun setSyncInterval(minutes: Long) {
    settingsRepository.setSyncIntervalMinutes(minutes)
    _syncIntervalMinutes.value = minutes
    syncScheduler.schedule(minutes, policy = ExistingPeriodicWorkPolicy.REPLACE)
}
```

Ter as duas políticas disponíveis, escolhidas por quem chama a função,
evita um problema sutil: se o `SyncScheduler` sempre usasse `REPLACE`,
o app reagendaria (e resetaria o cronômetro interno do WorkManager)
toda vez que fosse simplesmente aberto — não só quando o usuário
realmente muda algo.

Com o agendamento agora vivendo numa classe própria, a `MainActivity`
fica só com a responsabilidade de chamar ela ao abrir, lendo o
intervalo que estiver salvo:

```kotlin
class MainActivity : ComponentActivity() {

    @Inject lateinit var syncScheduler: SyncScheduler

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
        syncScheduler.schedule(settingsRepository.getSyncIntervalMinutes())
```

Repare que a injeção aqui é diferente da que já vimos em `ViewModel`s
até agora (`@Inject constructor(...)`, no construtor). Uma `Activity` é
criada pelo próprio Android, não pelo Hilt — então não dá pra pedir as
dependências no construtor, porque quem chama esse construtor é o
sistema, não a gente. A saída é a **injeção em campo**: `@Inject
lateinit var`, que o Hilt preenche automaticamente logo depois de
`super.onCreate(...)` rodar, graças ao `@AndroidEntryPoint` que já
anota essa classe desde a Aula 2.

### A tela: lista de opções + slider

```kotlin
SYNC_INTERVAL_OPTIONS.forEach { option ->
    Row(
        modifier = Modifier
            .selectable(
                selected = option.minutes == syncIntervalMinutes,
                onClick = { viewModel.setSyncInterval(option.minutes) }
            )
    ) {
        RadioButton(
            selected = option.minutes == syncIntervalMinutes,
            onClick = { viewModel.setSyncInterval(option.minutes) }
        )
        Text(text = option.label)
    }
}
```

Cada opção de intervalo é uma linha com `RadioButton` — só uma pode
estar selecionada por vez, comparando o valor salvo com o valor daquela
opção. Repare que o `onClick` está duplicado (na `Row` inteira via
`.selectable`, e no `RadioButton` também) — isso é intencional: permite
tocar tanto no círculo quanto em qualquer parte da linha (o texto,
por exemplo) pra selecionar aquela opção, o que é mais fácil de acertar
com o dedo do que só o círculo pequeno.

O limiar usa um `Slider` do Material3, de 0 a 50°C, mostrando o valor
atual como texto logo acima — cada movimento do slider já salva o novo
valor, sem precisar de um botão "salvar" separado.

### O SyncWorker passa a usar o limiar salvo

```kotlin
override suspend fun doWork(): Result {
    val favorites = repository.getFavorites().first()
    val threshold = settingsRepository.getTemperatureThreshold()
    favorites.forEach { city ->
        val result = repository.getForecast(city).last()
        result.getOrNull()?.let { forecast ->
            NotificationHelper.notifyIfThresholdCrossed(applicationContext, city, forecast.currentTemperature, threshold)
        }
    }
    return Result.success()
}
```

A única mudança em relação à Aula 10 é ler `threshold` uma vez, no
início do `doWork()`, e passar ele adiante pra
`notifyIfThresholdCrossed`. Antes (Aula 10), essa função tinha o
limiar (`THRESHOLD_CELSIUS = 30.0`) escrito dentro dela mesma, como uma
constante; agora ela recebe o valor de fora, como parâmetro
(`thresholdCelsius: Float`), sem saber nem se importar de onde ele
veio — podia ser fixo, podia vir do `SettingsRepository`, tanto faz pra
essa função. Essa troca (constante interna → parâmetro externo) é o
que torna qualquer comportamento configurável de verdade: a função que
decide "notificar ou não" para de guardar a regra, e passa só a aplicar
a regra que alguém de fora decidiu.

### Por que não tem opção de 30 segundos, 1, 5 ou 10 minutos

A ideia inicial era oferecer intervalos bem curtos, pra facilitar testar
o app. Só que o WorkManager tem uma trava rígida: **nenhum trabalho
periódico pode rodar com menos de 15 minutos de intervalo** — é uma
regra do próprio Android (evita apps abusando de execução em segundo
plano e gastando bateria de todo mundo), não uma limitação do nosso
código. Se a gente oferecesse "30 segundos" na lista e o WorkManager
simplesmente ignorasse e usasse 15 minutos mesmo assim, o aluno teria
uma informação errada na tela.

A solução foi separar os dois casos de uso, que na verdade são
diferentes:

```kotlin
fun scheduleOnce() {
    val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        )
        .build()
    WorkManager.getInstance(context).enqueue(syncRequest)
}
```

`OneTimeWorkRequest` não tem a trava de 15 minutos — porque não é
periódico, é uma execução única, disparada uma vez e pronto. O botão
"Sincronizar agora" usa exatamente isso: não mexe no agendamento
periódico configurado acima (continuam duas coisas independentes), só
roda o `SyncWorker` imediatamente, uma vez, pra quem quiser testar sem
esperar.

## Critério de aceite

Testado no dispositivo físico: mudar o intervalo pra "15 minutos" e o
limiar pra 38°C — o log do sistema confirma um novo agendamento do
`SyncWorker` sendo criado na hora (`Scheduling work ID ...`, com um ID
diferente do agendamento anterior). Fechar e reabrir o app mantém os
dois valores escolhidos. O botão "Sincronizar agora" dispara e conclui
o `SyncWorker` em menos de 2 segundos, confirmado pelo log
(`Worker result SUCCESS`). Ao final do teste, os valores voltam para os
padrões de produção (6 horas, 30°C).
