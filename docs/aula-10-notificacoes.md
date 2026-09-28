# Aula 10 — Notificações locais (última aula!)

## Objetivo

Chegamos na última peça do app: avisar o usuário quando uma cidade
favorita está com a temperatura acima de um certo valor — mesmo que ele
não esteja com o app aberto no momento. Isso conecta com o trabalho da
Aula 9: o `SyncWorker` já atualiza os dados em segundo plano; agora ele
também vai decidir se vale a pena avisar alguém sobre isso.

## O problema: notificação tem duas permissões diferentes, em momentos diferentes

Antes de qualquer notificação aparecer, o Android exige duas coisas — e
é fácil confundir as duas achando que são a mesma:

1. Um **canal de notificação**, criado em código, existindo desde antes
   da primeira notificação ser enviada (obrigatório a partir do Android
   8). É uma "categoria" que o usuário pode configurar depois (silenciar,
   mudar prioridade) nas configurações do sistema.
2. **Permissão explícita do usuário**, concedida por um diálogo do
   sistema, em tempo de execução (obrigatório a partir do Android 13).
   Sem essa permissão, mesmo com um canal criado corretamente, nenhuma
   notificação chega a aparecer.

Faltando qualquer uma das duas, a notificação simplesmente não aparece
— sem erro, sem crash. É um comportamento silencioso por design, então
vale testar de propósito, não só confiar que "deve estar funcionando".

## O que vamos construir

- `NotificationHelper` — cria o canal e decide quando notificar.
- O canal sendo criado assim que o app inicia.
- O `SyncWorker` (Aula 9) chamando essa decisão pra cada cidade
  favorita, depois de buscar a previsão atualizada.
- Pedido da permissão de notificação a partir do Android 13.

## Passo a passo

### Decidindo quando notificar

```kotlin
fun notifyIfThresholdCrossed(context: Context, city: City, temperature: Double) {
    if (temperature < THRESHOLD_CELSIUS) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("${city.name} está quente")
        .setContentText("Temperatura atual: ${temperature}°C")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify(city.id.toInt(), notification)
}
```

Essa função checa duas coisas antes de sequer montar a notificação: se
a temperatura realmente cruzou o limiar, e (só em Android 13+) se o
usuário realmente concedeu a permissão. Só depois disso ela monta e
envia a notificação de verdade. O limiar (`THRESHOLD_CELSIUS = 30.0`)
fica fixo em código — sem tela de configurações, pra manter esta aula
simples.

### Pedindo a permissão

```kotlin
val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { /* resultado tratado silenciosamente — notificação só some se negado */ }

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

Isso dispara o diálogo do sistema assim que o app abre. Repare que não
tratamos o caso de recusa de nenhum jeito especial — o app continua
funcionando normalmente, só não notifica. Essa é uma decisão consciente:
notificação é um extra, não algo essencial pro app funcionar.

## Dois bugs reais, encontrados testando esta aula

Vale documentar os dois, porque nenhum deles tem a ver com notificação
— apareceram *enquanto* testávamos o fluxo completo, e são o tipo de
coisa que só aparece testando de verdade, não só lendo o código.

### 1. Crash ao buscar certas cidades

Buscar "Manaus" (digitando aos poucos) derrubava o app com:

```
NullPointerException: Parameter specified as non-null is null: method City.<init>, parameter country
```

Investigando a API diretamente, achamos a causa: existem lugares que a
Open-Meteo devolve **sem o campo `country`** — por exemplo, "Mana" (um
território ultramarino francês) só tem `country_code`, sem `country`.
Nosso `GeocodingResultDto.country` estava declarado como `String` (não
nulo), mas o Gson não respeita anotações de nulidade do Kotlin — ele
simplesmente atribui `null` de qualquer jeito. O crash só acontecia
depois, quando esse valor era lido para montar um `City`, que aí sim
rejeita `null` em tempo de execução.

**Correção:** `country` virou `String?` no DTO (refletindo a API real),
e o mapper decide o que fazer com a ausência (`country ?: ""`). Essa é
uma lição geral sobre DTOs: eles devem espelhar o que a API **realmente
pode devolver**, não o que a documentação sugere que ela sempre devolve.

### 2. WorkManager periódico é difícil de forçar pra testar

Ao tentar confirmar a notificação manualmente, descobrimos que forçar
um trabalho periódico *cedo demais* (via `adb shell cmd jobscheduler
run -f`) faz o próprio WorkManager **adiar** a execução de propósito
("Delaying execution... because it is being executed before schedule"),
e cada tentativa forçada empurra o agendamento ainda mais pra frente —
o oposto do que se quer ao testar. Isso não é um bug do nosso código, é
uma proteção do WorkManager contra execuções fora de hora.

A forma que funcionou de verdade: usar `ExistingPeriodicWorkPolicy.REPLACE`
com um intervalo bem curto (os 15 minutos mínimos permitidos, em vez das
6 horas de produção) só durante o teste, confirmar que a notificação
aparece, e depois reverter para os valores de produção. É um lembrete de
que "forçar" nem sempre é o caminho mais rápido — às vezes o sistema
está literalmente dizendo "ainda não é hora".

## Critério de aceite

Testado no dispositivo físico, de ponta a ponta: favoritar uma cidade
com temperatura ≥ 30°C (Manaus, 30.2°C), o `SyncWorker` rodar, e a
notificação **"Manaus está quente — Temperatura atual: 30.2°C"**
aparecer de verdade na barra de notificações do tablet — confirmado
tanto pelo log do sistema (`NotificationManager: ... notify(...)`)
quanto visualmente, com a barra de notificações expandida.

## Fim das 10 aulas

Com isso, o app de clima está completo: busca de cidades, previsão
detalhada, favoritos com cache offline-first, sincronização periódica
em background, e notificações locais — construído aula a aula, com
arquitetura em camadas e injeção de dependência via Hilt desde o
início. Os bugs reais encontrados pelo caminho (e documentados em cada
aula) fazem parte do processo tanto quanto o código que funcionou de
primeira.
