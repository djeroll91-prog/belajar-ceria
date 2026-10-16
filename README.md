# Belajar Ceria

Aplikasi Android untuk kanak-kanak tadika yang baru mengenal huruf, nombor,
perkataan dan ayat. Dwibahasa: Bahasa Melayu dan English.

Ceria, ringkas dan berfungsi sepenuhnya luar talian. Tiada iklan, tiada
perpustakaan pihak ketiga.

## Muat turun

Pergi ke halaman [Releases](../../releases/latest) dan muat turun
`BelajarCeria-v1.1.apk`.

1. Buka fail APK pada telefon.
2. Benarkan "Pasang aplikasi tidak dikenali" jika diminta.
3. Tekan Pasang.

Android 5.0 ke atas.

## Ciri

- Kad imbas besar untuk huruf, nombor, perkataan dan ayat.
- Butang 🔊 untuk mendengar sebutan yang betul (Text-to-Speech) dan butang 🐢
  untuk sebutan perlahan.
- Butang 🎤 supaya kanak-kanak boleh bercakap dan cuba menyebut; sebutan dinilai
  dan diberi pujian.
- Kandungan tambahan: warna, tubuh badan, haiwan, tumbuh-tumbuhan, buah-buahan,
  benda hidup, huruf Jawi dan huruf Arab.
- Mini game: kuiz berperingkat, memori, kira objek dan permainan mewarna.
- Sistem markah, bintang, lencana, rentetan jawapan betul dan konfeti.
- Maskot robot animasi yang berkelip mata dan bergerak mulut.
- Tahap berkunci: tahap seterusnya hanya terbuka selepas lulus markah minimum.
- Paparan trend kemajuan dan laluan belajar.

## Bina sendiri

Perlu JDK 17+, Android SDK (platform 34, build-tools 34.0.0) dan Gradle 8.9.

```bash
gradle :app:assembleRelease        # APK release (ditandatangani)
gradle :app:assembleDebug          # APK debug
gradle :app:testDebugUnitTest      # ujian unit + Robolectric
```

Hasil: `app/build/outputs/apk/release/app-release.apk`.

Ikon pelancar dijana oleh `tools/make_icons.py` (Python tulen, tanpa Pillow):

```bash
python3 tools/make_icons.py
```

## Lesen

Dikongsi untuk kegunaan pendidikan.
