# Aula 8 — Favoritos offline-first

## Objetivo

Chegou a hora de conectar o banco de dados local (Aula 7) ao resto do
app. Vamos implementar o botão de favoritar e fazer a tela de Favoritos
funcionar de verdade — inclusive sem internet.

## O problema: quem manda, o cache ou a rede?

Existe uma armadilha comum ao adicionar cache num app: tratar ele como
"plano B", só usado quando a rede falha. Isso parece razoável, mas gera
um comportamento estranho — toda vez que a internet está disponível, a
tela fica esperando a rede responder antes de mostrar qualquer coisa,
mesmo que já exista um dado (talvez levemente desatualizado) guardado
localmente.

Este projeto segue um padrão diferente, chamado **single source of
truth** ("uma única fonte de verdade"): a tela nunca lê a rede
diretamente. Ela sempre lê o banco local. A rede só serve pra
**atualizar** o banco local — e assim que atualiza, a tela reflete essa
mudança automaticamente, porque já está "escutando" o banco. Na prática,
isso significa: mostra o que já se sabe imediatamente, e atualiza assim
que uma informação mais nova chegar.

## O que vamos construir

- `getForecast` passa a consultar o cache primeiro, e a rede depois.
- Favoritar/desfavoritar uma cidade, persistido no Room.
- A tela de Favoritos, lendo do banco local — funciona mesmo sem
  internet.
- Um botão de favoritar na tela de Detalhes.

## Passo a passo

### Buscando a previsão: cache primeiro, rede depois

```kotlin
override fun getForecast(city: City): Flow<Result<WeatherForecast>> = flow {
    cityDao.getById(city.id)?.lastTemperature?.let { cachedTemp ->
        emit(Result.success(WeatherForecast(cachedTemp, 0, emptyList(), emptyList())))
    }
    try {
        val response = forecastApi.getForecast(city.latitude, city.longitude)
        val forecast = response.toDomain()
        cityDao.getById(city.id)?.let { existing ->
            cityDao.upsert(existing.copy(lastTemperature = forecast.currentTemperature))
        }
        emit(Result.success(forecast))
    } catch (e: IOException) {
        emit(Result.failure(e))
    } catch (e: HttpException) {
        emit(Result.failure(e))
    }
}
```

Repare que essa função pode `emit` (mandar um resultado pra quem está
observando) **duas vezes**: primeiro, se já existir uma temperatura
salva no banco pra aquela cidade, ela aparece imediatamente — sem
esperar a rede. Depois, quando a resposta da internet chega, um segundo
resultado é emitido com o dado atualizado, e o banco é atualizado junto.
Se a cidade não for favorita (não está no banco), simplesmente pula
direto pra rede, sem nada pra mostrar antes.

### Favoritar é só "existe ou não existe no banco"

```kotlin
override suspend fun toggleFavorite(city: City): Boolean {
    val existing = cityDao.getById(city.id)
    return if (existing != null) {
        cityDao.delete(city.id)
        false
    } else {
        cityDao.upsert(
            CityEntity(city.id, city.name, city.country, city.latitude, city.longitude, city.lastTemperature)
        )
        true
    }
}
```

Favoritar uma cidade é simplesmente inserir ela na tabela do Room;
desfavoritar é apagar. Não existe uma coluna "é favorita" separada — o
próprio fato de a cidade estar ou não na tabela `favorite_cities` já é
a resposta.

### A tela de Favoritos lendo direto do banco

```kotlin
val favorites: StateFlow<List<City>> = repository.getFavorites()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

Esse `ViewModel` não pede nada à rede — ele só observa a lista de
cidades favoritas guardada no Room. É exatamente por isso que a tela de
Favoritos funciona sem internet: ela nunca dependeu da internet pra
existir, só pra ficar atualizada.

## Um ajuste que não estava no plano original: navegar até os Favoritos

Ao testar esta aula, percebemos que não existia nenhum jeito de chegar
na tela de Favoritos pela interface — só tínhamos ligado Busca→Detalhes
na Aula 6. Adicionamos um botão simples "Favoritos" no topo da tela de
Busca pra resolver isso. Voltar de Detalhes ou Favoritos pra Busca já
funciona sozinho, através do botão de voltar do sistema — o Navigation
Compose cuida disso automaticamente.

## Um bug encontrado depois: favoritar salvava temperatura vazia

Testando esta aula junto com a Aula 9 (que passou a exibir a
temperatura na tela de Favoritos), apareceu um problema: favoritar uma
cidade recém-buscada salvava `lastTemperature = null` — mesmo a tela de
Detalhes já tendo acabado de mostrar a temperatura atual. O motivo:
`toggleFavorite` sempre criava a `CityEntity` com `lastTemperature =
null` "na mão", ignorando que a `DetailsScreen` já tinha esse dado em
memória (no `uiState`) no exato momento em que o usuário tocava em
"Favoritar".

A correção: o `DetailsViewModel.toggleFavorite()` passa a ler a
temperatura atual do `uiState` (se já tiver carregado) e leva ela junto
no `City` que envia pro Repository — que agora usa
`city.lastTemperature` em vez de forçar `null`. Assim, favoritar uma
cidade já salva o dado que a tela já tinha, sem precisar esperar o
próximo sync em background (Aula 9) ou uma nova visita à tela de
Detalhes.

## Critério de aceite

Testado no dispositivo físico: favoritar São Paulo, ativar modo avião,
e a tela de Favoritos continua mostrando a cidade normalmente — a rede
foi desligada, mas o app não perdeu nenhuma informação, porque nunca
dependeu da rede pra mostrar o que já sabia. Também testado: favoritar
uma cidade já mostra a temperatura na lista de Favoritos imediatamente,
sem esperar nenhum sync.
