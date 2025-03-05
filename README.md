# cDuello

Minecraft sunucuları için Paper 1.21.4 üzerinde çalışan basit ama güçlü bir düello eklentisi.

## Özellikler

- Diğer oyunculara düello daveti gönderme
- Zaman aşımı ile otomatik düello yönetimi
- Özelleştirilebilir düello ayarları (envanteri koruma, düello sonrası iyileştirme vb.)
- İstatistik takibi (galibiyetler, mağlubiyetler, galibiyet oranı)
- Özelleştirilebilir mesajlar
- İzin tabanlı erişim kontrolü

## Yeni Özellikler

### SQLite Veri Depolama
- Oyuncu istatistiklerini SQLite veritabanında saklar
- Veri kaybını önlemek için düzenli otomatik kaydetme
- Etkili ve hızlı veri erişimi

### Sıralama Sistemi
- `/duello sıralama` komutu ile oyuncu sıralamalarını görüntüleme
- Oyuncu kafaları ile görsel sıralama menüsü
- En çok galibiyet, en çok para kazanan ve en iyi oran kriterlerine göre sıralama

### PlaceholderAPI Entegrasyonu
- `%duello_siralama_(numara)%` formatında yer tutucular
- Oyuncu istatistikleri için yer tutucular:
  - `%duello_wins%` - Galibiyet sayısı
  - `%duello_losses%` - Mağlubiyet sayısı
  - `%duello_total_duels%` - Toplam düello sayısı
  - `%duello_win_ratio%` - Kazanma oranı
  - `%duello_money_won%` - Kazanılan para
  - `%duello_money_lost%` - Kaybedilen para

### İstatistik Sistemi Geliştirmeleri
- Gerçek zamanlı istatistik takibi
- Para kazancı ve kaybı istatistikleri
- Kazanma oranı hesaplamaları

### Admin Komutları
- `/dueladmin reload` - Konfigürasyonu yeniden yükler
- `/dueladmin arena` - Arena yönetim komutları
- `/dueladmin stats` - İstatistik yönetim komutları

## Kurulum

1. Eklentinin en son sürümünü sürümler bölümünden indirin.
2. JAR dosyasını sunucunuzun `plugins` dizinine yerleştirin.
3. Sunucunuzu yeniden başlatın.
4. `plugins/cDuello` dizinindeki `config.yml` dosyasını düzenleyerek eklentiyi yapılandırın.

## Komutlar

- `/duel <oyuncu>` - Bir oyuncuya düello daveti gönder
- `/duel accept` - Bekleyen bir düello davetini kabul et
- `/duel deny` - Bekleyen bir düello davetini reddet
- `/duel stats [oyuncu]` - Kendi veya başka bir oyuncunun düello istatistiklerini görüntüle
- `/duel reload` - Eklenti yapılandırmasını yeniden yükle (izin gerektirir: `cduello.admin`)

## İzinler

- `cduello.use` - Oyuncunun düello işlevselliğini kullanmasına izin verir (varsayılan: true)
- `cduello.admin` - Oyuncunun yönetici komutlarını kullanmasına izin verir (varsayılan: op)

## Yapılandırma

Eklenti, `config.yml` dosyası aracılığıyla oldukça özelleştirilebilir. İşte bazı önemli yapılandırma seçenekleri:

- `duels.request-timeout` - Bir düello davetinin süresi dolmadan önceki süre (saniye cinsinden)
- `duels.countdown` - Düello başlamadan önceki geri sayım süresi (saniye cinsinden)
- `duels.teleport-back` - Düello sonrası oyuncuları orijinal konumlarına geri ışınlayıp ışınlamama
- `duels.heal-after-duel` - Düello sonrası oyuncuları iyileştirip iyileştirmeme
- `duels.clear-effects` - Düello sonrası efektleri temizleyip temizlememe
- `duels.keep-inventory` - Düello sırasında envanteri koruyup korumama
- `duels.allowed-commands` - Düello sırasında izin verilen komutlar listesi

## Kurulum Talimatları

### IntelliJ IDEA
1. IntelliJ IDEA'yı açın
2. "Open" seçeneğini seçin ve proje klasörüne gidin
3. IntelliJ, bunu otomatik olarak bir Maven projesi olarak tanımalıdır
4. Değilse, `pom.xml` dosyasına sağ tıklayın ve "Add as Maven Project" seçeneğini seçin
5. Maven'in tüm bağımlılıkları indirmesini bekleyin
6. Hala içe aktarma hataları görüyorsanız, Dosya > Önbellekleri Temizle / Yeniden Başlat'a gidin

### Eclipse
1. Eclipse'i açın
2. Dosya > İçe Aktar > Maven > Mevcut Maven Projeleri'ni seçin
3. Proje klasörüne gidin ve seçin
4. Eclipse, proje yapısını otomatik olarak tanımalıdır
5. Hala içe aktarma hataları görüyorsanız, projeye sağ tıklayın > Maven > Projeyi Güncelle

### VS Code
1. VS Code'u açın
2. Proje klasörünü açın
3. "Java için Uzantı Paketi" ve "Java için Maven" uzantılarını yükleyin (eğer yüklü değilse)
4. VS Code, projeyi otomatik olarak bir Maven projesi olarak tanımalıdır
5. Hala içe aktarma hataları görüyorsanız, Komut Paleti'ni açın (Ctrl+Shift+P), "Java: Java Dil Sunucusu Çalışma Alanını Temizle" yazın ve "Yeniden Başlat" seçeneğini seçin

### Manuel Kurulum (IDE entegrasyonu başarısız olursa)
1. Maven'in yüklü olduğundan emin olun
2. Proje dizininde bir terminal açın
3. `mvn clean install` komutunu çalıştırın
4. Paper API jar dosyasını (libs/paper-api-1.21.4-R0.1-SNAPSHOT.jar konumunda) projenizin classpath'ine manuel olarak ekleyin

## Derleme

Kaynak koddan eklentiyi derlemek için:

1. Depoyu klonlayın
2. `mvn clean package` komutunu çalıştırın
3. Derlenmiş JAR dosyası `target` dizininde olacaktır

## Gereksinimler

- Paper 1.21.4
- Java 8 veya üstü

## Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.

## Destek

Herhangi bir hata ile karşılaşırsanız veya önerileriniz varsa, lütfen [GitHub deposunda](https://github.com/theelytra/cDuello) bir sorun oluşturun.

## Katkılar

- itscactusdev tarafından oluşturulmuştur

## Bağımlılıklar
- Java 21
- Paper API 1.21.4-R0.1-SNAPSHOT 