# BrainPal: See Your Reel Count

## Product Requirements Document (PRD)

Dokumen kebutuhan produk untuk aplikasi penghitung reels dan pemblokir Reels dan Shorts di Android. Menghitung setiap scroll secara live, menampilkannya sebagai angka dan kondisi otak, lalu menghentikannya saat limit harian tercapai.

| Item | Detail |
| --- | --- |
| Nama produk | BrainPal: See Your Reel Count |
| Developer | EpowerX Labs Private Limited, Bangalore, India |
| Package Android | com.brainrot.android |
| Situs resmi | https://www.brainpalapp.ai/ |
| Versi PRD | 1.0 |
| Tanggal | 21 November 2026 |
| Penulis | Disusun dari riset publik atas permintaan Nico |
| Status | Reverse engineering PRD. Dokumen ini bukan dokumen resmi EpowerX Labs dan tidak berafiliasi dengan EpowerX Labs. |
| Bahasa dokumen | Bahasa Indonesia |

### Cara membaca dokumen ini

Setiap klaim penting diberi label:

- **FAKTA**: terverifikasi dari sumber publik yang dicantumkan.
- **ASUMSI**: dugaan penulis PRD yang masih perlu diuji.
- **REKOMENDASI**: usulan penulis PRD, bukan fitur yang sudah ada.

### Tahapan visual otak

| Jumlah reels | Kondisi otak |
| ---: | --- |
| 0 | Sehat |
| 50 | Mulai lelah |
| 100 | Retak |
| 150 | Layu abu abu |
| 200+ | Hilang |

Tahapan di atas adalah fakta terverifikasi dari situs resmi brainpalapp.ai, diakses 21 November 2026. **[FAKTA - brainpalapp.ai]**

## Daftar isi

1. Ringkasan eksekutif
2. Latar belakang dan pernyataan masalah
3. Target pengguna dan persona
4. Tujuan produk dan metrik sukses
5. Ruang lingkup
6. Fitur detail, user stories, dan acceptance criteria
7. Alur pengguna
8. Model bisnis dan monetisasi
9. Kebutuhan non fungsional
10. Analisis kompetitor
11. Bukti pasar dan suara pengguna
12. Risiko, keterbatasan, dan ketergantungan
13. Roadmap yang diusulkan
14. Pertanyaan terbuka dan asumsi
15. Lampiran sumber
16. Lampiran: Memakai PRD ini di Google AI Studio

## Identitas produk yang benar

Dokumen ini hanya membahas satu produk: **BrainPal: See Your Reel Count** dari EpowerX Labs Private Limited. Produk ini adalah reels counter dan reel blocker untuk Android saja dan belum tersedia di iPhone. **[FAKTA - brainpalapp.ai]**

Produk ini bukan brainpal.app, yaitu aplikasi voice to task berbasis AI dengan nama mirip. Produk ini juga bukan Brainpal Heal and Meditation di iOS. Menyamakan ketiganya akan menghasilkan PRD yang salah sejak halaman pertama.

> **Inti produk dalam satu kalimat:** BrainPal tidak mengukur berapa lama kamu membuka aplikasi. BrainPal mengukur berapa banyak reels yang benar benar kamu scroll, menampilkannya live di layar, dan pada paket berbayar menghentikan scroll itu saat angka harianmu habis.

## 1. Ringkasan eksekutif

BrainPal adalah aplikasi produktivitas dan digital wellbeing untuk Android yang dibuat khusus untuk konten video pendek. Aplikasi ini menghitung setiap reels dan short yang di-scroll pengguna secara real time di Instagram, YouTube, Facebook, dan Snapchat, lalu menampilkan angkanya langsung di layar sebagai overlay. **[FAKTA - brainpalapp.ai dan listing Google Play via AppBrain]**

Keunggulan pembedanya bukan pada pemblokiran. Banyak aplikasi bisa memblokir aplikasi lain. Pembeda BrainPal ada pada tiga hal yang bekerja bersamaan. Pertama, satuan ukurnya adalah reels, bukan menit. Kedua, umpan baliknya visual dan emosional melalui kondisi otak yang memburuk seiring angka naik. Ketiga, ada unsur sosial melalui Scroll Battles, di mana jumlah reels yang lebih rendah menang.

Modelnya freemium. Counter, tampilan otak, statistik harian, dan battles gratis. Kemampuan untuk benar benar berhenti, yaitu daily reel limit, blokir Reels dan Shorts saat limit tercapai, dan breaks saat scrolling, dikunci di BrainPal Plus yang berbayar. **[FAKTA - brainpalapp.ai, bagian Pricing dan FAQ]**

Traksinya besar untuk kategori ini. Per 19 November 2026, AppBrain mencatat rating 4,36 dari 129.627 ulasan dan 2,6 juta install, dengan sekitar 500 ribu install dalam 30 hari terakhir. **[FAKTA - AppBrain]**

Di sisi lain, ulasan pengguna berulang kali mengeluhkan onboarding yang agresif, permission yang mati sendiri, dan masalah restore langganan. Artinya, masalah produk saat ini bukan pada ide, melainkan pada kepercayaan dan keandalan.

> **Kesimpulan PRD:** Prioritas tertinggi bukan menambah platform atau fitur baru. Prioritas tertinggi adalah membuat counter tidak pernah berhenti diam diam, membuat onboarding terasa jujur, dan membuat status langganan selalu bisa dipulihkan. Tanpa tiga hal itu, fitur baru hanya menambah permukaan keluhan.

## 2. Latar belakang dan pernyataan masalah

### Masalahnya bukan screen time. Masalahnya adalah angka yang tidak terlihat.

Fitur screen time bawaan ponsel melaporkan durasi, misalnya 2 jam di Instagram. Angka itu abstrak. Pengguna bisa merasa 2 jam itu wajar karena mengira sebagian besar waktunya dipakai untuk melihat pesan atau profil. Padahal di dalam 2 jam itu bisa ada 400 sampai 800 reels yang lewat tanpa diingat satu pun.

BrainPal membingkai ulang masalahnya dengan satuan yang konkret. Satu scroll adalah satu reels. Setiap reels menambah angka di layar dan memperburuk kondisi otak. Pengguna tidak lagi menebak. Ia melihat. **[FAKTA - brainpalapp.ai: otak sehat di 0, retak di 100, hilang di 200 lebih]**

### Pernyataan masalah

Pengguna video pendek kehilangan kendali bukan karena ia tidak tahu scrolling itu buruk, melainkan karena tidak ada sinyal berhenti di dalam pengalaman scrolling itu sendiri. Feed dirancang tanpa akhir, tanpa nomor halaman, dan tanpa total. Akibatnya sesi yang diniatkan 5 menit berubah menjadi 45 menit, dan pengguna baru sadar setelahnya dari laporan durasi yang sudah terlambat.

### Akar masalah yang ditangani produk

- **Tidak ada umpan balik saat kejadian berlangsung.** Laporan screen time datang setelah sesi selesai, ketika waktu sudah hilang.

- **Satuan menit menyembunyikan volume.** Menit tidak membedakan 10 reels yang ditonton penuh dan 200 reels yang di-skip cepat.

- **Tidak ada batas yang mengikat.** Niat untuk berhenti tidak punya mekanisme penegak di dalam aplikasi tujuan.

- **Tidak ada akuntabilitas sosial.** Kebiasaan scrolling bersifat privat, sehingga tidak ada tekanan positif dari teman.

- **Pemblokir aplikasi umum terlalu kasar.** Memblokir Instagram secara penuh juga memutus pesan dan fungsi lain yang masih dibutuhkan pengguna.

BrainPal menjawab kelimanya dengan counter live di dalam feed, satuan reels, limit harian dengan blokir yang hanya menyasar Reels dan Shorts, serta battles antar teman. Sisa aplikasi tujuan tetap bisa dipakai saat blokir aktif. **[FAKTA - brainpalapp.ai, FAQ blokir]**

## 3. Target pengguna dan persona

Segmen inti BrainPal adalah pengguna Android yang merasa scrolling video pendek sudah mengganggu belajar, kerja, atau tidur, tetapi belum mau menghapus aplikasi sosialnya. Persona di bawah ini adalah **[REKOMENDASI]** penulis PRD berdasarkan positioning produk dan pola ulasan, bukan data riset resmi EpowerX Labs.

### Persona A: Pelajar dan mahasiswa yang mengejar fokus

