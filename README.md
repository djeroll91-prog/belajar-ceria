# Belajar Ceria

Aplikasi Android untuk kanak-kanak tadika yang baru mengenal huruf, nombor,
perkataan dan ayat. Dwibahasa: Bahasa Melayu dan English.

Ceria, ringkas dan berfungsi sepenuhnya luar talian. Tiada iklan, tiada
perpustakaan pihak ketiga.

## Muat turun

Pergi ke halaman [Releases](../../releases/latest) dan muat turun
`BelajarCeria-v1.6.apk`.

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
- Mini game: kuiz berperingkat, memori, kira objek, eja perkataan, cari yang
  berbeza dan permainan mewarna.
- Permainan baharu untuk merangsang otak:
  - **Apa Seterusnya?** - lengkapkan corak bergambar (latihan logik).
  - **Letup Gelembung** - kenal huruf dan nombor dengan pantas.
  - **Bakul Susun** - asingkan haiwan, tumbuhan dan buah ke bakul betul.
  - **Tambah & Tolak** - operasi pertama, sentiasa bergambar untuk dikira.
  - **Tulis Huruf / Nombor / Jawi** - surih dengan jari; pujian hanya diberi
    selepas garis huruf benar-benar dilalui.
  - **Sebut Jawi** - sebut nama huruf Jawi dengan tepat.
- Sebutan Jawi yang betul: nama huruf ikut piawaian KSSR/DBP. Permainan **tidak**
  menerima sebutan yang dieja satu-satu ("K-H-A"); kanak-kanak mesti menyebut
  "kha" sekali gus. Enjin suara diberi frasa pembawa ("Ini kha") supaya ia tidak
  mengeja nama huruf itu.
- Animasi pada setiap tindakan: butang membesar, jawapan betul melompat (pop),
  jawapan salah bergegar lembut, dan maskot robot berkelip serta bergerak mulut.
- Sistem markah, bintang, lencana, rentetan jawapan betul dan konfeti.
- Tahap berkunci: tahap seterusnya hanya terbuka selepas lulus markah minimum.
- Paparan trend kemajuan dan laluan belajar.
- Dua suis berasingan di bar atas: 🔊 untuk kesan bunyi, 🎵 untuk muzik latar.
  Ibu bapa boleh matikan muzik tanpa kehilangan bunyi tepukan dan pujian.
  Muzik latar berhenti sendiri dalam semua permainan supaya kanak-kanak boleh
  berfikir dan mendengar soalan dengan jelas.

## Perubahan v1.6

- Muzik latar tidak lagi berbunyi semasa permainan. Ia berhenti dalam semua
  permainan, studio rakaman, karaoke dan skrin mikrofon, dan hanya berbunyi di
  halaman utama serta skrin kad imbas.
- Suis muzik dipisahkan daripada suis kesan bunyi.
- Kuiz huruf tidak lagi memaparkan simbol mentah seperti `A|🍎`; hanya huruf
  yang dipaparkan.
- Skrin kad imbas huruf kini menyebut **nama huruf** ("A", "B", "C"), bukan nama
  gambar di sebelahnya ("Apple", "Ball"). Nama huruf ikut sebutan Malaysia
  (DBP): A = "ei", B = "bi", C = "si".
- Lagu ABC di skrin karaoke kini menyanyikan keseluruhan 26 huruf, bukan hanya
  "A", dan baris kad boleh diskrol dengan kad semasa sentiasa di tengah.

## Suara manusia (pilihan)

Aplikasi berfungsi sepenuhnya dengan suara enjin telefon. Untuk sebutan yang
lebih jelas, klip rakaman manusia dari Wikimedia Commons boleh dibundel:

```bash
./tools/refetch_loop.sh      # muat turun (Commons hadkan kadar, jadi berulang)
python3 tools/voice_pack.py bundle build/voice-commons
python3 tools/voice_pack.py check
```

Kredit perakam ditulis ke `app/src/main/assets/voice-credits.csv` dan dipaparkan
dalam aplikasi di skrin **Kredit suara**. Klip ialah CC BY-SA 4.0, jadi kredit
mesti kekal bersama klip; satu ujian unit memastikan setiap klip ada kreditnya.

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
