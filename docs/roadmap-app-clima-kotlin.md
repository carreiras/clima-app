# Roadmap técnico — App de Previsão do Tempo (Kotlin Android)

**Contexto:** projeto de 5 aulas para turma intermediário/avançado. Já cobriram consumo de API simples (crypto monitor) e persistência local básica (to-do list + SQLite). Este projeto adiciona arquitetura em camadas, cache offline-first, DI, background work e testes.

**API:** [Open-Meteo](https://open-meteo.com/en/docs) — gratuita, sem API key, sem cadastro (uso não-comercial/educacional).
- Geocoding (nome da cidade → lat/lon): `https://geocoding-api.open-meteo.com/v1/search?name={cidade}`
- Previsão: `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,weather_code&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min&timezone=auto`

**Stack alvo:** Kotlin, Jetpack Compose + Navigation Compose, Hilt (DI), Retrofit + kotlinx.serialization, Room, Coroutines/Flow, WorkManager, JUnit + Turbine + MockK + coroutines-test.

**Estrutura de pacotes:**
```
com.fiap.weatherapp
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
│   └── theme/
└── work/            (SyncWorker)
```

---

## Aula 1 — Arquitetura base

**Objetivo:** sair do "tudo na Activity", estabelecer camadas e DI antes de qualquer lógica de negócio.

Tarefas:
1. Criar projeto Compose, adicionar dependências: Hilt, Navigation Compose, Retrofit, Room, kotlinx-coroutines.
2. Criar a estrutura de pacotes acima (mesmo vazia).
3. `Application` class anotada com `@HiltAndroidApp`, módulos Hilt vazios (`NetworkModule`, `DatabaseModule`, `RepositoryModule`).
4. Navigation Compose com 3 rotas: `Search`, `Details(cityId)`, `Favorites` — telas placeholder.

**Critério de aceite:** app compila, navega entre as 3 telas vazias, um ViewModel injetado via Hilt (sem lógica) resolve sem erro.

**Prompt sugerido para o Claude Code:**
> Crie um projeto Android Kotlin com Jetpack Compose, Hilt, Navigation Compose, Retrofit e Room configurados (build.gradle.kts + Application class + módulos Hilt vazios). Estrutura de pacotes: [colar estrutura acima]. Crie 3 telas placeholder navegáveis: Search, Details, Favorites. Não implemente lógica de negócio ainda — só o esqueleto.

---

## Aula 2 — Consumo de API com Coroutines/Flow

**Objetivo:** Retrofit + Flow + tratamento de estado de tela real (loading/success/error), não só "deu certo".

Tarefas:
1. `WeatherApiService` (Retrofit) com os dois endpoints do Open-Meteo (geocoding + forecast).
2. DTOs para as respostas (geocoding e forecast) + mappers para os domain models (`City`, `WeatherForecast`).
3. `WeatherRepository.searchCity(query: String): Flow<Result<List<City>>>` e `getForecast(city: City): Flow<Result<WeatherForecast>>`.
4. `SearchViewModel` com estado via sealed class (`Idle`, `Loading`, `Success`, `Error`) exposto como `StateFlow`.
5. UI: campo de busca + lista de resultados + card de previsão atual.
6. Tratamento explícito de erro de rede (sem internet, timeout) — não deixar a exceção estourar.

**Critério de aceite:** buscar "São Paulo" retorna a temperatura atual; desligar o Wi-Fi mostra um estado de erro tratado, não crash.

**Prompt sugerido para o Claude Code:**
> Implemente o consumo da API Open-Meteo (geocoding + forecast) via Retrofit, com DTOs, mappers para domain models e Repository expondo Flow<Result<T>>. Crie o SearchViewModel com estado sealed class (Idle/Loading/Success/Error) e a UI em Compose para busca e exibição da previsão atual. Trate erros de rede explicitamente, sem deixar exceção não tratada.

---

## Aula 3 — Room como cache (offline-first)

**Objetivo:** Room não é "o banco do app" — é uma camada de cache que o Repository orquestra. Esse é o ponto pedagógico central da aula, diferente do to-do list que já deram.

Tarefas:
1. Entities Room (`CityEntity`, `ForecastEntity`) + DAO + `AppDatabase`.
2. Repository decide fonte: retorna cache imediatamente (se existir) via Flow, dispara atualização da rede em paralelo, atualiza o cache quando a resposta chega (padrão single source of truth).
3. Funcionalidade de favoritar cidade (persistida no Room).
4. Tela de Favoritos lendo do Room, funcionando sem internet.

**Critério de aceite:** favoritar uma cidade, ativar modo avião, reabrir o app — os dados da última sincronização ainda aparecem.

**Prompt sugerido para o Claude Code:**
> Adicione Room como camada de cache seguindo o padrão single source of truth: o Repository deve emitir dados do cache local imediatamente via Flow e atualizar em background quando a rede responder. Implemente favoritar/desfavoritar cidade persistido no Room, e a tela de Favoritos deve funcionar sem conexão de rede usando os últimos dados salvos.

---

## Aula 4 — Background sync e notificações

**Objetivo:** WorkManager de verdade (constraints, agendamento periódico) e notificações — tema que costuma surpreender em entrevista técnica.

Tarefas:
1. `SyncWorker` (CoroutineWorker) que atualiza a previsão de todas as cidades favoritas.
2. Agendamento periódico via `WorkManager` com constraints (ex: `NetworkType.CONNECTED`).
3. Notificação local quando a temperatura de uma cidade favorita cruza um limiar configurável.
4. Canal de notificação criado corretamente (Android 8+) e tratamento de permissão de notificação (Android 13+).

**Critério de aceite:** disparar o worker manualmente (via `WorkManager.getInstance().enqueue` de teste ou `adb shell cmd jobscheduler`) atualiza os favoritos e, se o limiar for cruzado, a notificação aparece.

**Prompt sugerido para o Claude Code:**
> Implemente um CoroutineWorker que sincroniza a previsão das cidades favoritas em background, agendado via WorkManager com constraint de rede conectada. Adicione notificação local quando a temperatura de uma cidade favorita ultrapassar um limiar configurável, incluindo canal de notificação (Android 8+) e solicitação de permissão de notificação (Android 13+).

---

## Aula 5 — Testes e fechamento

**Objetivo:** garantir que o projeto não é só "funciona na minha máquina" — testável e resiliente a rotação/process death.

Tarefas:
1. Testes unitários do Repository (fake da API + Room em memória ou fake DAO).
2. Testes do ViewModel com `runTest` + Turbine para os estados do Flow.
3. Preservar estado de busca/tela em rotação e process death via `SavedStateHandle`.
4. (Opcional, se sobrar tempo) workflow simples de CI (GitHub Actions) rodando `./gradlew test`.

**Critério de aceite:** suíte de testes roda e passa; girar o dispositivo durante uma busca não perde o resultado exibido.

**Prompt sugerido para o Claude Code:**
> Escreva testes unitários para o WeatherRepository (com fakes para API e Room) e para o SearchViewModel (usando runTest e Turbine para testar os estados emitidos). Adicione SavedStateHandle no ViewModel para preservar o estado de busca em rotação e process death.

---

## Observações para uso com o Claude Code

- Peça para gerar **só o esqueleto** em cada aula, não a lógica completa de uma vez — o objetivo é o aluno completar as partes centrais de cada tópico (Flow, cache, WorkManager, testes) em aula.
- Se quiser uma versão "gabarito" completa antes de dar a aula, rode todos os prompts em sequência num branch separado e use como referência ao corrigir os alunos.
- Ajuste a estrutura de pacotes ou nomes conforme a convenção que já usa nas outras aulas, para manter consistência com o que os alunos já viram no crypto monitor e no to-do list.