- **Profil:** Usia 16 sampai 24 tahun, Android kelas menengah, membuka Reels dan Shorts saat jeda belajar dan sebelum tidur.

- **Kebutuhan:** Bukti konkret berapa banyak reels yang sudah ia habiskan hari ini, batas harian yang realistis seperti 50 reels, dan cara membuktikan ke teman bahwa ia lebih disiplin.

- **Frustrasi:** Pengingat screen time bawaan mudah diabaikan. Ia merasa bersalah setelah sesi panjang, tetapi tidak punya sinyal untuk berhenti di tengah sesi.

- **Fitur penentu:** Counter live, visual otak, Scroll Battles, dan streak. Harga menjadi penentu karena uang saku terbatas, sehingga harga tahunan yang didiskon terasa jauh lebih masuk akal daripada bulanan.

- **Kalimat khas:** Aku cuma mau istirahat 10 menit, tahu tahu sudah 300 reels.

### Persona B: Pekerja muda yang ingin merebut kembali malamnya

- **Profil:** Usia 25 sampai 34 tahun, bekerja kantoran atau freelance, scrolling terberat terjadi setelah jam kerja dan menjelang tidur.

- **Kebutuhan:** Blokir yang bekerja otomatis pada jam tertentu dan saat limit tercapai, tanpa memblokir WhatsApp atau fungsi Instagram lain yang masih dipakai untuk kerja dan keluarga.

- **Frustrasi:** Sudah mencoba app timer dan mode fokus, tetapi selalu menekan tombol tunda. Ia butuh friksi yang lebih keras pada momen yang tepat, bukan laporan mingguan.

- **Fitur penentu:** Daily reel limit, Blocks terjadwal, mode Strict, dan breaks. Ia paling sensitif terhadap keandalan. Satu kali counter mati diam diam membuat ia berhenti percaya dan berhenti berlangganan.

- **Kalimat khas:** Blokir saja reels-nya. Pesan dan pencarian jangan ikut dimatikan.

### Persona C: Orang tua atau pendamping anak dan remaja

- **Profil:** Usia 35 sampai 50 tahun, memasang aplikasi di ponsel anak atau memakai battles sebagai pendamping kebiasaan keluarga.

- **Kebutuhan:** Cara memulai percakapan tentang scrolling tanpa ceramah, dengan angka yang sama sama bisa dilihat, plus batas harian yang disepakati.

- **Frustrasi:** Ia tidak bisa mengawasi setiap sesi dan tidak ingin memata matai isi tontonan. Ia juga paling khawatir soal izin aksesibilitas yang terdengar menakutkan.

- **Fitur penentu:** Penjelasan privasi yang jernih, battles sebagai permainan keluarga, dan laporan harian yang ringkas. **[ASUMSI]** Mode keluarga khusus belum terverifikasi sebagai fitur resmi, sehingga kebutuhan ini masuk roadmap, bukan klaim fitur saat ini.

- **Kalimat khas:** Aku tidak perlu tahu kamu nonton apa. Aku cuma perlu tahu angkanya tidak lepas kendali.

## 4. Tujuan produk dan metrik sukses

### Tujuan produk

- Membuat konsumsi reels terlihat pada saat konsumsi itu terjadi, bukan setelahnya.
- Menurunkan jumlah reels harian pengguna aktif secara terukur dalam 14 hari pertama.
- Mengubah kesadaran menjadi kendali melalui limit dan blokir yang hanya menyasar feed video pendek.
- Membangun kebiasaan harian melalui streak dan akuntabilitas sosial melalui battles.
- Mengonversi pengguna gratis yang sudah merasakan manfaat menjadi pelanggan Plus tanpa merusak kepercayaan.

### North Star Metric

**North Star:** Jumlah pengguna aktif mingguan yang menurunkan median reels hariannya dibanding minggu pertamanya, sambil tetap aktif di BrainPal pada minggu berikutnya. **[REKOMENDASI]**

Alasan pemilihan North Star ini sederhana. Install dan rating bisa naik karena rasa penasaran, tetapi nilai produk hanya terbukti bila angka reels benar benar turun dan pengguna bertahan. Metrik ini juga mencegah tim mengoptimalkan rasa takut sesaat yang membuat pengguna uninstall.

### KPI per tahap

| Tahap | Metrik | Definisi dan catatan |
| --- | --- | --- |
| Akuisisi | Install baru per hari, peringkat kategori | AppBrain mencatat rata rata sekitar 17 ribu install per hari pada 30 hari terakhir dan peringkat Indonesia nomor 78 Top Free Productivity. Angka ini adalah baseline pasar, bukan target internal resmi. **[FAKTA - AppBrain]** |
| Aktivasi | Persentase install yang menyelesaikan setup permission dan tercatat reels pertamanya | Titik kritis produk. Counter yang tidak jalan pada sesi pertama adalah penyebab uninstall dan rating bintang satu yang paling sering muncul di ulasan. |
| Keterlibatan | Hari aktif per minggu, sesi dengan counter tampil, battles aktif per pengguna | Diukur terpisah untuk pengguna gratis dan Plus agar nilai sosial dan nilai blokir tidak tercampur. |
| Retensi | Retensi hari ke 7, 14, dan 30, serta streak aktif | Streak berbentuk api menyala bila pengguna tetap di bawah limit harian. **[FAKTA - catatan rilis di AppBrain]** |
| Konversi Plus | Trial atau paywall view ke berbayar, restore success rate, refund rate | Restore success rate wajib dipantau sebagai metrik kepercayaan, bukan hanya metrik pembayaran, karena keluhannya muncul berulang di ulasan. |
| Hasil pengguna | Perubahan median reels harian hari ke 1 dibanding hari ke 14 | Testimoni yang dikutip situs dan ulasan menyebut penurunan dari 1000 lebih ke sekitar 30 dalam dua minggu. Angka itu testimoni, bukan hasil studi, dan tidak boleh dipakai sebagai janji pemasaran tanpa verifikasi. **[FAKTA - testimoni di brainpalapp.ai]** |

### Angka pasar saat ini

| Indikator | Nilai |
| --- | ---: |
| Rating | 4,36 dari 129.627 ulasan |
| Total install | 2,6 juta |
| Install 30 hari terakhir | 500 ribu |
| Ukuran aplikasi | 38,7 MB |

Semua angka pada tabel di atas berasal dari AppBrain untuk listing Google Play, versi 23.1.698, diperbarui 19 November 2026. Peringkat Indonesia nomor 37 Top Grossing Productivity menunjukkan monetisasi berjalan, bukan hanya unduhan gratis.

## 5. Ruang lingkup

### Masuk ruang lingkup

- Platform Android melalui Google Play, package com.brainrot.android. **[FAKTA - AppBrain]**
- Pelacakan reels di Instagram dan Instagram Lite, YouTube, Facebook dan Facebook Lite, serta Snapchat dan Snapchat Lite. **[FAKTA - Privacy Policy resmi]**
- Counter live dengan overlay, visual kondisi otak, statistik harian, streak, dan Scroll Battles.
- Untuk Plus: daily reel limit, blokir Reels dan Shorts saat limit tercapai, breaks saat scrolling, Blocks dan mode Strict.
- Langganan melalui Google Play dengan pengelolaan entitlement dan pemulihan pembelian.
- Akun opsional dengan Google Sign In untuk battles, sinkronisasi, dan backup riwayat hitungan.

### Tidak masuk ruang lingkup

- Aplikasi iPhone atau iOS. Situs resmi menyatakan belum tersedia di iPhone. **[FAKTA - brainpalapp.ai]**
- Pemblokiran seluruh aplikasi, situs web umum, atau kategori konten di luar feed video pendek yang didukung.
- Pembacaan isi video, pesan, komentar, atau teks yang diketik pengguna. Privacy Policy resmi menyatakan hal ini tidak dilakukan.
- Diagnosis atau klaim medis tentang kerusakan otak. Visual otak adalah metafora perilaku, bukan pengukuran medis.
- Pelacakan lokasi, kontak, foto, kamera, atau mikrofon. Privacy Policy resmi menyatakan data ini tidak dikumpulkan.
- Fitur voice to task. Itu adalah produk lain bernama mirip di domain berbeda dan bukan bagian dari produk ini.

> **Prinsip batas:** Bila sebuah fitur mengharuskan BrainPal membaca isi konten pengguna, fitur itu di luar ruang lingkup, apa pun manfaatnya. Kepercayaan privasi adalah bagian dari produk, bukan lampiran hukum.

