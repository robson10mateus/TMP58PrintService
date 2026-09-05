# ESC/POS Print Service

Serviço de impressão para Android que disponibiliza impressoras térmicas Bluetooth ESC/POS no menu de impressão do sistema. O aplicativo localiza uma impressora já pareada, abre uma conexão Bluetooth Serial Port Profile (SPP) e permite configurar a largura de cada equipamento.

## Funcionalidades

- integração com a API `PrintService` do Android;
- descoberta da impressora térmica Bluetooth selecionada;
- comunicação Bluetooth clássica por RFCOMM/SPP;
- presets de 58 mm/384 pontos e 80 mm/576 pontos;
- perfil personalizado com largura física, largura imprimível, DPI e limiar monocromático;
- configuração de papel persistida separadamente para cada endereço Bluetooth;
- impressão monocromática e margens zero;
- solicitação da permissão `BLUETOOTH_CONNECT` no Android 12 ou superior;
- seleção persistente entre dispositivos Bluetooth já pareados;
- renderização de documentos PDF em bitmap monocromático com dithering;
- envio raster ESC/POS em blocos, com suporte a múltiplas páginas e cancelamento;
- impressão de teste em CP860 para caracteres do português;
- indicação de disponibilidade baseada no Bluetooth e no pareamento.

## Requisitos

- Android Studio e Android SDK 37;
- JDK 25, conforme o toolchain do Gradle incluído no projeto;
- dispositivo Android 6.0 (API 23) ou superior com Bluetooth;
- impressora térmica ESC/POS com Bluetooth clássico e perfil SPP.

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

1. Pareie a impressora nas configurações de Bluetooth do Android.
2. Instale e abra o aplicativo.
3. Autorize o acesso a dispositivos Bluetooth, quando solicitado.
4. Toque em **Selecionar impressora** e escolha o dispositivo pareado.
5. Selecione o preset de 58 mm, 80 mm ou configure o papel manualmente.
6. Toque em **Configurações de impressão** e ative o serviço.
7. Em um aplicativo compatível, escolha **Imprimir** e selecione a impressora configurada.

## Arquitetura

- `MainActivity.kt`: solicita a permissão e abre as configurações de impressão.
- `ThermalPrintService.kt`: recebe e controla os trabalhos de impressão.
- `ThermalPrinterDiscoverySession.kt`: registra a impressora no sistema.
- `BluetoothPrinter.kt`: procura dispositivos pareados e gerencia a conexão SPP.
- `PdfDocumentRenderer.kt`: renderiza páginas PDF na largura configurada.
- `EscPos.kt`: aplica dithering e monta os comandos raster ESC/POS.
- `PrinterPreferences.kt`: mantém a impressora e o perfil de papel de cada dispositivo.

O código-fonte está em `app/src/main/java/com/robson/tmp58printservice/`; recursos e metadados Android ficam em `app/src/main/res/`.

## Testes e qualidade

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
```

O segundo comando exige um dispositivo ou emulador conectado. Há testes unitários para o empacotamento raster e a codificação CP860, além de um teste instrumental para o fluxo PDF → ESC/POS. Alterações na comunicação Bluetooth também devem ser validadas com uma impressora física. O workflow em `.github/workflows/android.yml` executa testes e lint a cada push ou pull request.

## Limitações atuais

O status “disponível” confirma que o Bluetooth está ativo e que o dispositivo está pareado, mas não garante que a impressora esteja ligada ou com papel. O tamanho não pode ser detectado de forma universal por ESC/POS e deve ser configurado no aplicativo. O serviço anuncia uma página virtual de 200 mm de comprimento e remove o espaço branco final antes do envio. O projeto não oferece suporte a BLE, USB, Wi-Fi, ZPL ou protocolos proprietários e ainda não consulta papel, tampa, temperatura ou outros estados específicos do equipamento. PDFs muito longos são limitados a 12.000 pontos por página para controlar o uso de memória.

## Contribuição

Consulte [AGENTS.md](AGENTS.md) para conhecer a organização do projeto, convenções de código, testes e orientações para pull requests.
