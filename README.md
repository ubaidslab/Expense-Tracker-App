# Kharcha

Personal kharcha tracker. Aap bolein ya likhein ("aaj bijli ka bill 5000 diya"), AI entry samajh kar confirm karwaye, phir save ho jati hai. Monthly report PDF mein share ho sakti hai.

## Features
- Chat-style input (likh kar ya bol kar)
- AI parsing: Google Gemini ya Groq (aap apni API key daalein)
- Confirm-before-save: AI ki galti record mein nahi jati
- Entries list, edit, delete, aur har change ka audit history
- Monthly report, category aur tag totals, PDF share, CSV export

## Privacy
- Data is phone ki local database (Room) mein rehta hai. Koi server nahi.
- API key Android Keystore se encrypted hai. Code mein koi key nahi.
- AI ko sirf woh ek sentence jata hai jo aap ne likha ya bola.
- Gemini ke free tier par Google inputs ko product improvement ke liye use kar sakta hai. Sensitive data ke liye paid tier ya Groq istemal karein.

## Build
1. Android Studio mein project kholein, ya GitHub Actions se build karein (`.github/workflows/build.yml`).
2. Debug APK: `gradle :app:assembleDebug` (output: `app/build/outputs/apk/debug/`).
3. Install: APK file phone par khol kar "Install unknown apps" allow karein.

## Requirements
- Android 8.0 (API 26) ya naya
- Internet (sirf AI parsing ke liye; entries aur report offline chalti hain)

## Tech
Kotlin, Jetpack Compose (Material 3), Room, EncryptedSharedPreferences, Android PdfDocument.

## License
MIT (agar aap chahein to LICENSE file add karein).