## 6. Fitur detail, user stories, dan acceptance criteria

Setiap fitur ditulis dengan format yang sama: deskripsi, user stories, lalu acceptance criteria yang bisa diuji oleh QA. Label sumber membedakan fitur resmi dari usulan perbaikan.

### 6.1 Onboarding dan setup permission

**Deskripsi:** Alur perkenalan yang menjelaskan masalah scrolling, menanyakan kebiasaan dan target pengguna, meminta target harian, lalu memandu pengaktifan Accessibility Service dan izin overlay sampai counter benar benar jalan. Catatan rilis menyebut alur mulai yang lebih jelas: melihat jumlah scroll, memilih limit harian, lalu mendapatkan rencana. **[FAKTA - catatan rilis AppBrain]**

**User stories:**

- Sebagai pengguna baru, aku ingin tahu dalam waktu kurang dari satu menit mengapa aplikasi ini meminta izin aksesibilitas, agar aku tidak takut dan tidak langsung menghapus aplikasi.
- Sebagai pengguna baru, aku ingin membuktikan counter bekerja pada reels pertamaku sebelum diminta berlangganan, agar aku percaya pada nilainya.

**Acceptance criteria:**

- Setiap layar permintaan izin menjelaskan fungsi izin, data yang tidak diambil, dan cara mencabutnya di Pengaturan Android.
- Pengguna dapat melewati survei non esensial dan tetap mencapai layar setup permission.
- Setelah kedua izin aktif, membuka reels di aplikasi yang didukung menaikkan counter dalam sesi yang sama tanpa membuka ulang BrainPal.
- Bila salah satu izin mati, Home menampilkan status yang jelas dan tombol perbaikan, bukan angka nol yang diam.
- Paywall tidak menghalangi pembuktian counter pertama. **[REKOMENDASI]** berdasarkan keluhan onboarding agresif di ulasan.

### 6.2 Live reel counter dan overlay

**Deskripsi:** Setiap reels atau short yang di-scroll menambah hitungan secara real time. Angka tampil sebagai overlay kecil di tepi layar di atas aplikasi tujuan, dan total hari ini tampil di dalam ring pada Home. Hitungan dibedakan per aplikasi sumber. **[FAKTA - brainpalapp.ai dan catatan rilis]**

**User stories:**

- Sebagai pengguna, aku ingin melihat angkaku naik pada setiap scroll tanpa keluar dari Instagram atau YouTube, agar sinyal berhenti datang tepat saat aku scrolling.
- Sebagai pengguna, aku ingin tahu aplikasi mana yang paling banyak menyumbang reels hari ini, agar aku tahu kebiasaan mana yang harus diubah dulu.

**Acceptance criteria:**

- Scroll ke reels berikutnya menaikkan counter tepat satu dan tampil tanpa jeda yang terasa.
- Kembali ke reels sebelumnya tidak menggandakan hitungan secara tidak wajar sesuai aturan deduplikasi yang didokumentasikan tim.
- Overlay tetap terlihat di aplikasi yang didukung, tidak menutupi tombol utama aplikasi tujuan, dan dapat dipindah atau diperkecil. **[ASUMSI]** detail posisi perlu verifikasi langsung di aplikasi.
- Total di Home, total overlay, dan total per aplikasi selalu sama setelah sesi berakhir.
- Bila deteksi berhenti, aplikasi memberi tahu pengguna. Catatan rilis menyebut BrainPal kini memberi tahu bila ia berhenti menghitung. **[FAKTA - AppBrain]**

### 6.3 Brain state visual

**Deskripsi:** Representasi otak yang memburuk mengikuti jumlah reels harian: sehat pada 0, mulai lelah pada 50, retak pada 100, layu dan abu abu pada 150, serta hilang atau membusuk pada 200 lebih. Visual ini adalah umpan balik emosional inti produk. **[FAKTA - brainpalapp.ai]**

**User stories:**

- Sebagai pengguna, aku ingin melihat dampak scrollingku dalam gambar yang langsung aku pahami, agar angkanya tidak terasa abstrak.

**Acceptance criteria:**

- Perubahan tahap terjadi tepat pada ambang 50, 100, 150, dan 200 dan konsisten antara Home dan layar statistik.
- Tahap kembali ke kondisi awal pada pergantian hari sesuai zona waktu perangkat.
- Setiap tahap disertai label teks, bukan hanya gambar, agar tetap bisa dipahami pengguna dengan keterbatasan penglihatan warna.
- Salinan layar tidak menyatakan visual ini sebagai diagnosis medis.

### 6.4 Statistik harian dan riwayat

**Deskripsi:** Ringkasan reels hari ini, sisa kuota menuju limit, rincian per aplikasi, dan riwayat harian untuk melihat tren. Situs resmi mencantumkan brain view dan daily stats sebagai bagian dari paket gratis. **[FAKTA - brainpalapp.ai]**

**User stories:**

- Sebagai pengguna, aku ingin membandingkan hari ini dengan rata rata mingguku, agar aku tahu apakah aku benar benar membaik atau hanya merasa membaik.

**Acceptance criteria:**

- Home menampilkan hitungan hari ini di dalam ring, sisa menuju limit bila limit aktif, dan status streak.
- Riwayat tidak hilang setelah aplikasi ditutup atau ponsel direstart, dan tersinkron bila pengguna masuk dengan akun. Sinkronisasi dan backup riwayat memerlukan akun menurut Privacy Policy. **[FAKTA - Privacy Policy]**
- Pengguna dapat menghapus akun dan data terkait melalui pengaturan atau halaman penghapusan resmi.

### 6.5 Daily reel limit

**Deskripsi:** Pengguna Plus menetapkan jumlah maksimum reels per hari, misalnya 50 reels per hari seperti target pada screenshot paywall Indonesia bertanggal 21 November 2026 dengan horizon 21 Desember. Saat limit tercapai, konsekuensi blokir dijalankan. **[FAKTA - brainpalapp.ai dan screenshot pengguna]**

**User stories:**

- Sebagai pelanggan Plus, aku ingin menetapkan limit yang realistis dan melihat sisa kuotaku sepanjang hari, agar aku bisa mengatur kecepatan scrollingku sendiri sebelum diblokir.

**Acceptance criteria:**

- Limit dapat diubah sebelum hari berjalan berakhir, dan perubahan berlaku jelas sejak waktu yang ditampilkan ke pengguna.
- Peringatan muncul mendekati limit, bukan hanya saat limit sudah habis. Ambang peringatan perlu diuji. **[REKOMENDASI]**
- Streak hari itu dihitung berdasarkan kepatuhan pada limit yang berlaku pada hari tersebut.

### 6.6 Blokir Reels dan Shorts

**Deskripsi:** Fitur Plus yang menghentikan akses ke feed Reels dan Shorts di dalam aplikasi tujuan saat limit tercapai, sementara bagian lain aplikasi yang sama tetap bekerja. Inilah janji paling khas BrainPal dibanding pemblokir aplikasi penuh. **[FAKTA - brainpalapp.ai, FAQ]**

**User stories:**

- Sebagai pelanggan Plus, aku ingin Reels diblokir saat limitku habis tetapi pesan Instagram tetap bisa kubuka, agar kendali ini tidak memutus komunikasi penting.

**Acceptance criteria:**

- Pada hitungan yang sama dengan limit, upaya membuka atau melanjutkan feed yang didukung menampilkan layar blokir BrainPal.
- Layar blokir menampilkan hitungan hari ini, limit, waktu reset, dan pilihan sah untuk membuka blokir sesuai aturan mode yang aktif.
- Fungsi non feed pada aplikasi yang sama tetap dapat diakses selama secara teknis dapat dibedakan oleh deteksi aksesibilitas.
- Blokir pulih otomatis pada pergantian hari dan hitungan kembali ke nol.

### 6.7 Breaks saat scrolling

**Deskripsi:** Jeda paksa atau pengingat istirahat di tengah sesi scrolling, tercantum sebagai bagian dari BrainPal Plus di situs resmi. **[FAKTA - brainpalapp.ai, Pricing]** Detail durasi dan pemicunya tidak dijelaskan di situs resmi, sehingga parameternya dicatat sebagai pertanyaan terbuka di bab 14.

**User stories:**

- Sebagai pengguna, aku ingin dipaksa berhenti sejenak setelah rangkaian scroll panjang, agar aku punya momen sadar untuk memutuskan lanjut atau berhenti.

