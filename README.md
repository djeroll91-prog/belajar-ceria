# Belajar Ceria

Aplikasi Android untuk kanak-kanak tadika yang baru mengenal huruf, nombor,
perkataan dan ayat. Dwibahasa: Bahasa Melayu dan English.

Ceria, ringkas dan berfungsi sepenuhnya luar talian. Tiada iklan, tiada
perpustakaan pihak ketiga.

## Muat turun

Pergi ke halaman [Releases](../../releases/latest) dan muat turun
`BelajarCeria-v1.8.apk`.

1. Buka fail APK pada telefon.
2. Benarkan "Pasang aplikasi tidak dikenali" jika diminta.
3. Tekan Pasang.

Android 5.0 ke atas.

## Kandungan mengikut KSPK

Kandungan aplikasi ini disusun mengikut **Kurikulum Standard Prasekolah
Kebangsaan (KSPK)**, Semakan 2017, Kementerian Pendidikan Malaysia.

- **6 tunjang**: Komunikasi; Kerohanian, Sikap dan Nilai; Kemanusiaan;
  Perkembangan Fizikal dan Estetika; Sains dan Teknologi; Keterampilan Diri.
- **42 unit** dan **534 item** pembelajaran, daripada huruf dan suku kata
  sehingga ayat, Jawi, nombor, wang, masa, haiwan, tumbuhan, cuaca, emosi,
  kebersihan, keselamatan dan asas ICT.
- **42 tema mingguan** sepanjang tahun tadika, seperti yang digunakan di tadika
  KEMAS dan prasekolah KPM. Skrin **Tema Mingguan** menandakan minggu semasa.

Unit yang mempunyai kod KSPK diambil terus daripada dokumen rasmi. Unit yang
tiada kod diletakkan mengikut tunjang dan tema sahaja, dan **tidak** ditandakan
sebagai standard rasmi. Sumber kandungan ialah `data/kspk.json`; fail
`Content.java` dan `web/data.js` dijana daripadanya.

## Ciri

- Navigasi mengikut tunjang KSPK: pilih tunjang, pilih unit, terus belajar.
- Kad imbas besar untuk huruf, nombor, perkataan dan ayat.
- Butang 🔊 untuk mendengar sebutan yang betul (Text-to-Speech) dan butang 🐢
  untuk sebutan perlahan.
- Butang 🎤 supaya kanak-kanak boleh bercakap dan cuba menyebut; sebutan dinilai
  dan diberi pujian.
- **Baca ikut suku kata**: bahasa Melayu diajar melalui suku kata (ba, bi, bu),
  bukan nama huruf, kerana kanak-kanak yang kenal "ba" dan "ju" boleh membaca
  "baju" tanpa menghafal abjad. Bahasa Inggeris dibina daripada huruf.
- Skrin **Ulang Kaji Pintar**: item yang selalu salah muncul lebih kerap
  (sistem kotak Leitner), dan subjek yang lemah disenaraikan untuk dilatih.
- Skrin **Tema Mingguan**: 42 tema tahun tadika, minggu semasa ditanda.
- Mini game: kuiz, memori, kira objek, bina perkataan, cari yang berbeza dan
  permainan mewarna.
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
- Mod cerah dan mod gelap, dipilih di skrin Tetapan.
- Dua suis berasingan di bar atas: 🔊 untuk kesan bunyi, 🎵 untuk muzik latar.
  Ibu bapa boleh matikan muzik tanpa kehilangan bunyi tepukan dan pujian.
  Muzik latar berhenti sendiri dalam semua permainan supaya kanak-kanak boleh
  berfikir dan mendengar soalan dengan jelas.

## Perubahan v1.8

Kandungan aplikasi ditulis semula sepenuhnya mengikut sukatan KSPK terkini.

- Enam tunjang, 42 unit, 534 item dan 42 tema mingguan menggantikan senarai
  kandungan lama. Setiap item ada nama dalam Bahasa Melayu dan English.
- Navigasi baharu: Utama → tunjang → unit → Belajar / Kuiz / Bina / Padan.
- Sistem tahap berkunci dibuang. Ia tidak lagi sesuai kerana kanak-kanak
  prasekolah perlu boleh membuka mana-mana topik bila-bila masa. Sebagai
  gantinya, kemajuan diukur mengikut penguasaan setiap item.
- **Ulang Kaji Pintar** ditambah: item yang salah disimpan dalam kotak Leitner
  dan muncul semula lebih kerap; subjek yang lemah disenaraikan untuk dilatih.
- **Tema Mingguan** ditambah: 42 tema tahun tadika dengan minggu semasa ditanda.
- Bina perkataan kini ikut cara tadika mengajar: suku kata untuk Bahasa Melayu
  (ba + ju = baju), huruf untuk Bahasa Inggeris.
- Mod gelap ditambah; semua skrin membaca warna daripada satu palet tema.
- Pembetulan: unit ayat dahulu kosong kerana ayat disimpan di tempat yang salah.
- Pembetulan: warna disimpan sebagai 8 digit heks (`#AARRGGBB`) sedangkan kod
  menjangka 6 digit, jadi kad warna tidak dikenali dan memaparkan teks heks.
- Pembetulan: senarai lama dan senarai unit membina objek `Item` yang berbeza,
  jadi pemeriksaan identiti gagal dan simbol mentah seperti `H|🏠` boleh terlepas
  ke jubin kuiz. Kini satu objek dikongsi, dan huruf dikenal pasti melalui bentuk
  simbolnya.
- Jubin kuiz huruf kini memaparkan huruf sahaja. Dahulu nama gambar ("House")
  dipaparkan di sebelah huruf, yang memberi jawapan dan mengajar bunyi yang salah.

## Perubahan v1.7

- Lagu karaoke tidak lagi terpotong. Dahulu setiap kad diberi masa tetap 1.2
  saat, sedangkan klip nombor berjalan 1.4-1.5 saat, jadi setiap perkataan
  dipotong di hujungnya. Kini kad seterusnya bermula hanya selepas audio
  perkataan semasa benar-benar tamat.
- Lagu karaoke tidak lagi senyap di sana sini. Huruf belum ada klip rakaman,
  jadi ia bergantung pada enjin suara telefon; enjin kadangkala lambat memulakan
  sebutan lalu dipotong oleh pemasa lama. Penantian audio ini juga membetulkannya.
- Ada jaring keselamatan: jika telefon tidak pernah melaporkan audio tamat, lagu
  bergerak sendiri selepas 4 saat supaya tidak tersekat pada satu huruf.
- Butang Berhenti kini benar-benar menghentikan audio, dan lagu tidak akan
  bermula semula selepas ditekan.
- Suara yang dipilih diingati, jadi lagu tidak menyemak semula senarai suara pada
  setiap perkataan. Ini mengurangkan kependaman dan tumpang tindih bunyi.

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
