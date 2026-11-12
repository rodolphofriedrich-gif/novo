# Android MIDI Synth

Aplicativo Android que recebe eventos MIDI por **USB** e **Bluetooth LE (BLE)** e os reproduz pelo motor **FluidSynth**, usando Kotlin, C++ e JNI.

## Recursos

- Entrada MIDI USB e Bluetooth LE.
- Reprodução de notas, Note Off e Control Change.
- Suporte a canais MIDI, troca de programa e pitch bend.
- SoundFont padrão Kawai Stereo Grand em formato SF3.
- Síntese nativa com FluidSynth e saída de áudio Oboe em baixa latência.

## Compilação

O projeto usa Gradle, Android Gradle Plugin e CMake/NDK.

Para gerar o APK de depuração em um ambiente com Android SDK, NDK e Gradle configurados:

```bash
gradle assembleDebug
```

O fluxo automatizado está em [GitHub Actions](.github/workflows/build-apk.yml). O APK de depuração é publicado como artefato da execução.

## Arquitetura

- `app/src/main/java`: atividade, gerenciamento MIDI USB/BLE e interface JNI do sintetizador.
- `app/src/main/cpp`: integração C++ com FluidSynth.
- `app/src/main/cpp/fluidsynth`: cabeçalhos e bibliotecas nativas pré-compiladas.
- `app/src/main/assets`: SoundFont padrão.

As bibliotecas nativas atualmente incluídas abrangem `arm64-v8a`, `armeabi-v7a`, `x86` e `x86_64`.

## Licenças e créditos

O código deste projeto é distribuído sob a licença MIT, conforme o arquivo [LICENSE](LICENSE).

O projeto utiliza componentes de terceiros que mantêm suas próprias licenças. Consulte os arquivos de licença distribuídos junto a cada componente, especialmente:

- FluidSynth: licença LGPL 2.1.
- Kawai Stereo Grand SoundFont: CC0 1.0 Universal.
- Drivers MIDI USB e BLE: licenças dos respectivos projetos upstream.

Este aplicativo deriva do trabalho original [Android MIDI Synth de Robson Martins](https://github.com/robsonsmartins/android-midi-synth) e do artigo [Criando um App Sintetizador MIDI para Android com Kotlin e C++](https://embarcados.com.br/criando-um-app-sintetizador-midi-para-android-com-kotlin-e-c/).