**Acceptance criteria:**

- Break terpicu berdasarkan pemicu yang ditampilkan jelas di pengaturan, baik berbasis jumlah reels maupun durasi sesi.
- Selama break, feed tidak dapat dilanjutkan dan hitungan waktu jeda tampil.
- Melewati break hanya mungkin melalui mekanisme yang disepakati pada mode aktif, dan tercatat di riwayat.

### 6.8 Scroll Battles

**Deskripsi:** Fitur gratis untuk menambah teman dan membandingkan jumlah reels harian secara berdampingan. Jumlah yang lebih rendah menang pada hari itu. Contoh resmi situs menampilkan 36 melawan 93 dengan status You win today. **[FAKTA - brainpalapp.ai]**

**User stories:**

- Sebagai pengguna, aku ingin mengajak temanku bertanding jumlah reels terendah, agar menjaga limit terasa seperti permainan, bukan hukuman.
- Sebagai pengguna, aku ingin membagikan tautan undangan hanya ke orang yang kupercaya, karena menurut Privacy Policy siapa pun yang membuka tautan undangan otomatis terhubung sebagai teman dan dapat melihat nama, foto profil, dan statistik battle. **[FAKTA - Privacy Policy]**

**Acceptance criteria:**

- Battle memerlukan akun dan menampilkan nama, foto, hitungan kedua pihak, dan pemenang harian dengan aturan seri yang jelas.
- Pengguna dapat menghapus teman dan menghentikan pembagian statistiknya.
- Hitungan battle memakai total harian yang sama dengan Home, tanpa selisih akibat sinkronisasi yang tidak dijelaskan.

### 6.9 Streak

**Deskripsi:** Rangkaian hari patuh yang divisualkan sebagai api. Streak tetap menyala bila pengguna bertahan di bawah limit harian, dimulai pada hari pengguna menyelesaikan setup, dan hitungan hari ini tampil di dalam ring pada Home. Rules kini bernama Blocks. **[FAKTA - catatan rilis via AppBrain]**

**User stories:**

- Sebagai pengguna, aku ingin melihat rangkaian hariku bertahan, agar satu malam scrolling terasa sayang untuk merusaknya.

**Acceptance criteria:**

- Streak bertambah hanya setelah hari ditutup di bawah atau sama dengan limit yang berlaku.
- Streak yang putus menampilkan penyebabnya secara jujur, termasuk bila penyebabnya adalah permission yang mati, bukan kesalahan pengguna.
- Tanggal mulai streak sama dengan tanggal penyelesaian setup.

### 6.10 Blocks dan Strict mode

**Deskripsi:** Blocks adalah nama baru untuk Rules, yaitu aturan pemblokiran seperti kunci selama satu jam, kunci setiap malam, dan kunci All day. Catatan rilis menyebut pengguna kini dapat mematikan All day langsung dari lock screen bahkan saat Strict aktif, dan setiap lock menampilkan waktu mulainya. Setup Pro terbaru mengaktifkan night, limit, dan all day locks dalam beberapa ketukan. **[FAKTA - catatan rilis via AppBrain]**

**User stories:**

- Sebagai pelanggan Plus, aku ingin menjadwalkan kunci malam agar reels tertutup pada jam tidurku tanpa perlu mengaturnya setiap hari.
- Sebagai pelanggan Plus, aku ingin mode Strict mencegahku mematikan blokir secara impulsif, tetapi tetap memberi jalan keluar yang sah dan terlihat untuk keadaan darurat.

**Acceptance criteria:**

- Setiap Block menampilkan nama, cakupan aplikasi, waktu mulai, waktu selesai, dan status Strict sebelum diaktifkan.
- Perilaku mematikan All day dari lock screen saat Strict aktif bekerja persis seperti yang dijanjikan catatan rilis dan dijelaskan di layar blokir.
- Perubahan Block yang sedang berjalan tidak berlaku surut tanpa pemberitahuan.

### 6.11 Paywall dan langganan Plus

**Deskripsi:** Layar penawaran yang menjual hasil, bukan sekadar fitur. Contoh Indonesia menampilkan target 50 reels a day by 21 Dec, diskon 70 persen dengan countdown, testimoni pengguna, ilustrasi before dan after, lalu pilihan Yearly dan Monthly dengan tombol Continue dan keterangan billed annually, cancel anytime. **[FAKTA - screenshot pengguna 21 November 2026]**

**User stories:**

- Sebagai pengguna gratis, aku ingin memahami dengan tepat apa yang kubayar, berapa total yang ditagih hari ini, dan bagaimana cara membatalkannya, sebelum aku menekan Continue.
- Sebagai pelanggan, aku ingin pembelianku dikenali di semua fitur Plus dan dapat dipulihkan setelah ganti ponsel, tanpa diminta membayar lagi.

**Acceptance criteria:**

- Harga total, periode, harga per bulan, harga coret, dan syarat pembaruan otomatis tampil tanpa perlu scroll tersembunyi.
- Countdown promo tidak menyesatkan. Bila penawaran yang sama muncul lagi setelah countdown habis, hal itu harus dapat dijelaskan secara jujur. **[REKOMENDASI]**
- Tombol restore purchase mudah ditemukan dan berhasil memulihkan entitlement yang tercatat di Google Play dan RevenueCat.
- Tidak ada fitur Plus yang meminta pembayaran kedua setelah status Plus aktif. Keluhan ini muncul eksplisit di ulasan dan harus menjadi kasus uji regresi.
- Pembatalan dapat dilakukan kapan saja melalui Google Play, sesuai keterangan resmi. **[FAKTA - brainpalapp.ai]**

> **Mekanik unlock challenge, perlu verifikasi:** Kreator dan sebagian ulasan melaporkan bahwa untuk membuka Reels yang terkunci, pengguna diminta tantangan acak seperti menelungkupkan ponsel 30 detik, berjalan 50 langkah, atau melompat 10 kali. Sebagian ulasan menilai tugas ini menjadi terlalu mudah setelah update. Mekanik ini belum tercantum sebagai klaim resmi di situs atau Privacy Policy yang diakses, sehingga di PRD ini ia berstatus perlu verifikasi langsung di aplikasi dan tidak ditulis sebagai spesifikasi final.

## 7. Alur pengguna

### Alur 1: Instal sampai counter jalan

1. Pengguna menginstal BrainPal dari Google Play dan membuka aplikasi.
2. Aplikasi menjelaskan masalah dengan satuan reels dan menampilkan tahapan visual otak.
3. Pengguna menjawab pertanyaan kebiasaan seperlunya atau melewatinya, lalu memilih target atau limit harian awal.
4. Aplikasi menjelaskan Accessibility Service dengan bahasa sederhana: hanya untuk mendeteksi feed di aplikasi yang didukung, tidak membaca pesan atau isi video.
5. Pengguna diarahkan ke Pengaturan Android, mengaktifkan Accessibility Service untuk BrainPal, lalu kembali ke aplikasi.
6. Pengguna mengaktifkan izin tampil di atas aplikasi lain untuk overlay counter.
7. Pengguna diarahkan membuka Instagram, YouTube, Facebook, atau Snapchat dan scroll satu reels.
8. Counter naik dan overlay terlihat. Momen ini adalah aktivasi. Baru setelah momen ini paywall boleh tampil penuh. **[REKOMENDASI]**

### Alur 2: Mencapai limit dan terblokir

1. Pelanggan Plus scrolling seperti biasa dan counter naik menuju limit.
2. Mendekati limit, pengguna menerima peringatan sisa kuota.
3. Pada hitungan yang sama dengan limit, feed Reels atau Shorts tertutup oleh layar blokir.
4. Layar blokir menampilkan hitungan, limit, kondisi otak, waktu reset, dan aturan membuka blokir sesuai Block dan mode Strict yang aktif.
5. Pengguna beralih ke bagian lain aplikasi yang sama, misalnya pesan, yang tetap dapat dipakai, atau menutup aplikasi.
6. Pada pergantian hari, hitungan direset, streak dinilai, dan blokir harian dibuka.

### Alur 3: Membuka blokir

