# SoundFont Player para Android

Aplicativo Android simples para carregar e tocar arquivos SoundFont SF2 e SF3 usando o motor nativo FluidSynth.

## Funções atuais

- Abrir arquivos SF2/SF3 pelo seletor de arquivos do Android.
- Copiar o arquivo escolhido para o armazenamento privado do aplicativo antes de carregá-lo.
- Ler e listar os bancos e presets/instrumentos existentes no SoundFont.
- Selecionar um instrumento pelo nome, banco e número de programa.
- Tocar notas em um teclado virtual de duas oitavas, com suporte a acordes por multitoque.
- Ajustar oitava, volume e reverberação.
- Interromper todas as notas com um comando.

O aplicativo não exige conexão MIDI externa. A reprodução é feita pelo FluidSynth e pela saída de áudio do próprio Android.

## Compilação

O projeto usa Gradle, Android Gradle Plugin, CMake e NDK.

Para gerar o APK de depuração, execute gradle assembleDebug em um ambiente com Android SDK, NDK e Gradle configurados.

O fluxo automatizado está em [GitHub Actions](.github/workflows/build-apk.yml). O APK de depuração é publicado como artefato da execução.

## Arquitetura

- app/src/main/java: interface do player, teclado virtual e integração Kotlin/JNI.
- app/src/main/cpp: integração C++ com FluidSynth.
- app/src/main/cpp/fluidsynth: cabeçalhos e bibliotecas nativas pré-compiladas.

As bibliotecas nativas atualmente incluídas abrangem arm64-v8a, armeabi-v7a, x86 e x86_64.

## Licenças e créditos

O código deste projeto é distribuído sob a licença MIT, conforme o arquivo LICENSE.

O FluidSynth e suas bibliotecas de apoio mantêm suas próprias licenças. Consulte os arquivos de licença distribuídos em app/src/main/cpp/fluidsynth.

Este projeto deriva do trabalho original [Android MIDI Synth de Robson Martins](https://github.com/robsonsmartins/android-midi-synth) e do artigo [Criando um App Sintetizador MIDI para Android com Kotlin e C++](https://embarcados.com.br/criando-um-app-sintetizador-midi-para-android-com-kotlin-e-c/).