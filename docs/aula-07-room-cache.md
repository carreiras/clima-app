# Aula 7 — Room como cache

## Objetivo

O app já busca dados na internet e mostra na tela — mas se a internet
cair, ou o usuário fechar e reabrir o app, tudo que foi buscado
desaparece. Nesta aula vamos guardar uma cópia local dos dados, usando o
Room (um banco de dados que mora dentro do próprio celular).

Um aviso importante antes de começar: nesta aula o banco ainda não está
**conectado** a nada — é só a fundação. A conexão de verdade (o
Repository decidindo usar o cache) é o assunto da Aula 8.

## O problema: nem tudo deveria depender da internet estar disponível

Pensa no cenário: o usuário favoritou "São Paulo" ontem, com Wi-Fi. Hoje
ele abre o app no metrô, sem sinal. Isso deveria realmente significar
"nenhum dado disponível"? Não faz sentido — a última temperatura que
sabíamos ainda é uma informação útil, mesmo que não seja a mais atual
possível.

Esse é o problema que um banco de dados local resolve: uma cópia dos
dados que mora no próprio aparelho, sobrevive ao app fechar, e não
depende de rede pra ser lida. O **Room** é a ferramenta que o Android
oferece pra isso — ele guarda dados em tabelas (como uma planilha), e
cuida de todo o trabalho chato de ler/escrever nessas tabelas, deixando
você trabalhar só com classes Kotlin normais.

## O que vamos construir

- `CityEntity` — como uma cidade favoritada fica guardada no banco.
- `CityDao` — as operações que podemos fazer nesse banco (ler todas,
  ler uma específica, salvar, apagar).
- `AppDatabase` — o banco de dados em si, juntando tudo.

Ninguém usa esse banco ainda — nem o Repository, nem nenhuma tela. Essa
ligação é o assunto da próxima aula.

## Passo a passo

### O formato de uma linha no banco

```kotlin
@Entity(tableName = "favorite_cities")
data class CityEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val lastTemperature: Double?
)
```

Essa classe descreve uma tabela chamada `favorite_cities`, onde cada
linha é uma cidade favoritada. Repare que ela é bem parecida com o
domain model `City` (Aula 4) — mas não é a mesma coisa: ela tem um campo
a mais, `lastTemperature`, que guarda a última temperatura que
conseguimos buscar daquela cidade. É justamente esse campo extra que vai
permitir mostrar alguma informação mesmo sem internet — o dado "velho"
fica guardado ali, esperando.

### O que podemos fazer com essa tabela

```kotlin
@Dao
interface CityDao {
    @Query("SELECT * FROM favorite_cities")
    fun getAll(): Flow<List<CityEntity>>

    @Query("SELECT * FROM favorite_cities WHERE id = :id")
    suspend fun getById(id: Long): CityEntity?

    @Upsert
    suspend fun upsert(city: CityEntity)

    @Query("DELETE FROM favorite_cities WHERE id = :id")
    suspend fun delete(id: Long)
}
```

Essa interface descreve as quatro operações que vamos precisar: listar
todas as cidades favoritadas, buscar uma específica, salvar (nova ou
atualização) e apagar. Assim como os serviços Retrofit da Aula 4, essa é
só uma descrição — o Room lê essa interface e gera, por baixo dos panos,
o código de verdade que conversa com o banco.

Vale explicar duas palavras que aparecem aqui: `Upsert` é a junção de
"update" e "insert" — salva um registro novo se ele não existir ainda,
ou atualiza se já existir, sem você ter que verificar qual dos dois
casos é. E repare que `getAll()` devolve um `Flow`, não uma lista
comum — isso significa que, se os dados no banco mudarem, quem estiver
"escutando" esse `Flow` recebe a lista atualizada automaticamente, sem
precisar pedir de novo.

### O banco de dados, juntando tudo

```kotlin
@Database(entities = [CityEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cityDao(): CityDao
}
```

Essa classe representa o banco como um todo — que tabelas ele tem
(`entities`), e como chegar até elas (`cityDao()`). É essa classe que o
`DatabaseModule` do Hilt vai usar pra criar o banco de verdade, uma
única vez pro app inteiro, e entregar pra quem precisar dele:

```kotlin
@Provides
@Singleton
fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "clima-app.db").build()

@Provides
@Singleton
fun provideCityDao(database: AppDatabase): CityDao = database.cityDao()
```

> **`Room.databaseBuilder(context, AppDatabase::class.java, "clima-app.db")`**
> → monta o banco de verdade a partir da classe `AppDatabase`, salvando
> num arquivo chamado `clima-app.db` dentro do armazenamento privado do
> app (o mesmo padrão builder do `Retrofit.Builder()` da Aula 1 —
> configura peça por peça, e `.build()` finaliza).
>
> O segundo `@Provides` (`provideCityDao`) existe porque, embora o
> `AppDatabase` já saiba entregar o DAO (`database.cityDao()`), quem for
> pedir ao Hilt só um `CityDao` (como o `WeatherRepositoryImpl` vai
> fazer, na Aula 8) não precisa saber que ele vem de dentro de um banco —
> só precisa pedir `CityDao` no construtor, e o Hilt resolve o resto.

## Critério de aceite

`./gradlew.bat assembleDebug` termina com sucesso. Isso confirma que a
estrutura do banco está correta e o Hilt sabe construir ele — o teste de
verdade (dados sobrevivendo a um fechamento do app, ou a um modo avião)
só é possível a partir da Aula 8, quando o Repository passar a usar esse
banco de verdade.