1. Dari layar blokir, pengguna memilih opsi membuka blokir yang tersedia untuk modenya.
2. Sistem menjelaskan konsekuensinya sebelum konfirmasi, termasuk dampaknya ke streak dan limit.
3. Bila unlock challenge aktif pada versi yang dipakai, pengguna menyelesaikan tantangan yang ditampilkan. Detail tantangan berstatus perlu verifikasi, lihat bab 6.11.
4. Setelah selesai, feed dibuka untuk durasi atau syarat yang ditampilkan, dan kejadian tercatat di riwayat.
5. Khusus Block All day, catatan rilis menyebut pembukaan dapat dilakukan dari lock screen walau Strict aktif. Perilaku ini wajib diuji apa adanya.

### Alur 4: Mengikuti Scroll Battle

1. Pengguna masuk dengan Google agar fitur sosial dan sinkronisasi aktif.
2. Pengguna membagikan tautan undangan ke teman yang dipercaya. Siapa pun yang membuka tautan otomatis terhubung sebagai teman.
3. Kedua pihak scrolling seperti biasa dan counter masing masing tersinkron.
4. Sepanjang hari keduanya dapat melihat hitungan berdampingan. Pada akhir hari, hitungan lebih rendah menang.
5. Pengguna dapat menghapus teman kapan saja untuk menghentikan pembagian statistik.

### Alur 5: Upgrade ke Plus

1. Pengguna gratis menekan fitur berbayar, misalnya menetapkan limit yang mengaktifkan blokir, atau membuka penawaran dari Home.
2. Paywall menampilkan hasil yang dijanjikan, perbandingan Free dan Plus, testimoni, dan pilihan paket.
3. Pengguna memilih Yearly atau Monthly dan membaca total tagihan serta syarat pembaruan.
4. Pembayaran diproses Google Play. Entitlement dicatat dan diverifikasi.
5. Semua fitur Plus terbuka dalam sesi yang sama tanpa meminta pembayaran ulang, dan status Plus terlihat di pengaturan akun.
6. Bila pengguna ganti perangkat, ia masuk dengan akun yang sama dan memakai restore purchase sampai status Plus kembali.

## 8. Model bisnis dan monetisasi

Modelnya adalah freemium berlangganan. Lapisan gratis membangun kebiasaan dan distribusi sosial melalui battles. Lapisan berbayar menjual kendali, yaitu kemampuan menghentikan scroll secara otomatis. Aplikasi tercatat tanpa iklan. **[FAKTA - AppBrain: No ads]**

### Perbandingan Free dan Plus

| Pembeda | Free | Plus atau Pro |
| --- | --- | --- |
| Live reel counter dan overlay | Ya | Ya |
| Brain view dan statistik harian | Ya | Ya |
| Scroll Battles dengan teman | Ya | Ya |
| Daily reel limit dengan penegakan | Tidak, atau hanya sebagai target | Ya |
| Blokir Reels dan Shorts saat limit tercapai | Tidak | Ya |
| Breaks saat scrolling | Tidak | Ya |
| Blocks terjadwal dan Strict mode | Tidak | Ya, mengikuti setup Pro |

Isi tabel mengikuti pembagian resmi di brainpalapp.ai. Penyebutan nama paket tidak sepenuhnya konsisten di sumber publik. Situs memakai Plus, listing dan catatan rilis juga memakai Pro. PRD ini memakai Plus sebagai nama utama dan mencatat Pro sebagai nama yang juga muncul. Menyatukan penamaan adalah rekomendasi perbaikan.

### Harga terverifikasi

| Pasar dan sumber | Yearly | Monthly |
| --- | --- | --- |
| India, situs resmi brainpalapp.ai | 999 rupee per tahun | 99 rupee per bulan |
| Indonesia, screenshot pengguna 21 Nov 2026 | Rp49.900 per tahun, diskon 70 persen dari Rp169.000, setara Rp4.159 per bulan | Rp16.900 per bulan |

Pembayaran di India disebut dapat melalui UPI, kartu, atau Google Play, dan langganan memperpanjang otomatis sampai dibatalkan serta dapat dibatalkan kapan saja. **[FAKTA - brainpalapp.ai]** Untuk Indonesia, pembayaran mengikuti mekanisme Google Play pada perangkat pengguna.

### Taktik promo yang teramati

- Diskon besar yang ditampilkan sebagai kupon, 70 persen pada contoh Indonesia.
- Countdown timer, contohnya Ends in 02:59:18 pada screenshot, untuk menciptakan urgensi.
- Target hasil yang dipersonalisasi di judul paywall, contohnya 50 reels a day by 21 Dec.
- Testimoni bintang lima dan ilustrasi before dan after yang membandingkan rebahan scrolling dengan aktivitas produktif di meja.
- Penekanan harga per bulan pada paket tahunan agar terlihat jauh lebih murah daripada bulanan.

> **Penilaian PRD:** Taktik di atas efektif untuk konversi jangka pendek tetapi berisiko untuk kepercayaan jangka panjang, terutama countdown yang berulang dan paywall sebelum nilai terbukti. Keluhan ulasan tentang animasi pembuka yang panjang dan dorongan langganan yang agresif menunjukkan biayanya sudah terasa. Rekomendasinya adalah mempertahankan diskon tahunan, tetapi menunda paywall penuh sampai counter pertama terbukti jalan dan menampilkan total harga dengan menonjol.

## 9. Kebutuhan non fungsional

| Area | Kebutuhan |
| --- | --- |
| Performa dan akurasi hitungan | Counter harus naik pada setiap pergantian reels di aplikasi yang didukung dengan jeda yang tidak terasa oleh pengguna. Akurasi adalah janji inti produk, sehingga tim perlu rangkaian uji regresi untuk setiap versi baru Instagram, YouTube, Facebook, dan Snapchat. Bila deteksi gagal, kegagalan harus terlihat oleh pengguna, bukan berupa angka yang diam. Angka Home, overlay, per aplikasi, battle, dan riwayat harus konsisten. |
| Baterai dan latar belakang | Layanan deteksi berjalan sepanjang hari, sehingga konsumsi baterai harus rendah dan terukur per versi Android dan per merek perangkat. Aplikasi boleh meminta pengecualian pembatasan baterai agar tetap bekerja saat ponsel terkunci atau direstart, sesuai penjelasan Privacy Policy, tetapi permintaan itu harus disertai alasan dan dampak baterainya. **[FAKTA - Privacy Policy]** |
| Keandalan permission | Status Accessibility dan overlay diperiksa setiap sesi. Bila sistem atau pengguna mencabut izin, Home menampilkan status, penyebab yang diketahui, dan langkah perbaikan dalam bahasa sederhana. Riwayat kejadian permission mati perlu dicatat secara lokal untuk membantu dukungan. Keluhan toggle yang mati sendiri adalah risiko retensi nomor satu. |
| Aksesibilitas produk | Ironis bila aplikasi yang bergantung pada Accessibility Service justru sulit dipakai. Semua tahap otak memiliki label teks, kontras overlay terbaca di atas video terang dan gelap, target sentuh cukup besar, dan layar blokir dapat dibaca screen reader sejauh dimungkinkan Android. |
| Keamanan dan privasi | Deteksi hanya berjalan untuk daftar paket aplikasi yang didukung dan terpasang tetap di dalam aplikasi. Daftar resmi saat ini adalah Instagram dan Instagram Lite, YouTube, Snapchat dan Snapchat Lite, serta Facebook dan Facebook Lite. Data yang keluar perangkat dibatasi pada statistik turunan seperti jumlah reels, jumlah per aplikasi, dan catatan battle atau sinkronisasi. Isi layar, pesan, teks ketikan, kata sandi, OTP, tangkapan layar, dan riwayat browsing tidak dikirim. Aplikasi tidak berjalan untuk aplikasi perbankan dan pembayaran. **[FAKTA - Privacy Policy, bagian Accessibility]** |
| Data dan akun | Penggunaan tanpa akun tetap dimungkinkan untuk fungsi dasar. Akun Google hanya wajib untuk battles, koneksi teman, backup riwayat, dan sinkronisasi antar perangkat. Data akun dan hitungan disimpan selama akun aktif dan dihapus saat akun dihapus. Laporan crash dan log server disimpan sampai 90 hari menurut Privacy Policy. **[FAKTA - Privacy Policy]** |
| Keandalan langganan | Status entitlement harus menjadi satu sumber kebenaran antara Google Play, RevenueCat, dan aplikasi. Restore harus berhasil tanpa dukungan manual pada kasus normal. Tidak boleh ada layar fitur Plus yang meminta pembayaran kedua saat status sudah Plus. |
| Kompatibilitas | Ukuran aplikasi 38,7 MB dan rating konten Everyone pada versi 23.1.698. **[FAKTA - AppBrain]** Matriks uji wajib mencakup perbedaan perilaku latar belakang antar merek Android populer di Indonesia dan India, dua pasar harga yang terverifikasi di dokumen ini. |

