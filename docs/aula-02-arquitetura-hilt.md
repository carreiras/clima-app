# Aula 2 — Arquitetura em camadas + Hilt

## Objetivo

Na Aula 1 preparamos a bancada: as ferramentas do Hilt entraram no
projeto, mas nenhuma classe do app realmente as usava ainda. É como
comprar as ferramentas mas não ter tirado nenhuma delas da caixa. Nesta
aula vamos finalmente ligar o Hilt: dar a ele um lugar pra começar a
trabalhar, e preparar os espaços onde as próximas aulas vão guardar as
peças de verdade (conexão de rede, banco de dados, repository).

## O que vamos construir

- Um ponto de partida para o Hilt dentro do app.
- A tela principal preparada para receber coisas prontas, montadas pelo
  Hilt.
- Três "gavetas" vazias — cada uma vai guardar, nas próximas aulas, a
  receita de como construir um tipo de peça do app (conexão de rede,
  banco de dados, repository).

De novo: nenhuma tela nova. O critério de aceite desta aula é simples —
o app continua abrindo normalmente, só que agora com o Hilt de fato
funcionando por baixo dos panos.

## Passo a passo

### 1. Dando ao Hilt um lugar para começar

Todo app Android tem, desde o primeiro instante em que é aberto, um
objeto que representa "o aplicativo inteiro" — ele existe antes de
qualquer tela, e continua existindo enquanto o app estiver na memória.
Normalmente esse objeto é genérico e você nem percebe que ele está lá.

O Hilt precisa se apoiar exatamente nesse objeto, porque é ali que ele
vai guardar as peças que devem existir uma única vez enquanto o app
estiver aberto — por exemplo, uma única conexão com o banco de dados,
não uma nova a cada tela que abre. Por isso, criamos a nossa própria
versão desse objeto:

```kotlin
package com.example.climaapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClimaApplication : Application()
```

Com isso, o Hilt passa a ter uma base sólida no app inteiro — um lugar
fixo onde ele organiza tudo o que vai poder ser reaproveitado por
qualquer tela. Só criar essa classe não é suficiente, porém: o Android
também precisa saber que deve *usar* essa versão nossa, em vez da
genérica padrão. Isso é dito no arquivo de configuração do app:

```xml
<application
    android:name=".ClimaApplication"
    ...
```

### 2. Preparando a tela principal para receber peças prontas

```kotlin
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
```

O Hilt não consegue entregar peças montadas pra qualquer tela do app —
cada tela que quiser participar precisa "avisar" que está disposta a
receber. Sem esse aviso na tela principal, quando formos pedir ao Hilt,
a partir da Aula 5, que entregue prontinho um objeto que já sabe buscar
dados de clima, isso simplesmente não funcionaria: o Hilt nem saberia
que aquela tela existe.

### 3. Criando as "gavetas" vazias

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule
```

(O mesmo padrão se repete em `DatabaseModule`, com um nome diferente.)

Essas gavetas ainda estão vazias — sozinhas, elas não fazem nada. Cada
uma delas vai, nas próximas aulas, guardar a receita de como montar um
tipo específico de peça: `NetworkModule` vai aprender a montar a conexão
com a internet (Aula 4), `DatabaseModule` vai aprender a montar o banco
de dados local (Aula 7). Criá-las já agora, vazias, é só reservar o
espaço onde essas receitas vão morar.

Existe uma terceira gaveta, ligeiramente diferente:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule
```

Ela também está vazia por enquanto, mas nasce de um jeito um pouco
diferente das outras duas — é uma preparação pro tipo de receita que a
Aula 5 vai guardar nela (uma forma mais curta de dizer "quando pedirem
essa interface, entregue essa implementação"), que só funciona nesse
formato específico de gaveta.

## Critério de aceite

O app compila, instala e abre sem travar — continua mostrando
"Hello Android!" na tela, e está tudo bem que seja assim. O que essa aula
confirma é que a fundação do Hilt não quebrou nada, mesmo sem nenhuma
lógica de negócio rodando ainda. As próximas aulas vão começar a
preencher essas gavetas de verdade.
