# Aula 5 — Repository + Flow + ViewModel

## Objetivo

Temos os serviços de rede prontos desde a Aula 4, mas nenhuma tela pode
usá-los diretamente — chamar a internet direto de dentro de uma tela
traria vários problemas de uma vez. Nesta aula vamos criar as duas
camadas que ficam entre "buscar dados na internet" e "mostrar dados na
tela": o **Repository** e o **ViewModel**.

## O problema: uma tela não deveria falar direto com a internet

Imagine que a tela de busca chamasse o serviço de rede diretamente. Ela
precisaria lidar, sozinha, com tudo que pode dar errado: sem internet,
API fora do ar, resposta demorando. E se amanhã quiséssemos adicionar um
cache local (Aula 7), teríamos que abrir a tela de novo e misturar mais
essa lógica ali dentro. Toda vez que a regra de "de onde vêm os dados"
muda, a tela também precisaria mudar — mesmo que ela só quisesse mostrar
uma lista de cidades na tela.

O **Repository** existe pra isolar essa responsabilidade: é o único
lugar do app que sabe de onde os dados vêm (rede, cache, ambos). A tela
não pergunta "chama a API pra mim", ela pergunta "me dá a lista de
cidades" — e não importa, do ponto de vista da tela, como essa resposta
foi obtida por trás.

Só isso ainda não é suficiente, porque telas em Compose são recriadas
com frequência (por exemplo, ao girar a tela), e não podem guardar
estado por conta própria de forma confiável. É aí que entra o
**ViewModel**: ele guarda o estado atual da tela (o que já foi buscado,
se está carregando, se deu erro) de um jeito que sobrevive a essas
recriações, e é o meio-campo entre a tela e o Repository.

## O que vamos construir

- `WeatherRepository` — o "contrato" de o que o repository sabe fazer.
- `WeatherRepositoryImpl` — a implementação de verdade, usando os
  serviços de rede da Aula 4.
- `SearchUiState` — todas as situações possíveis em que a tela de busca
  pode estar.
- `SearchViewModel` — conecta o Repository ao estado da tela.

Ainda sem tela real usando isso — a Aula 6 vai ligar tudo numa interface
que você consegue tocar.

## Passo a passo

### Descrevendo os possíveis estados de uma tela

Uma tela de busca não está só "com resultado" ou "sem resultado" — ela
passa por várias situações diferentes: ainda não buscou nada, está
buscando, encontrou resultado, ou deu erro. Em vez de tentar controlar
isso com variáveis soltas (um booleano de carregando, outro de erro,
uma lista que pode estar vazia por dois motivos diferentes...),
descrevemos essas situações de forma explícita:

```kotlin
sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val cities: List<City>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
```

Isso garante que a tela só pode estar em **um** desses quatro estados
por vez, e o compilador nos obriga a tratar todos eles quando formos
desenhar a tela (Aula 6) — não tem como esquecer de tratar o caso de
erro, por exemplo.

### O Repository indo buscar dados, sem deixar erro estourar

```kotlin
override fun searchCity(query: String): Flow<Result<List<City>>> = flow {
    try {
        val response = geocodingApi.searchCity(query)
        val cities = response.results.orEmpty().map { it.toDomain() }
        emit(Result.success(cities))
    } catch (e: IOException) {
        emit(Result.failure(e))
    } catch (e: HttpException) {
        emit(Result.failure(e))
    }
}
```

Essa função nunca deixa uma falha de rede virar uma exceção não tratada
que derruba o app — toda falha possível (sem internet, erro do
servidor) é capturada e devolvida como um resultado normal, só que
marcado como falha. Quem chama essa função sempre recebe uma resposta,
nunca um crash.

### O ViewModel conectando o Repository ao estado da tela

```kotlin
fun search(query: String) {
    if (query.isBlank()) {
        _uiState.value = SearchUiState.Idle
        return
    }
    viewModelScope.launch {
        _uiState.value = SearchUiState.Loading
        repository.searchCity(query).collect { result ->
            _uiState.value = result.fold(
                onSuccess = { SearchUiState.Success(it) },
                onFailure = { SearchUiState.Error(it.message ?: "Erro ao buscar cidade") }
            )
        }
    }
}
```

Essa função faz a ponte: assim que alguém digita algo, ela marca o
estado como "carregando", pede ao Repository os dados, e — assim que a
resposta chega — traduz o resultado (sucesso ou falha) pro estado
correspondente que a tela vai entender. A tela nunca fala com o
Repository diretamente; ela só observa esse estado.

### Ligando o Repository ao seu contrato

```kotlin
@Binds
@Singleton
abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository
```

Essa linha completa a gaveta `RepositoryModule`, criada vazia na
Aula 2: agora, sempre que alguma classe pedir ao Hilt um
`WeatherRepository`, ela recebe uma instância de `WeatherRepositoryImpl`
pronta, com os dois serviços de rede já injetados dentro. É graças a
essa ligação que o `SearchViewModel` (acima) consegue simplesmente pedir
um `WeatherRepository` no seu construtor, sem se preocupar em montar
ele manualmente.

## Critério de aceite

`./gradlew.bat assembleDebug` termina com sucesso. Isso confirma que
Repository e ViewModel estão corretos e se encaixam no restante do
app — o teste visual de verdade (buscar uma cidade e ver o resultado na
tela) só é possível a partir da Aula 6, quando existir uma interface
real usando esse ViewModel.