Catatan istilah: Uji regresi adalah pengujian yang diulang setiap ada perubahan aplikasi tujuan atau versi BrainPal, agar perbaikan di satu aplikasi tidak merusak deteksi di aplikasi lain.

## 10. Analisis kompetitor

Tabel ini membandingkan cara kerja, bukan sekadar daftar fitur. Data BrainPal, one sec, Opal, ScreenZen, ScrollGuard, dan Doomscrol mengikuti perbandingan yang dicek dari situs dan listing resmi masing masing pada 16 November 2026 oleh Doomscrol, dilengkapi situs resmi BrainPal. Baris Scroll Block, StopScroll, dan ScrollLight diringkas pada tingkat kategori dan ditandai sebagai ringkasan penulis PRD karena detail harga dan platformnya berubah cepat dan perlu pengecekan ulang sebelum dipakai untuk keputusan bisnis.

| Produk | Cara kerja inti | Platform | Harga | Relatif terhadap BrainPal |
| --- | --- | --- | --- | --- |
| BrainPal | Menghitung reels per scroll secara live, visual otak, battles, blokir feed saat limit | Android | Free, Plus berbayar | Paling spesifik pada satuan reels. Kelemahannya adalah Android saja dan bergantung penuh pada Accessibility. |
| one sec | Jeda dan friksi sebelum membuka aplikasi atau fitur, mendorong niat sadar | Android, iOS, ekstensi browser | Free untuk satu aplikasi, Pro untuk tanpa batas | Lebih luas platformnya dan lebih lembut pendekatannya. Tidak berpusat pada hitungan reels live seperti BrainPal. |
| Opal | Sesi fokus, jadwal, dan batas harian dengan Rules dan pemblokiran aplikasi | Android, iOS, iPad, Mac | Free 3 Rules, PRO berbayar | Lebih matang untuk kerja fokus lintas perangkat. Kurang spesifik pada perilaku di dalam feed video pendek. |
| ScreenZen | Jeda, batas waktu, dan pemblokiran opsional pada limit harian | Android, iOS, macOS, Windows | Semua fitur gratis, berbasis donasi | Ancaman harga terkuat karena gratis penuh. BrainPal harus menang lewat akurasi counting dan battles, bukan lewat blokir saja. |
| ScrollGuard | Menghilangkan Reels dan Shorts sambil mempertahankan fungsi lain aplikasi | Android, iPhone via web app | Inti gratis, premium untuk kontrol per aplikasi | Pesaing langsung pada janji blokir feed saja. BrainPal membedakan diri lewat counter, visual otak, dan battles. |
| Doomscrol | Menghitung reels Instagram dan postingan feed, limit per sesi dan per hari, bertanya saat limit bukan mengunci | Android | Gratis selama early access | Paling mirip pada counting. Pendekatannya lebih lunak karena bertanya, bukan memblokir, dan cakupan awalnya lebih sempit. |
| Scroll Block | Pemblokir feed dan pengingat batas scroll pada tingkat kategori | Android | Freemium, perlu cek ulang | Ringkasan kategori penulis PRD. Umumnya bersaing pada blokir, bukan pada visualisasi kondisi dan kompetisi sosial. |
| StopScroll | Pembatas waktu dan sesi untuk aplikasi video pendek pada tingkat kategori | Android | Freemium, perlu cek ulang | Ringkasan kategori penulis PRD. Diferensiasi BrainPal tetap pada hitungan per reels yang tampil live di layar. |
| ScrollLight | Pengingat ringan dan batas pemakaian pada tingkat kategori | Android | Freemium, perlu cek ulang | Ringkasan kategori penulis PRD. Posisi ringannya dekat dengan pengguna yang menolak blokir keras, segmen yang dilayani BrainPal gratis. |

> **Pelajaran kompetitif:** Blokir sudah menjadi komoditas dan bahkan tersedia gratis. Yang belum menjadi komoditas adalah kombinasi hitungan per reels yang dipercaya, visual yang diingat pengguna, dan kompetisi harian dengan teman. Investasi produk sebaiknya mempertahankan tiga hal itu.

## 11. Bukti pasar dan suara pengguna

### Data pasar terverifikasi

| Indikator | Angka |
| --- | --- |
| Tanggal rilis | Maret 2026 **[FAKTA - AppBrain]** |
| Versi terbaru dan tanggal update | 23.1.698, 19 November 2026 |
| Rating dan jumlah ulasan | 4,36 dari 129.627 ulasan |
| Total install | 2,6 juta |
| Install 30 hari terakhir | Sekitar 500 ribu, rata rata sekitar 17 ribu per hari |
| Peringkat Indonesia | Nomor 78 Top Free Productivity dan nomor 37 Top Grossing Productivity |
| Ukuran, iklan, rating konten | 38,7 MB, tanpa iklan, Everyone |
| Developer dan alamat | EpowerX Labs Private Limited, Whitefield, Bangalore, Karnataka, India, sesuai situs resmi |

Kombinasi 500 ribu install sebulan dan peringkat grossing nomor 37 di Indonesia menunjukkan dua hal sekaligus. Permintaan terhadap solusi anti scrolling itu nyata dan sebagian pengguna bersedia membayar. Namun rating 4,36, bukan 4,7 atau 4,8, menunjukkan pengalaman yang belum konsisten.

### Ringkasan ulasan positif

- Pengguna merasa terbantu karena angkanya terlihat live dan sulit diabaikan, berbeda dari laporan screen time yang datang belakangan.
- Testimoni yang dikutip situs resmi menyatakan penurunan dari 1000 lebih reels per hari menjadi sekitar 30 dalam dua minggu.
- Ulasan dan testimoni lain yang diringkas dalam arahan riset menyebut penurunan dari kisaran 600 sampai 700 menjadi 50 reels per hari.
- Scroll Battles dipuji sebagai cara yang membuat usaha mengurangi scrolling terasa seperti permainan bersama teman.

Testimoni adalah klaim individu yang dikutip pemasar atau penulis ulasan. Testimoni bukan hasil uji klinis dan bukan rata rata pengguna. Di dokumen ini testimoni dipakai sebagai sinyal kepuasan, bukan sebagai janji hasil.

### Ringkasan ulasan negatif

- **Onboarding panjang dan agresif.** Animasi pembuka yang lama dan dorongan berlangganan yang terus muncul sebelum pengguna membuktikan manfaatnya.
- **Survei awal yang sensitif.** Pertanyaan tentang penghasilan pada survei awal dinilai tidak relevan dan menimbulkan curiga.
- **Permission mati sendiri.** Toggle izin kadang nonaktif sendiri sehingga tidak ada reels yang terhitung sama sekali.
- **Counter berhenti setelah beberapa jam.** Sebagian pengguna melaporkan aplikasi masuk mode tidur setelah 3 sampai 4 jam dan berhenti mendeteksi, memblokir, dan memberi notifikasi, serta meminta izin berulang kali.
- **Restore dan pembayaran ganda.** Pengguna yang sudah membeli Pro melaporkan fitur blokir situs meminta pembelian lagi dan tombol restore sulit ditemukan.
- **Unlock task terlalu mudah.** Setelah update, sebagian pengguna menilai tantangan membuka kunci terlalu gampang sehingga friksinya hilang.
- **Kekhawatiran privasi izin.** Sebagian pengguna menghapus aplikasi setelah menyadari luasnya kuasa izin aksesibilitas, meski sudah membayar setahun.

> **Pola yang harus dibaca tim produk:** Keluhan negatif jarang menyerang ide BrainPal. Keluhan menyerang pelaksanaan, yaitu onboarding, keandalan izin, dan keadilan pembayaran. Itu kabar baik, karena ketiganya dapat diperbaiki tanpa mengubah konsep produk.

## 12. Risiko, keterbatasan, dan ketergantungan

