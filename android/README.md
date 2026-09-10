# PhoneWheel - Aplicativo Android

Aplicativo Android em Kotlin responsável por capturar dados do giroscópio e enviá-los via UDP para o servidor Windows.

## 📋 Requisitos

- **Java Development Kit (JDK)**: 11 ou superior
- **Android SDK**: Instalado via Android Studio ou gerenciador de pacotes
- **Gradle**: 8.4+ (incluído no projeto via wrapper)
- **Kotlin**: 1.9.22+ (configurado no Gradle)

## 🏗️ Estrutura do Projeto

```
android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml      # Configuração do aplicativo
│   │       ├── java/com/phonewheel/
│   │       │   └── MainActivity.kt      # Atividade principal
│   │       └── res/
│   │           ├── layout/
│   │           │   └── activity_main.xml # Layout principal
│   │           └── values/
│   │               ├── colors.xml
│   │               ├── strings.xml
│   │               └── styles.xml
│   ├── build.gradle.kts                # Configuração do módulo app
│   └── proguard-rules.pro              # Regras ProGuard
├── gradle/
│   └── wrapper/                        # Gradle wrapper (auto-download)
├── build.gradle.kts                    # Configuração raiz
├── settings.gradle.kts                 # Configuração de módulos
├── gradle.properties                   # Propriedades do Gradle
├── gradlew                             # Script do Gradle (Unix)
└── gradlew.bat                         # Script do Gradle (Windows)
```

## 🚀 Compilação

### Instalação de Dependências

1. **Instalar Java JDK 11+**
   - [Oracle JDK](https://www.oracle.com/java/technologies/downloads/#java11)
   - [OpenJDK](https://adoptopenjdk.net/)

2. **Instalar Android SDK**
   - Via [Android Studio](https://developer.android.com/studio)
   - Ou usar `sdkmanager` se já tiver o SDK instalado

### Compilar o Projeto

```bash
# No Windows
cd android
gradlew.bat build

# No Linux/Mac
cd android
./gradlew build
```

### Compilar APK de Debug

```bash
# No Windows
gradlew.bat assembleDebug

# No Linux/Mac
./gradlew assembleDebug
```

O APK estará em `app/build/outputs/apk/debug/app-debug.apk`

### Compilar com Release

```bash
# No Windows
gradlew.bat assembleRelease

# No Linux/Mac
./gradlew assembleRelease
```

## 🛠️ Desenvolvimento

### Usando VS Code

1. Instalar extensões:
   - Android Development Tools
   - Kotlin Language
   - Gradle for Java

2. Abrir a pasta `android/` como workspace

3. Gradle deverá carregar automaticamente

### Usando Android Studio

1. Abrir Android Studio
2. File → Open → Selecionar pasta `android/`
3. Aguardar sincronização do Gradle

## 📦 Dependências

Dependências mínimas configuradas:

- **AndroidX Core**: `androidx.core:core-ktx:1.12.0`
- **AndroidX AppCompat**: `androidx.appcompat:appcompat:1.6.1`
- **AndroidX ConstraintLayout**: `androidx.constraintlayout:constraintlayout:2.1.4`
- **Kotlin StdLib**: `org.jetbrains.kotlin:kotlin-stdlib:1.9.22`

## 🎯 Próximos Passos

- [ ] Implementar leitura do giroscópio
- [ ] Implementar envio de dados via UDP
- [ ] Processar dados do sensor
- [ ] Criar UI para calibração
- [ ] Adicionar permissões necessárias

## 📝 Notas

- O projeto está configurado para compilar com `viewBinding` ativado
- Suporte a Android API 24+ (Android 7.0+)
- Target SDK: 34
- Kotlin como linguagem principal

## 🐛 Troubleshooting

### Erro: "JAVA_HOME is not set"
Defina a variável de ambiente JAVA_HOME:

```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-11

# Linux/Mac
export JAVA_HOME=/usr/libexec/java_home
```

### Erro: "Android SDK not found"
Instale o Android SDK via Android Studio ou defina `ANDROID_HOME`:

```bash
# Windows
set ANDROID_HOME=C:\Users\<username>\AppData\Local\Android\Sdk

# Linux/Mac
export ANDROID_HOME=$HOME/Android/Sdk
```

---

**Versão**: 0.1.0 | **Status**: Estrutura Inicial
