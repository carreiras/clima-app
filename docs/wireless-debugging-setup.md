# Configuração de Depuração sem Fio (Wireless Debugging)

Procedimento para conectar um dispositivo Android ao Android Studio via Wi-Fi,
usado quando o cabo USB não transmite dados (só carrega) ou não há cabo de
dados disponível.

Testado com: Samsung Galaxy Tab S7 FE (SM-P620), Android 11+.

## Pré-requisitos

- PC e dispositivo Android conectados na **mesma rede Wi-Fi**.
- Android 11 ou superior no dispositivo (pareamento por código nativo).
- Android SDK Platform Tools instalado (contém o `adb`).
  - Caminho local usado neste projeto:
    `C:\Users\ewert\AppData\Local\Android\Sdk\platform-tools\adb.exe`

## Passo a passo

### 1. Ativar Opções do desenvolvedor (se ainda não estiver ativo)

1. No dispositivo: **Configurações > Sobre o tablet/telefone**.
2. Toque 7 vezes em **"Número da versão"** (ou "Build number").
3. Volte para Configurações; a opção **"Opções do desenvolvedor"** vai
   aparecer (geralmente em Sistema > Opções do desenvolvedor).

### 2. Ativar Depuração sem fio

1. Vá em **Configurações > Sistema > Opções do desenvolvedor**.
2. Ative **"Depuração sem fio" (Wireless debugging)**.

### 3. Parear o dispositivo com código

1. Na tela de Depuração sem fio, toque em **"Parear dispositivo com
   código de pareamento"**.
2. Vai aparecer:
   - Um **código de 6 dígitos**.
   - Um **IP:porta de pareamento** (ex: `192.168.15.6:45089`).
3. No PC, rode:

   ```bash
   adb pair <IP>:<PORTA_PAREAMENTO> <CODIGO_6_DIGITOS>
   ```

   Exemplo:

   ```bash
   adb pair 192.168.15.6:45089 101763
   ```

4. Deve retornar `Successfully paired to ...`.

### 4. Conectar ao dispositivo

1. Volte para a **tela principal** de Depuração sem fio (não a de
   pareamento). Logo abaixo do nome do dispositivo aparece um
   **IP:porta de conexão** (porta diferente da usada no pareamento, ex:
   `192.168.15.6:46307`).
2. No PC, rode:

   ```bash
   adb connect <IP>:<PORTA_CONEXAO>
   ```

3. Confirme com:

   ```bash
   adb devices -l
   ```

   O dispositivo deve aparecer com status `device` (não `unauthorized`).

> **Observação:** a porta de conexão muda a cada nova sessão/reconexão.
> Sempre confira o valor atual na tela do dispositivo antes de rodar
> `adb connect`.

### 5. Dispositivo duplicado na lista do Android Studio

É normal o Android Studio mostrar o **mesmo dispositivo duas vezes** em
"Running devices": uma entrada via auto-descoberta mDNS
(`adb-<serial>._adb-tls-connect._tcp`) e outra pela conexão manual feita
com `adb connect` (`IP:porta`). Ambas apontam para o mesmo hardware.

Para remover a duplicata manual e manter só a auto-descoberta:

```bash
adb disconnect <IP>:<PORTA_CONEXAO>
```

## Problema conhecido: Samsung "Bloqueio automático" (Auto Blocker)

Em aparelhos Samsung (One UI 6+), o recurso de segurança **"Bloqueio
automático" (Auto Blocker)** pode bloquear comandos de depuração via
USB e Wi-Fi, mesmo com a Depuração sem fio ativada e pareada. Sintoma:
a conexão simplesmente para de funcionar / cai sozinha.

**Como resolver:**

1. Vá em **Configurações > Segurança e privacidade > Bloqueio
   automático (Auto Blocker)**.
2. Desative a opção de **bloqueio de comandos via USB e sem fio**
   (o nome exato pode variar por versão do One UI).
3. Não é necessário desligar o Auto Blocker inteiro — só esse
   sub-item, se a interface permitir.
4. Depois de desativar, repita o passo 4 (conectar) com o IP:porta
   atual da tela de Depuração sem fio.

## Diagnóstico rápido (troubleshooting)

| Sintoma | Causa provável |
|---|---|
| `adb devices` retorna lista vazia, sem "unauthorized" | Depuração USB desativada, ou cabo/porta sem linhas de dados |
| Nenhuma notificação de modo USB ao conectar o cabo | Cabo é "carregamento apenas" (sem pinos de dados) ou porta USB com problema |
| Dispositivo aparece duas vezes no Android Studio | Normal — auto-descoberta mDNS + conexão manual via `adb connect` apontando para o mesmo aparelho |
| Conexão Wi-Fi cai sozinha depois de funcionar | Samsung Auto Blocker bloqueando comandos — ver seção acima |
| `adb pair`/`adb connect` falha | Confirmar que PC e dispositivo estão na mesma rede Wi-Fi e que IP/porta foram lidos corretamente (porta tem no máximo 5 dígitos, até 65535) |

## Comandos úteis

```bash
# Localização do adb usada neste projeto
ADB="/c/Users/ewert/AppData/Local/Android/Sdk/platform-tools/adb.exe"

# Reiniciar o servidor adb
"$ADB" kill-server
"$ADB" start-server

# Listar dispositivos conectados
"$ADB" devices -l

# Build + instalar + rodar o app (debug)
cd /c/projetos/fiap/clima-app
./gradlew.bat assembleDebug
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n com.example.climaapp/.MainActivity

# Parar o app
"$ADB" shell am force-stop com.example.climaapp
```