| Risiko | Dampak dan mitigasi |
| --- | --- |
| Ketergantungan pada Accessibility API | Tidak ada cara lain di Android untuk mendeteksi reels di dalam aplikasi lain menurut Privacy Policy resmi. Perubahan kebijakan Google atau pembatasan aksesibilitas dapat mematikan fungsi inti. Mitigasinya adalah kepatuhan ketat pada penjelasan penggunaan, deteksi hanya untuk daftar aplikasi yang didukung, dan komunikasi transparan ke pengguna. |
| Perubahan UI aplikasi pihak ketiga | Instagram, YouTube, Facebook, dan Snapchat dapat mengubah struktur layar kapan saja sehingga view ID dan deskripsi konten yang dibaca deteksi berubah dan hitungan meleset. Mitigasinya adalah pemantauan versi, uji regresi per aplikasi, dan rilis perbaikan cepat. |
| Permission dicabut sistem atau pengguna | Penghemat baterai antar merek Android dapat mematikan layanan latar belakang. Counter berhenti diam diam dan pengguna menyimpulkan aplikasi rusak. Mitigasinya adalah indikator status yang jujur, pemberitahuan saat deteksi berhenti, dan panduan per merek perangkat. |
| Persepsi paywall agresif | Countdown, diskon besar, dan paywall dini dapat menaikkan konversi sesaat tetapi menurunkan rating dan memicu refund. Mitigasinya adalah aktivasi dulu sebelum paywall penuh, total harga yang menonjol, dan restore yang selalu bekerja. |
| Ketakutan privasi | Izin aksesibilitas secara teknis sangat kuat. Satu pemberitaan buruk dapat merusak kepercayaan seluruh kategori. Mitigasinya adalah daftar aplikasi yang terpasang tetap, janji yang spesifik tentang data yang tidak diambil, dan audit yang dapat dijelaskan dengan bahasa sederhana. |
| Perilaku berbalik menjadi kompetisi negatif | Battles dapat disalahartikan sebagai lomba jumlah tertinggi oleh sebagian kelompok pengguna, seperti terlihat pada komentar kreator. Mitigasinya adalah desain yang selalu merayakan jumlah terendah dan tidak memberi penghargaan untuk jumlah tinggi. |
| Klaim kesehatan yang berlebihan | Metafora otak yang membusuk mudah dibaca sebagai klaim medis. Terms of Use resmi menegaskan BrainPal adalah alat perilaku dan produktivitas, bukan saran medis atau kesehatan mental. Semua salinan harus menjaga batas ini. |
| Android saja | Seluruh pengguna iPhone tertutup, termasuk banyak calon pengguna di segmen pelajar dan kreator. Ini keterbatasan pasar yang disadari dan menjadi bahan roadmap, bukan janji saat ini. |

## 13. Roadmap yang diusulkan

Roadmap ini adalah **[REKOMENDASI]** penulis PRD. Ini bukan roadmap resmi EpowerX Labs.

### Fase 0: Produk yang sudah ada (MVP terbukti di pasar)

- Counter live, visual otak, statistik harian, battles, streak, limit, blokir, breaks, Blocks, dan langganan Plus di Android.
- Bukti pasar berupa 2,6 juta install dan monetisasi yang berjalan di India dan Indonesia.

### Fase 1: Perbaikan keandalan dan kepercayaan

- Indikator kesehatan deteksi yang selalu terlihat, pemberitahuan saat counter berhenti, dan panduan perbaikan izin per merek perangkat.
- Paywall setelah aktivasi pertama, total harga yang menonjol, satu nama paket yang konsisten antara Plus dan Pro, dan restore satu ketukan yang diuji sebagai kasus regresi.
- Memangkas survei onboarding, menghapus pertanyaan sensitif yang tidak dipakai untuk personalisasi nyata, dan memberi jalan lewati yang jelas.
- Uji regresi deteksi untuk setiap pembaruan besar aplikasi yang didukung.

### Fase 2: Kedalaman hasil pengguna

- Laporan mingguan yang membandingkan median reels, hari patuh, dan aplikasi penyumbang terbesar, dengan saran limit minggu berikutnya.
- Kalibrasi unlock challenge agar friksinya terasa nyata tetapi aman, termasuk menolak tantangan fisik yang berisiko dan menyediakan alternatif non fisik.
- Mode battle yang lebih kaya, misalnya liga mingguan kecil dan target tim, tanpa memberi penghargaan untuk jumlah reels tinggi.
- Eksperimen ambang peringatan menuju limit dan waktu break yang paling menurunkan reels tanpa menaikkan uninstall.

### Fase 3: Ekspansi

- iOS dengan pendekatan yang jujur terhadap batasan sistem Apple. Pengalaman iOS kemungkinan tidak dapat menyamai deteksi aksesibilitas Android, sehingga janji produknya harus ditulis ulang, bukan disalin.
- Dukungan platform tambahan yang disebut sebagai kemungkinan masa depan di Privacy Policy, seperti TikTok atau LinkedIn, hanya setelah akurasi deteksinya terbukti dan melalui rilis Play Store baru sesuai mekanisme yang dijelaskan kebijakan tersebut.
- Mode keluarga untuk persona orang tua dan pendamping, dengan persetujuan anak yang jelas, laporan ringkas, dan tanpa pembacaan isi tontonan.
- Lokalisasi harga dan salinan paywall yang lebih rapi untuk pasar Indonesia, termasuk penjelasan pembatalan dalam Bahasa Indonesia.

> **Urutan yang disengaja:** Fase keandalan mendahului fase ekspansi. Membawa counter yang kadang mati ke iOS atau ke TikTok hanya memperluas masalah yang sama ke lebih banyak pengguna.

## 14. Pertanyaan terbuka dan asumsi

### Pertanyaan terbuka, belum terverifikasi

- Apakah unlock challenge berupa menelungkupkan ponsel 30 detik, berjalan 50 langkah, dan melompat 10 kali benar benar bagian dari versi 23.1.698, dan bagaimana aturan lengkapnya. Perlu verifikasi langsung di aplikasi.
- Apa pemicu dan durasi pasti dari breaks saat scrolling. Situs resmi hanya mencantumkan namanya sebagai fitur Plus.
- Bagaimana aturan deduplikasi hitungan saat pengguna kembali ke reels sebelumnya, memutar ulang, atau menonton sebagian.
- Apakah ada masa uji coba gratis Plus, berapa lama, dan apakah memerlukan metode pembayaran di muka. Screenshot Indonesia tidak membuktikan hal ini.
- Mengapa nama paket berbeda antara Plus di situs dan Pro di listing serta catatan rilis, dan nama mana yang akan dipertahankan.
- Berapa retensi dan konversi Plus yang sebenarnya. Angka ini tidak dipublikasikan dan tidak ditebak di dokumen ini.
- Bagaimana tepatnya blokir membedakan feed dan bagian lain aplikasi pada setiap aplikasi tujuan, dan apa batas kegagalannya.
- Apakah countdown promo direset untuk pengguna yang sama dan apakah harga diskon yang sama tersedia setelah countdown berakhir.
- Berapa konsumsi baterai rata rata per hari pada perangkat kelas menengah yang umum di Indonesia.
- Apakah dukungan TikTok sedang dikerjakan. Privacy Policy hanya menyebutnya sebagai kemungkinan penambahan di masa depan.

### Asumsi penulis PRD

- **[ASUMSI]** Persona dan urutan prioritas roadmap adalah rekonstruksi penulis berdasarkan positioning, harga, dan pola ulasan, bukan dokumen internal EpowerX Labs.
- **[ASUMSI]** Target limit 50 reels per hari pada screenshot dianggap contoh personalisasi paywall, bukan limit bawaan untuk semua pengguna.
- **[ASUMSI]** Ringkasan Scroll Block, StopScroll, dan ScrollLight pada tabel kompetitor berada pada tingkat kategori dan perlu pengecekan ulang sebelum dipakai untuk keputusan investasi.
- **[ASUMSI]** Peringatan mendekati limit dan laporan mingguan adalah rekomendasi, bukan fitur yang diklaim sudah ada.

## 15. Lampiran sumber

Semua sumber diakses pada 21 November 2026, kecuali perbandingan kompetitor yang merujuk pengecekan sumber resmi masing masing produk pada 16 November 2026 sebagaimana dicantumkan sumbernya.

