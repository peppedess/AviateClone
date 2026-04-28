# AviateClone Launcher 🚀

Un launcher Android ispirato ad Aviate Launcher, con suggerimenti contestuali basati sull'ora del giorno.

## Funzionalità

- ⏰ **Orologio e data** in bella vista sulla home
- 🧠 **App suggerite dinamicamente** in base all'ora:
  - Mattina → News, Email, Salute
  - Pendolarismo → Mappe, Musica
  - Lavoro → Produttività, Comunicazione
  - Pranzo → Food delivery, Social
  - Sera → Intrattenimento, Media
  - Notte → Gaming, Streaming
- 📱 **App drawer** con scorrimento verso l'alto, sezioni alfabetiche
- 🔍 **Ricerca app** integrata nel drawer
- 📊 **Frequenza d'uso** — le app usate spesso salgono in cima
- 🔄 **Aggiornamento automatico** quando installi/disinstalli app

## Come buildare

### Prerequisiti
- Android Studio Hedgehog (2023.1.1) o superiore
- JDK 17
- Android SDK 34

### Passaggi
1. Apri **Android Studio**
2. Clicca su **"Open"** e seleziona la cartella `AviateClone`
3. Aspetta che Gradle sincronizzi le dipendenze
4. Collega il tuo Pixel via USB (abilita **Opzioni sviluppatore** e **Debug USB**)
5. Clicca **▶ Run** (o premi Shift+F10)
6. Sul telefono, quando ti chiede di scegliere il launcher predefinito, seleziona **AviateClone**

### In alternativa (build APK)
1. In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. L'APK si trova in: `app/build/outputs/apk/debug/app-debug.apk`
3. Trasferiscilo sul telefono e installalo

## Struttura del progetto

```
app/src/main/
├── java/com/aviateclone/launcher/
│   ├── ui/
│   │   ├── MainActivity.kt       ← Schermata principale
│   │   └── LauncherViewModel.kt  ← Logica e dati
│   ├── data/
│   │   ├── AppInfo.kt            ← Modello dati app
│   │   └── AppLoader.kt          ← Carica app installate
│   ├── engine/
│   │   └── ContextEngine.kt      ← Suggerimenti contestuali
│   ├── adapter/
│   │   ├── AppGridAdapter.kt     ← Griglia home
│   │   └── AppDrawerAdapter.kt   ← Lista drawer
│   └── receiver/
│       └── AppChangeReceiver.kt  ← Ascolta installazioni
└── res/
    ├── layout/                   ← Layout XML
    ├── drawable/                 ← Sfondi e icone
    └── values/                   ← Colori, temi, stringhe
```

## Personalizzazione

- **Cambia colori**: modifica `res/values/colors.xml`
- **Aggiungi categorie app**: modifica `AppLoader.kt` → funzione `guessCategory()`
- **Cambia logica temporale**: modifica `ContextEngine.kt` → `getCurrentContext()`
- **Modifica quante app mostrare**: nel `ContextEngine.kt` cambia `maxCount`
