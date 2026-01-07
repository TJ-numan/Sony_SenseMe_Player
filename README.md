# 🎵 SenseMePlayer

SenseMePlayer is an **Android music player** that automatically **analyzes your local music library**, extracts **audio features (MFCC)**, and **groups songs into mood-based clusters** using **K-Means clustering**. You can then play songs by selecting a mood instead of manually creating playlists.

This project is built as a **learning-focused but functional prototype** combining **audio signal processing**, **machine learning**, **Room database**, **coroutines**, and **Jetpack Compose UI**.

---

## ✨ Features

* 📂 Scan local music files from device storage
* 🎧 Extract **MFCC (Mel-Frequency Cepstral Coefficients)** from each song
* 💾 Store extracted features in **Room Database**
* 🧠 Apply **K-Means clustering** to group songs into moods
* 🎼 View songs grouped by mood
* ▶️ Built-in **Music Player**

  * Play / Pause
  * Next / Previous
  * Select song by tapping
* 📊 Processing **ProgressBar** for long operations
* 🧩 Modern UI using **Jetpack Compose**

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose
* **Audio Processing:** TarsosDSP
* **Database:** Room
* **Concurrency:** Kotlin Coroutines
* **JSON:** Gson
* **Media Playback:** Android MediaPlayer
* **Architecture:** Single-Activity (learning-oriented)

---

## 📱 How It Works

1. **Music Scan**
   Scans device storage for audio files.

2. **Feature Extraction**
   Converts audio into PCM and extracts MFCC features.

3. **Persistence**
   Stores MFCC data as JSON in Room DB.

4. **Clustering**
   Uses a simple **K-Means implementation in Kotlin** to group songs.

5. **Mood UI**
   Displays clusters as mood categories.

6. **Playback**
   User selects a mood → selects a song → music plays 🎶

---


## 🚀 Getting Started

### Prerequisites

* Android Studio (Giraffe or newer recommended)
* Android device or emulator (API 26+)
* Permission to read external storage

### Clone the Repository

```bash
git clone https://github.com/your-username/SenseMePlayer.git
```

### Open in Android Studio

1. Open Android Studio
2. Select **Open an existing project**
3. Choose the cloned folder
4. Sync Gradle

---

## 🔐 Permissions Required

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

> For Android 13+, ensure proper **media permissions** are handled.

---

## ⚠️ Known Limitations

* Mood labels are numeric (Cluster 0, 1, 2…)
* No real-time audio visualization yet
* MFCC extraction can be slow for large libraries
* Designed mainly for **learning & experimentation**, not production

---

## 🧪 Future Improvements

* 🎚 Playback seek bar (progress slider)
* 🏷 Human-readable mood labels (Happy, Chill, Energetic)
* 🎼 Background playback with notification controls
* ⚡ Faster feature extraction
* ☁ Cloud backup of mood data
* 🧠 Improved ML model

---

## 🤝 Contributing

Contributions, suggestions, and improvements are welcome!

1. Fork the repository
2. Create a new branch
3. Commit your changes
4. Open a Pull Request

---

## 📄 License

This project is open-source and available under the **MIT License**.

---

## 🙌 Acknowledgements

* [TarsosDSP](https://github.com/JorenSix/TarsosDSP)
* Android Jetpack Team
* Open-source Android community





---

⭐ If you find this project helpful, consider starring the repo!