- Situs resmi BrainPal, brainpalapp.ai, halaman utama, pricing, dan FAQ. URL: https://www.brainpalapp.ai/
- Listing Google Play BrainPal: See Your Reel Count melalui AppBrain, termasuk rating, install, versi 23.1.698, ukuran, peringkat, dan catatan rilis. URL: https://www.appbrain.com/app/brainpal-see-your-reel-count/com.brainrot.android
- Privacy Policy resmi BrainPal, EpowerX Labs Private Limited, tentang data yang dikumpulkan, Accessibility Service, daftar aplikasi yang didukung, penyedia layanan, retensi, dan hak penghapusan. URL: https://api.brainrotapp.ai/legal/privacy-policy
- Terms of Use resmi BrainPal, tentang definisi layanan, izin perangkat, dan batas sebagai alat perilaku bukan saran medis. URL: https://api.brainrotapp.ai/legal/terms-of-use
- Google Play, halaman aplikasi BrainPal: See Your Reel Count, package com.brainrot.android, termasuk contoh ulasan pengguna. URL: http://play.google.com/store/apps/details?id=com.brainrot.android
- Perbandingan alternatif one sec dan Opal untuk Android, Doomscrol, dicek 16 November 2026. URL: https://doomscrol.com/one-sec-opal-alternative-android
- Screenshot paywall Indonesia dari pengguna, 21 November 2026, menampilkan target 50 reels a day by 21 Dec, diskon 70 persen, countdown 02:59:18, Yearly Rp49.900 dari Rp169.000 setara Rp4.159 per bulan, dan Monthly Rp16.900 per bulan.
- Halaman unduhan BrainPal di Uptodown sebagai pembanding deskripsi kategori digital wellbeing. URL: https://brainpal.en.uptodown.com/android

> **Penutup:** BrainPal menang karena membuat angka yang selama ini tidak terlihat menjadi terlihat pada momen yang tepat. Tugas produk berikutnya bukan membuat angka itu lebih menakutkan, melainkan membuat angka itu selalu benar, selalu jujur, dan cukup dipercaya untuk dibayar. Dokumen ini disusun dari riset publik atas permintaan Nico pada 21 November 2026 dan berstatus reverse engineering PRD, bukan dokumen resmi EpowerX Labs Private Limited.

## 16. Lampiran: Memakai PRD ini di Google AI Studio

Lampiran ini untuk kamu yang mau membangun versi sendiri dari PRD ini di Google AI Studio tanpa berlangganan BrainPal Plus. Isinya praktis: prompt pertama, urutan bangun, dan batasan teknis yang perlu kamu tahu dari awal.

### 16.1 Contoh prompt pertama yang siap copy paste

Salin prompt di bawah ini, tempel sebagai pesan pertama di Google AI Studio, lalu lampirkan atau tempel juga isi PRD ini sebagai konteks. Prompt ini sengaja dimulai dari counter dan visual otak karena dua fitur itu adalah inti produk di bab 6.2 dan 6.3.

```text
Kamu adalah developer yang membantuku membangun prototipe aplikasi dari PRD yang kulampirkan: BrainPal, aplikasi penghitung reels.

Tugas pertamamu: bangun prototipe Tahap 1 saja, yaitu Live Reel Counter dan Brain State Visual, sesuai PRD bab 6.2 dan 6.3.

Ikuti user stories dan acceptance criteria di bab tersebut secara harfiah:
- Counter mulai dari 0 dan naik tepat 1 setiap kali simulasi scroll terjadi.
- Tampilkan counter besar di Home di dalam ring, lengkap dengan total hari ini dan rincian per aplikasi (Instagram, YouTube, Facebook, Snapchat) sebagai data simulasi.
- Brain State Visual harus berubah pada ambang yang sama seperti PRD: sehat di 0, mulai lelah di 50, retak di 100, layu abu abu di 150, hilang di 200 atau lebih. Setiap tahap wajib punya label teks, bukan hanya gambar atau warna.
- Tambahkan tombol simulasi yang jelas: tombol "+1 Reels" untuk menaikkan counter secara manual, tombol reset harian, dan pilihan aplikasi sumber. Ini simulasi, jangan berpura pura mendeteksi scroll asli dari Instagram atau YouTube.
- Simpan hitungan hari ini di local storage agar tidak hilang saat halaman dimuat ulang, dan reset otomatis berdasarkan tanggal perangkat.
- Desain mobile first, tema gelap seperti BrainPal, satu halaman utama yang rapi: ring counter di atas, visual otak di tengah, rincian per aplikasi dan tombol simulasi di bawah.

Jangan bangun dulu fitur limit, blokir, battle, streak, atau paywall. Itu tahap berikutnya.

Setelah selesai, jelaskan file apa saja yang kamu buat, cara menjalankan prototipenya, dan acceptance criteria mana saja dari bab 6.2 dan 6.3 yang sudah terpenuhi dan yang belum.
```

Setelah prototipe Tahap 1 jalan, kamu bisa lanjut dengan prompt pendek seperti: "Lanjut ke Tahap 2 sesuai PRD bab 6.4 dan 6.8, tambahkan statistik harian dan Scroll Battles simulasi, tetap ikuti acceptance criteria di bab tersebut."

### 16.2 Saran urutan pembangunan bertahap

Bangun bertahap. Jangan minta semua fitur sekaligus, hasilnya biasanya berantakan dan sulit dicek.

- **Tahap 1: Prototipe UI dan counter manual.** Fokus ke Home, ring counter, visual otak dengan 5 tahap, tombol simulasi +1, reset, dan penyimpanan lokal. Selesai tahap ini kamu sudah bisa merasakan ide inti BrainPal: angka naik, otak memburuk. Acuan: bab 6.2 dan 6.3.

- **Tahap 2: Statistik dan battle.** Tambahkan statistik harian dan riwayat 7 hari, rincian per aplikasi, streak sederhana, dan Scroll Battles simulasi. Untuk battle versi web, lawan bisa berupa data dummy atau teman yang memakai perangkat yang sama dulu, atau input manual hitungan teman. Acuan: bab 6.4, 6.8, dan 6.9.

- **Tahap 3: Limit dan blokir.** Tambahkan daily reel limit yang bisa diatur pengguna, peringatan saat mendekati limit, layar blokir simulasi saat limit tercapai, breaks timer, dan Blocks terjadwal versi sederhana. Di web, blokir berarti menampilkan overlay penuh di dalam prototipe itu sendiri, bukan memblokir aplikasi lain. Acuan: bab 6.5, 6.6, 6.7, dan 6.10.

- **Tahap 4 (opsional): Paywall simulasi.** Kalau mau meniru model bisnisnya, tambahkan halaman perbandingan Free vs Plus dan paywall simulasi seperti di bab 6.11 dan bab 8, tanpa pembayaran asli. Ini berguna untuk belajar alur monetisasi, bukan untuk menagih pengguna.

Di setiap tahap, minta AI Studio menyebutkan acceptance criteria mana yang sudah lolos. Itu cara paling cepat mengecek apakah hasilnya sesuai PRD atau hanya terlihat mirip.

### 16.3 Catatan batasan teknis: web vs Android native

Ini batasan teknis, bukan larangan.

Google AI Studio menghasilkan aplikasi web yang berjalan di browser. BrainPal asli adalah aplikasi Android native yang menghitung reels asli dengan Accessibility Service API Android. API itu yang memungkinkan BrainPal mendeteksi pergantian reels di dalam Instagram, YouTube, Facebook, dan Snapchat, lalu menampilkan overlay di atas aplikasi lain.

Aplikasi web tidak bisa mengakses Accessibility Service API Android, tidak bisa membaca scroll di dalam aplikasi lain, dan tidak bisa menampilkan overlay sistem di atas Instagram atau YouTube. Karena itu prototipe web dari PRD ini hanya bisa mensimulasikan hitungan, misalnya lewat tombol +1, geser simulasi di dalam halaman prototipe sendiri, atau timer sesi.

Artinya:

- Prototipe web cocok untuk memvalidasi UI, visual otak, statistik, battle, dan alur limit. Untuk tujuan belajar dan membuktikan konsep, itu sudah cukup.
- Versi yang benar benar menghitung reels asli di Instagram dan YouTube, dengan overlay live dan blokir feed saat limit tercapai, membutuhkan aplikasi Android native, misalnya dibangun dengan Kotlin dan Accessibility Service, di luar hasil AI Studio versi web.
- Kalau nanti kamu mau lanjut ke versi native, PRD ini tetap bisa dipakai apa adanya. Bab 6, 7, dan 9 adalah spesifikasi yang sama, hanya implementasi deteksinya yang pindah dari simulasi tombol ke Accessibility Service.

Singkatnya: pakai AI Studio untuk membangun dan menguji idenya dengan cepat dan gratis. Simpan ekspektasi bahwa penghitung aslinya adalah pekerjaan Android native tahap berikutnya.
