# TMP58 Print Service

Serviço de impressão para Android que disponibiliza a impressora térmica Bluetooth **IMP-TMP58ABT** no menu de impressão do sistema. O aplicativo localiza uma impressora já pareada, abre uma conexão Bluetooth Serial Port Profile (SPP) e envia comandos ESC/POS.

## Funcionalidades

- integração com a API `PrintService` do Android;
- descoberta da IMP-TMP58ABT como impressora térmica de 58 mm;
- comunicação Bluetooth clássica por RFCOMM/SPP;
- suporte anunciado a 203 DPI, impressão monocromática e margens zero;
- solicitação da permissão `BLUETOOTH_CONNECT` no Android 12 ou superior;
- geração de uma impressão de teste em ESC/POS.

## Requisitos

- Android Studio e Android SDK 37;
- JDK 25, conforme o toolchain do Gradle incluído no projeto;
- dispositivo Android 6.0 (API 23) ou superior com Bluetooth;
- impressora pareada cujo nome contenha `IMP-TMP58ABT`.

## Compilação

Clone o repositório, abra-o no Android Studio ou utilize o Gradle Wrapper:

```bash
./gradlew assembleDebug
```

O APK de desenvolvimento será gerado em `app/build/outputs/apk/debug/`. Para instalar em um dispositivo conectado:

```bash
./gradlew installDebug
```

## Como usar

1. Pareie a IMP-TMP58ABT nas configurações de Bluetooth do Android.
2. Instale e abra o aplicativo.
3. Autorize o acesso a dispositivos Bluetooth, quando solicitado.
4. Toque em **CONFIGURAÇÕES DE IMPRESSÃO**.
5. Ative o serviço **TMP58 Print Service**.
6. Em um aplicativo compatível, escolha **Imprimir** e selecione `IMP-TMP58ABT`.

## Arquitetura

- `MainActivity.kt`: solicita a permissão e abre as configurações de impressão.
- `ThermalPrintService.kt`: recebe e controla os trabalhos de impressão.
- `ThermalPrinterDiscoverySession.kt`: registra a impressora no sistema.
- `BluetoothPrinter.kt`: procura dispositivos pareados e gerencia a conexão SPP.
- `EscPos.kt`: monta os bytes enviados à impressora.

O código-fonte está em `app/src/main/java/com/robson/tmp58printservice/`; recursos e metadados Android ficam em `app/src/main/res/`.

## Testes e qualidade

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
```

O segundo comando exige um dispositivo ou emulador conectado. Alterações na comunicação Bluetooth também devem ser validadas com uma impressora física.

## Limitações atuais

A versão atual não interpreta o documento fornecido pelo trabalho de impressão. Ao receber um trabalho, ela envia o conteúdo fixo produzido por `EscPos.teste()`. O status anunciado na descoberta também não confirma previamente se a impressora está ligada ou conectável. Conversão de PDF/imagem para bitmap ESC/POS, seleção de outros modelos e acompanhamento de status ainda não estão implementados.

## Contribuição

Consulte [AGENTS.md](AGENTS.md) para conhecer a organização do projeto, convenções de código, testes e orientações para pull requests.
