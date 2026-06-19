# Nexis AI 🚀

Nexis AI is a fully featured, visually polished, and highly responsive Android assistant application. Designed with modern Android development guidelines, Nexis AI serves as an intelligent conversational companion that provides instantaneous streaming responses, adaptive image and text reasoning, custom Material 3 themed styling, and structured cloud-based persistence.

---

## 🎨 Iconography & Visual Identity

Nexis AI features a meticulously designed, custom adaptive launcher icon that ensures a delightful first impression on the user's home screen.

### The Icon Character Design
The icon is centered around a charming, gradient-filled **Speech Bubble Character** representing conversational intelligence:
- **Gradient Fill**: Shifting beautifully from soft pink (`#FFB2C8`) to deep lavender (`#D680EE`) and royal purple (`#838DF7`), ending on an energetic electric blue (`#60BAFF`).
- **Playful Character Details**: Features a dark navy friendly smile and vertical oval eyes paired with delicate semi-transparent blushes (`#80FF9CB6`) for a delightful, approachable look.
- **Sparkle Star Accent**: Adorned with a floating gradient-colored sparkle star in the upper right.
- **Safe Zone Centering**: Scaled and translated carefully within a standard `108dp x 108dp` adaptive vector viewport. It remains completely visible and aligned perfectly in the middle, avoiding any corner-clipping on target devices, squircle masks, or circular launcher formats.

---

## ✨ Features Checklist

- **🔒 Secure Architecture & User Auth**: 
  - Integrated `AuthViewModel`, standard secure state controls, and remote Firestore user sync.
  - Supports authenticated cloud sync that binds the user's conversation state to their dynamic user profile.
- **⚡ Supercharged Gemini API Performance**:
  - Out of the box connection timeouts optimized down from 60s to a snappy **15 seconds** using customized Retrofit configurations.
  - Optimized **typing effect rendering**: increased chunk size boundaries from 5 to **20 characters** and decreased step delays down to **2ms** for an ultra-fast, smooth, dynamic response streaming feel.
- **🧠 Advanced Model Thinking Options**:
  - Leverages Google's high-intelligence conversational engine.
  - Integrates a **High Thinking / Deep Reasoning** switch toggling extra reasoning paths when handling complex tasks.
- **🎨 Dynamic Material 3 Multi-Theme Engine**:
  - Fully integrated system-wide custom theme configurations syncing to Firestore real-time storage.
  - Supports dark-mode toggling + **nine (9) custom style profiles**:
    - 🟢 Green (GptGreen)
    - 🟣 Lavender (LavenderPrimary)
    - 🔵 Pastel Blue (PastelBluePrimary)
    - 💖 Pink (PinkPrimary)
    - 🔴 Red (RedPrimary)
    - 🟡 Yellow (YellowPrimary)
    - 🟢 Mint (MintPrimary)
    - 🍑 Peach (PeachPrimary)
    - 👾 Purple (PurplePrimary)
- **💬 Rich Interactive Chat Interface**:
  - Seamless text editing and message deletion with instant local/remote state updates.
  - Base64 image attachment capture and inline multi-modal model analysis.
  - Code visual extraction: Beautiful formatted Markdown code block styling with quick-tap copy buttons.
  - User feedback interactions: Upvote/downvote and message copy controls.

---

## 🛠️ Architecture & Tech Stack

This project is built using professional Android Clean Architecture & MVVM design principles in 100% Kotlin:

- **UI Framework**: **Jetpack Compose** with complete Material Design 3 guidelines, dynamic material color adaptations, edge-to-edge window insets, and accessibility compliance.
- **Concurrency**: Kotlin **Coroutines and flow streams** (`MutableStateFlow`, `collectAsState`, `LaunchedEffect`) to avoid blocking main threads.
- **Networking**: Snappy configurations using **Retrofit**, **OkHttpClient**, and high-speed Json serialization.
- **Data Persistence**: Local runtime state backed up with real-time remote syncing to **Google Firestore database pipelines**.

---

## 🏃 How to Run, Test, and Build

### Prerequisites
- Android Studio Ladybug (or higher)
- JDK 17
- Active API Key configured within your safe environment variables dashboard (using `.env`).

### Environment Secrets Setting
Nexis AI manages its operational keys gracefully. To insert your actual Google Gemini API credentials:
1. Open the Google AI Studio **Secrets Panel**.
2. Add your variable with the key `GEMINI_API_KEY`.
3. Build the project; the compile engine will safely inject this into your `BuildConfig` to prevent key exposure in the repository.

### Commands
Use standard Gradle commands to compile and test:
```bash
# Build the application
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 💡 About Nexis AI
Whether you need a companion to help you write code snippets, reason through math, categorize notes, or simply have an engaging human-like chat, Nexis AI does it all with a premium, lightning-fast UI that lets you learn and solve problems in real-time. Created on Google AI Studio as part of an advanced agentic experiment!
