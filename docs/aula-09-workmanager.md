# Aula 9 — WorkManager: sync em background

## Objetivo

Até agora, os dados de uma cidade favorita só atualizam quando o usuário
abre a tela de Detalhes dela. Isso significa que, se ele só olhar a
lista de Favoritos, pode estar vendo uma temperatura de dias atrás.
Nesta aula vamos fazer o app atualizar essas cidades sozinho, de tempos
em tempos, mesmo com ele fechado.

## O problema: "rodar código de tempos em tempos" é mais difícil do que parece

Uma ideia ingênua seria criar uma `Thread` que dorme um tempo e roda de
novo, enquanto o app estiver aberto. O problema é: o Android mata
processos em segundo plano pra economizar bateria, então essa `Thread`
simplesmente para de existir assim que o usuário sai do app. Outra
ideia, ainda pior, seria ignorar isso e tentar forçar o processo a
continuar rodando — isso é exatamente o tipo de comportamento que o
Android passou a bloquear agressivamente nas últimas versões, porque
prejudica a bateria de todo mundo.

O **WorkManager** existe pra resolver isso de um jeito que o sistema
operacional respeita: você descreve o que quer rodar e com que
condições (por exemplo, "só com internet"), e entrega essa
responsabilidade pro Android decidir o melhor momento de executar —
mesmo que o app esteja fechado, e mesmo depois de o aparelho reiniciar.

## O que vamos construir

- `SyncWorker` — o código que efetivamente busca a previsão de todas as
  cidades favoritas.
- Um agendamento periódico desse trabalho, a cada 6 horas, só quando
  houver conexão de rede.

## Passo a passo

### O trabalho em si

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WeatherRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val favorites = repository.getFavorites().first()
        favorites.forEach { city ->
            repository.getForecast(city).first()
        }
        return Result.success()
    }
}
```

`doWork()` é o método que o WorkManager chama quando decide que é hora
de rodar esse trabalho. Repare que a lógica em si é simples: pega todas
as cidades favoritas, e pra cada uma, pede a previsão de novo. Não
precisamos escrever nenhum código novo pra "salvar" o resultado — isso
já acontece automaticamente, porque `getForecast` (Aula 8) já atualiza o
banco local como efeito colateral toda vez que é chamado. O Worker só
precisa "puxar" essa função pra cada cidade; quem decide o que fazer com
a resposta já foi decidido antes.

Por que `CoroutineWorker`, e não `Worker`? Porque nossas funções (buscar
da rede, ler o banco) são `suspend` — pensadas pra rodar com coroutines.
`CoroutineWorker` é a versão do WorkManager preparada pra isso.

### Avisando o Android sobre esse trabalho

```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
    .setConstraints(
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    )
    .build()
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "sync-favorites",
    ExistingPeriodicWorkPolicy.KEEP,
    syncRequest
)
```

> **`PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)`** →
> descreve "quero que o `SyncWorker` rode, repetidamente, a cada 6
> horas" (esse é o intervalo mínimo permitido pelo Android pra trabalho
> periódico).
>
> **`.setConstraints(...)`** → só executa quando a condição for
> satisfeita — aqui, só com internet conectada. Se não tiver rede na
> hora que seria pra rodar, o Android espera até ter.
>
> **`.build()`** → finaliza a descrição do pedido de trabalho.
>
> **`enqueueUniquePeriodicWork(...)`** → registra esse pedido com um
> nome único (`"sync-favorites"`). O `ExistingPeriodicWorkPolicy.KEEP`
> diz: se esse trabalho já estava agendado (por exemplo, de uma execução
> anterior do app), não crie outro do zero, mantém o que já existe — sem
> isso, toda vez que o app abrisse, um novo agendamento se acumularia em
> cima do anterior.

### Uma peça extra que essa combinação de ferramentas exige

Como o `SyncWorker` recebe um `WeatherRepository` injetado (via Hilt) em
vez de construir ele mesmo, precisamos ensinar o WorkManager a pedir
esse `Worker` pronto pro Hilt, em vez de tentar criar ele sozinho (o
WorkManager não sabe, por padrão, o que é injeção de dependência). Isso
é feito registrando uma fábrica especial (`HiltWorkerFactory`) na
configuração do WorkManager, dentro da nossa `ClimaApplication` — um
ajuste de configuração, não uma lógica de negócio nova.

## Extra: agora dá pra ver o resultado do sync

Como o dado que o `SyncWorker` atualiza (a última temperatura salva)
não aparecia em nenhuma tela até aqui, adicionamos a exibição dele na
tela de Favoritos:

```kotlin
supportingContent = {
    val temperatureText = city.lastTemperature?.let { "$it°C" } ?: "Ainda sem dado salvo"
    Text(text = temperatureText)
}
```

Cada cidade favorita agora mostra a última temperatura salva, ou uma
mensagem indicando que ainda não há dado (caso tenha sido favoritada
sem nunca ter tido a previsão buscada). Isso deixa visível, na prática,
o que o `SyncWorker` desta aula está fazendo em segundo plano.

> **Se você favoritar uma cidade e ela aparecer como "Ainda sem dado
> salvo" mesmo já tendo visto a temperatura em Detalhes:** foi
> exatamente isso que aconteceu ao testar esta aula. A causa (e a
> correção) não é sobre o `SyncWorker` — é sobre como `toggleFavorite`
> salvava a cidade favoritada sem levar a temperatura já conhecida
> junto. Está explicado na seção **"Um bug encontrado depois"** da
> [Aula 8](aula-08-favoritos-offline.md#um-bug-encontrado-depois-favoritar-salvava-temperatura-vazia).

## Critério de aceite

Testado no dispositivo físico: o log do sistema mostra
`Starting work for com.example.climaapp.work.SyncWorker` seguido de
`Worker result SUCCESS` — confirma que o trabalho agendado realmente
rodou e terminou sem erro.

> **Nota sobre testar manualmente:** o comando
> `adb shell cmd jobscheduler run -f <package> <jobId>` (sugerido pra
> forçar a execução) só funciona enquanto o job está registrado e
> pendente no JobScheduler do Android. Pra trabalho **periódico**, assim
> que uma execução termina, o WorkManager só re-registra a próxima
> ocorrência perto da hora de rodar de novo — então esse comando pode
> retornar "job não encontrado" logo depois de uma execução bem-sucedida.
> Isso não é um bug: nesse caso, ler o log (`adb logcat | grep
> SyncWorker`) é a forma mais confiável de confirmar que o trabalho
> rodou.
