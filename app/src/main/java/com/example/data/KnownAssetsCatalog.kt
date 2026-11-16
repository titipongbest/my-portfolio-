package com.example.data

import com.example.model.AssetCategory
import com.example.model.StockSector

data class KnownAsset(
    val symbol: String,
    val displayName: String,
    val category: AssetCategory,
    val sector: StockSector = StockSector.NONE,
    val defaultCurrency: String = category.defaultCurrency,
    val yahooTicker: String? = null,
    val coinGeckoId: String? = null
)

object KnownAssetsCatalog {
    val allAssets: List<KnownAsset> = listOf(
        // หุ้นไทย - พลังงานและสาธารณูปโภค
        KnownAsset("PTT", "บมจ. ปตท.", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "PTT.BK"),
        KnownAsset("PTTEP", "ปตท. สำรวจและผลิตปิโตรเลียม", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "PTTEP.BK"),
        KnownAsset("PTTGC", "พีทีที โกลบอล เคมิคอล", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "PTTGC.BK"),
        KnownAsset("GULF", "กัลฟ์ เอ็นเนอร์จี ดีเวลลอปเมนท์", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "GULF.BK"),
        KnownAsset("GPSC", "โกลบอล เพาเวอร์ ซินเนอร์ยี่", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "GPSC.BK"),
        KnownAsset("TOP", "ไทยออยล์", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "TOP.BK"),
        KnownAsset("OR", "ปตท. น้ำมันและการค้าปลีก", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "OR.BK"),
        KnownAsset("BCP", "บางจาก คอร์ปอเรชั่น", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "BCP.BK"),
        KnownAsset("BGRIM", "บี.กริม เพาเวอร์", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "BGRIM.BK"),
        KnownAsset("BANPU", "บ้านปู", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "BANPU.BK"),
        KnownAsset("EGCO", "ผลิตไฟฟ้า (EGCO)", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "EGCO.BK"),
        KnownAsset("RATCH", "ราช กรุ๊ป", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "RATCH.BK"),
        KnownAsset("EA", "พลังงานบริสุทธิ์", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "EA.BK"),
        KnownAsset("CKP", "ซีเค พาวเวอร์", AssetCategory.THAI_STOCK, StockSector.ENERGY, "THB", "CKP.BK"),

        // หุ้นไทย - อสังหาริมทรัพย์และที่อยู่อาศัย
        KnownAsset("LH", "แลนด์แอนด์เฮ้าส์", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "LH.BK"),
        KnownAsset("AP", "เอพี (ไทยแลนด์)", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "AP.BK"),
        KnownAsset("SPALI", "ศุภาลัย", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "SPALI.BK"),
        KnownAsset("SIRI", "แสนสิริ", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "SIRI.BK"),
        KnownAsset("CPN", "เซ็นทรัลพัฒนา", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "CPN.BK"),
        KnownAsset("WHA", "ดับบลิวเอชเอ คอร์ปอเรชั่น", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "WHA.BK"),
        KnownAsset("AMATA", "อมตะ คอร์ปอเรชัน", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "AMATA.BK"),
        KnownAsset("ORI", "ออริจิ้น พร็อพเพอร์ตี้", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "ORI.BK"),
        KnownAsset("QH", "ควอลิตี้เฮ้าส์", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "QH.BK"),
        KnownAsset("PSH", "พฤกษา โฮลดิ้ง", AssetCategory.THAI_STOCK, StockSector.PROPERTY, "THB", "PSH.BK"),

        // หุ้นไทย - โครงสร้างพื้นฐานและขนส่ง
        KnownAsset("AOT", "ท่าอากาศยานไทย", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "AOT.BK"),
        KnownAsset("BEM", "ทางด่วนและรถไฟฟ้ากรุงเทพ", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "BEM.BK"),
        KnownAsset("BTS", "บีทีเอส กรุ๊ป โฮลดิ้งส์", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "BTS.BK"),
        KnownAsset("CK", "ช.การช่าง", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "CK.BK"),
        KnownAsset("STEC", "ซิโน-ไทย เอ็นจีเนียริ่ง", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "STEC.BK"),
        KnownAsset("DIF", "กองทุนรวมโครงสร้างพื้นฐานโทรคมนาคม", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "DIF.BK"),
        KnownAsset("3BBIF", "กองทุนฯ บรอดแบนด์อินเทอร์เน็ต", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "3BBIF.BK"),
        KnownAsset("SCC", "ปูนซิเมนต์ไทย", AssetCategory.THAI_STOCK, StockSector.INFRASTRUCTURE, "THB", "SCC.BK"),
        KnownAsset("SCGP", "เอสซีจี แพคเกจจิ้ง", AssetCategory.THAI_STOCK, StockSector.OTHER, "THB", "SCGP.BK"),
        KnownAsset("IVL", "อินโดรามา เวนเจอร์ส", AssetCategory.THAI_STOCK, StockSector.OTHER, "THB", "IVL.BK"),

        // หุ้นไทย - ธนาคารและการเงิน
        KnownAsset("KBANK", "ธนาคารกสิกรไทย", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "KBANK.BK"),
        KnownAsset("SCB", "เอสซีบี เอกซ์", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "SCB.BK"),
        KnownAsset("BBL", "ธนาคารกรุงเทพ", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "BBL.BK"),
        KnownAsset("KTB", "ธนาคารกรุงไทย", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "KTB.BK"),
        KnownAsset("TTB", "ธนาคารทหารไทยธนชาต", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "TTB.BK"),
        KnownAsset("TISCO", "ทิสโก้ไฟแนนเชียลกรุ๊ป", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "TISCO.BK"),
        KnownAsset("KKP", "ธนาคารเกียรตินาคินภัทร", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "KKP.BK"),
        KnownAsset("MTC", "เมืองไทย แคปปิตอล", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "MTC.BK"),
        KnownAsset("SAWAD", "ศรีสวัสดิ์ คอร์ปอเรชั่น", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "SAWAD.BK"),
        KnownAsset("TIDLOR", "เงินติดล้อ", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "TIDLOR.BK"),
        KnownAsset("KTC", "บัตรกรุงไทย", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "KTC.BK"),
        KnownAsset("TLI", "ไทยประกันชีวิต", AssetCategory.THAI_STOCK, StockSector.FINANCE, "THB", "TLI.BK"),

        // หุ้นไทย - เทคโนโลยีและการสื่อสาร
        KnownAsset("ADVANC", "แอดวานซ์ อินโฟร์ เซอร์วิส (AIS)", AssetCategory.THAI_STOCK, StockSector.TECH_ICT, "THB", "ADVANC.BK"),
        KnownAsset("TRUE", "ทรู คอร์ปอเรชั่น", AssetCategory.THAI_STOCK, StockSector.TECH_ICT, "THB", "TRUE.BK"),
        KnownAsset("DELTA", "เดลต้า อีเลคโทรนิคส์", AssetCategory.THAI_STOCK, StockSector.TECH_ICT, "THB", "DELTA.BK"),
        KnownAsset("HANA", "ฮานา ไมโครอิเล็คโทรนิคส", AssetCategory.THAI_STOCK, StockSector.TECH_ICT, "THB", "HANA.BK"),
        KnownAsset("KCE", "เคซีอี อีเลคโทรนิคส์", AssetCategory.THAI_STOCK, StockSector.TECH_ICT, "THB", "KCE.BK"),
        KnownAsset("JMART", "เจมาร์ท กรุ๊ป โฮลดิ้งส์", AssetCategory.THAI_STOCK, StockSector.TECH_ICT, "THB", "JMART.BK"),

        // หุ้นไทย - พาณิชย์และค้าปลีก
        KnownAsset("CPALL", "ซีพี ออลล์", AssetCategory.THAI_STOCK, StockSector.COMMERCE, "THB", "CPALL.BK"),
        KnownAsset("CPAXT", "ซีพี แอ็กซ์ตร้า (แม็คโคร-โลตัส)", AssetCategory.THAI_STOCK, StockSector.COMMERCE, "THB", "CPAXT.BK"),
        KnownAsset("CRC", "เซ็นทรัล รีเทล", AssetCategory.THAI_STOCK, StockSector.COMMERCE, "THB", "CRC.BK"),
        KnownAsset("HMPRO", "โฮม โปรดักส์ เซ็นเตอร์", AssetCategory.THAI_STOCK, StockSector.COMMERCE, "THB", "HMPRO.BK"),
        KnownAsset("BJC", "เบอร์ลี่ ยุคเกอร์", AssetCategory.THAI_STOCK, StockSector.COMMERCE, "THB", "BJC.BK"),
        KnownAsset("GLOBAL", "สยามโกลบอลเฮ้าส์", AssetCategory.THAI_STOCK, StockSector.COMMERCE, "THB", "GLOBAL.BK"),
        KnownAsset("COM7", "คอมเซเว่น", AssetCategory.THAI_STOCK, StockSector.COMMERCE, "THB", "COM7.BK"),

        // หุ้นไทย - การแพทย์และอาหาร/ท่องเที่ยว
        KnownAsset("BDMS", "กรุงเทพดุสิตเวชการ", AssetCategory.THAI_STOCK, StockSector.HEALTHCARE, "THB", "BDMS.BK"),
        KnownAsset("BH", "โรงพยาบาลบำรุงราษฎร์", AssetCategory.THAI_STOCK, StockSector.HEALTHCARE, "THB", "BH.BK"),
        KnownAsset("BCH", "บางกอก เชน ฮอสปิทอล", AssetCategory.THAI_STOCK, StockSector.HEALTHCARE, "THB", "BCH.BK"),
        KnownAsset("CHG", "โรงพยาบาลจุฬารัตน์", AssetCategory.THAI_STOCK, StockSector.HEALTHCARE, "THB", "CHG.BK"),
        KnownAsset("CPF", "เจริญโภคภัณฑ์อาหาร", AssetCategory.THAI_STOCK, StockSector.FOOD, "THB", "CPF.BK"),
        KnownAsset("TU", "ไทยยูเนี่ยน กรุ๊ป", AssetCategory.THAI_STOCK, StockSector.FOOD, "THB", "TU.BK"),
        KnownAsset("MINT", "ไมเนอร์ อินเตอร์เนชั่นแนล", AssetCategory.THAI_STOCK, StockSector.FOOD, "THB", "MINT.BK"),
        KnownAsset("CENTEL", "โรงแรมเซ็นทรัลพลาซา", AssetCategory.THAI_STOCK, StockSector.FOOD, "THB", "CENTEL.BK"),
        KnownAsset("OSP", "โอสถสภา", AssetCategory.THAI_STOCK, StockSector.FOOD, "THB", "OSP.BK"),
        KnownAsset("CBG", "คาราบาวกรุ๊ป", AssetCategory.THAI_STOCK, StockSector.FOOD, "THB", "CBG.BK"),

        // หุ้นต่างประเทศ (US / Global)
        KnownAsset("NVDA", "NVIDIA Corporation", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "NVDA"),
        KnownAsset("AAPL", "Apple Inc.", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "AAPL"),
        KnownAsset("MSFT", "Microsoft Corporation", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "MSFT"),
        KnownAsset("GOOGL", "Alphabet Inc. (Google)", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "GOOGL"),
        KnownAsset("META", "Meta Platforms Inc.", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "META"),
        KnownAsset("AVGO", "Broadcom Inc.", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "AVGO"),
        KnownAsset("AMD", "Advanced Micro Devices", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "AMD"),
        KnownAsset("TSM", "Taiwan Semiconductor (TSMC)", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "TSM"),
        KnownAsset("ASML", "ASML Holding N.V.", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "ASML"),
        KnownAsset("PLTR", "Palantir Technologies", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "PLTR"),
        KnownAsset("NFLX", "Netflix Inc.", AssetCategory.GLOBAL_STOCK, StockSector.TECH_ICT, "THB", "NFLX"),
        KnownAsset("AMZN", "Amazon.com Inc.", AssetCategory.GLOBAL_STOCK, StockSector.COMMERCE, "THB", "AMZN"),
        KnownAsset("COST", "Costco Wholesale Corp.", AssetCategory.GLOBAL_STOCK, StockSector.COMMERCE, "THB", "COST"),
        KnownAsset("WMT", "Walmart Inc.", AssetCategory.GLOBAL_STOCK, StockSector.COMMERCE, "THB", "WMT"),
        KnownAsset("TSLA", "Tesla Inc.", AssetCategory.GLOBAL_STOCK, StockSector.OTHER, "THB", "TSLA"),
        KnownAsset("BRK-B", "Berkshire Hathaway Inc.", AssetCategory.GLOBAL_STOCK, StockSector.FINANCE, "THB", "BRK-B"),
        KnownAsset("JPM", "JPMorgan Chase & Co.", AssetCategory.GLOBAL_STOCK, StockSector.FINANCE, "THB", "JPM"),
        KnownAsset("V", "Visa Inc.", AssetCategory.GLOBAL_STOCK, StockSector.FINANCE, "THB", "V"),
        KnownAsset("MA", "Mastercard Inc.", AssetCategory.GLOBAL_STOCK, StockSector.FINANCE, "THB", "MA"),
        KnownAsset("XOM", "Exxon Mobil Corp.", AssetCategory.GLOBAL_STOCK, StockSector.ENERGY, "THB", "XOM"),
        KnownAsset("CVX", "Chevron Corporation", AssetCategory.GLOBAL_STOCK, StockSector.ENERGY, "THB", "CVX"),
        KnownAsset("LLY", "Eli Lilly and Co.", AssetCategory.GLOBAL_STOCK, StockSector.HEALTHCARE, "THB", "LLY"),
        KnownAsset("JNJ", "Johnson & Johnson", AssetCategory.GLOBAL_STOCK, StockSector.HEALTHCARE, "THB", "JNJ"),
        KnownAsset("KO", "The Coca-Cola Company", AssetCategory.GLOBAL_STOCK, StockSector.FOOD, "THB", "KO"),
        KnownAsset("MCD", "McDonald's Corporation", AssetCategory.GLOBAL_STOCK, StockSector.FOOD, "THB", "MCD"),

        // กองทุนรวม / ETF
        KnownAsset("VOO", "Vanguard S&P 500 ETF", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", "VOO"),
        KnownAsset("VTI", "Vanguard Total Stock Market ETF", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", "VTI"),
        KnownAsset("QQQ", "Invesco QQQ Trust (Nasdaq 100)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", "QQQ"),
        KnownAsset("SCHD", "Schwab US Dividend Equity ETF", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", "SCHD"),
        KnownAsset("VT", "Vanguard Total World Stock ETF", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", "VT"),
        KnownAsset("TDEX", "ThaiDEX SET50 ETF", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", "TDEX.BK"),
        KnownAsset("1DIV", "ThaiDEX SET High Dividend ETF", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", "1DIV.BK"),
        KnownAsset("K-USA-A(A)", "กองทุนเปิดเค ยูเอสเอ หุ้นทุน (กสิกรไทย)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("K-CHANGE-A(A)", "กองทุนเปิดเค พอสซิทีฟ เชนจ์ (กสิกรไทย)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("K-VIETNAM", "กองทุนเปิดเค เวียดนาม (กสิกรไทย)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("K-GINCOME-A(R)", "กองทุนเปิดเค โกลบอล อินคัม (กสิกรไทย)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("K-FIXEDPLUS-A", "กองทุนเปิดเค ตราสารหนี้ พลัส (กสิกรไทย)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("SCBS&P500", "กองทุนเปิดไทยพาณิชย์ หุ้นยูเอส (SCBAM)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("SCBNDQ(A)", "กองทุนเปิดไทยพาณิชย์ หุ้นยูเอส แนสแด็ก (SCBAM)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("SCBROBOA", "กองทุนเปิดไทยพาณิชย์ โกลบอลโรโบติกส์ (SCBAM)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("SCBCTECHA", "กองทุนเปิดไทยพาณิชย์ China Technology (SCBAM)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("SCBSET50", "กองทุนเปิดไทยพาณิชย์ เซ็ท 50 อินเด็กซ์", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("DAOL-CHINABIO", "กองทุนเปิด ดาโอ ไชน่า ไบโอเทค (DAOL)", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("KFGBRAND-A", "กองทุนเปิดกรุงศรีโกลบอลแบรนด์อิควิตี้", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("KFACHINA-A", "กองทุนเปิดกรุงศรีไชน่าเอแชร์อิควิตี้", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("KFHHCARE-A", "กองทุนเปิดกรุงศรีโกลบอลเฮลธ์แคร์", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("ONE-UGG-RA", "กองทุนเปิด วรรณ อัลติเมท โกลบอล โกรท", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("B-INNOTECH", "กองทุนเปิดบัวหลวงโกลบอลอินโนเวชั่นและเทคโนโลยี", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),
        KnownAsset("PRINCIPAL VNEQ-A", "กองทุนเปิดพรินซิเพิล เวียดนาม อิควิตี้", AssetCategory.MUTUAL_FUND, StockSector.NONE, "THB", null),

        // ทองคำ
        KnownAsset("XAUUSD", "ทองคำโลก Spot (Oz)", AssetCategory.GOLD, StockSector.NONE, "THB", "GC=F"),
        KnownAsset("PAXG", "PAX Gold (โทเคนทองคำ 1 Oz)", AssetCategory.GOLD, StockSector.NONE, "THB", "PAXG-USD", "pax-gold"),
        KnownAsset("XAUT", "Tether Gold (1 Oz)", AssetCategory.GOLD, StockSector.NONE, "THB", "XAUT-USD", "tether-gold"),
        KnownAsset("GLD", "SPDR Gold Shares ETF", AssetCategory.GOLD, StockSector.NONE, "THB", "GLD"),
        KnownAsset("IAU", "iShares Gold Trust ETF", AssetCategory.GOLD, StockSector.NONE, "THB", "IAU"),
        KnownAsset("SCBGOLD", "กองทุนเปิดไทยพาณิชย์โกลด์", AssetCategory.GOLD, StockSector.NONE, "THB", null),
        KnownAsset("KGOLD", "กองทุนเปิดเค โกลด์", AssetCategory.GOLD, StockSector.NONE, "THB", null),
        KnownAsset("GOLD965", "ทองคำแท่ง 96.5% (บาททอง)", AssetCategory.GOLD, StockSector.NONE, "THB", null),

        // คริปโตเคอร์เรนซี
        KnownAsset("BTC", "Bitcoin", AssetCategory.CRYPTO, StockSector.NONE, "THB", "BTC-USD", "bitcoin"),
        KnownAsset("ETH", "Ethereum", AssetCategory.CRYPTO, StockSector.NONE, "THB", "ETH-USD", "ethereum"),
        KnownAsset("SOL", "Solana", AssetCategory.CRYPTO, StockSector.NONE, "THB", "SOL-USD", "solana"),
        KnownAsset("BNB", "BNB", AssetCategory.CRYPTO, StockSector.NONE, "THB", "BNB-USD", "binancecoin"),
        KnownAsset("XRP", "XRP", AssetCategory.CRYPTO, StockSector.NONE, "THB", "XRP-USD", "ripple"),
        KnownAsset("ADA", "Cardano", AssetCategory.CRYPTO, StockSector.NONE, "THB", "ADA-USD", "cardano"),
        KnownAsset("DOGE", "Dogecoin", AssetCategory.CRYPTO, StockSector.NONE, "THB", "DOGE-USD", "dogecoin"),
        KnownAsset("SUI", "Sui", AssetCategory.CRYPTO, StockSector.NONE, "THB", "SUI-USD", "sui"),
        KnownAsset("USDT", "Tether", AssetCategory.CRYPTO, StockSector.NONE, "THB", "USDT-USD", "tether")
    )

    fun byCategory(category: AssetCategory): List<KnownAsset> =
        allAssets.filter { it.category == category }

    fun findMatch(symbol: String, category: AssetCategory): KnownAsset? {
        val clean = symbol.trim().uppercase().removeSuffix(".BK")
        val cleanCrypto = if (category == AssetCategory.CRYPTO) clean.removeSuffix("-USD") else clean
        return allAssets.firstOrNull {
            it.category == category && (
                it.symbol.uppercase().removeSuffix(".BK") == clean ||
                    it.symbol.uppercase() == cleanCrypto
                )
        }
    }

    private fun normalizeCode(raw: String): String =
        raw.lowercase().replace("-", "").replace("(", "").replace(")", "").replace(" ", "")

    fun search(query: String, category: AssetCategory, limit: Int = 12): List<KnownAsset> {
        val q = query.trim().lowercase()
        val list = byCategory(category)
        if (q.isBlank()) return list.take(limit)
        val normQ = normalizeCode(q)
        val matched = list.filter {
            it.symbol.lowercase().contains(q) ||
                it.displayName.lowercase().contains(q) ||
                (normQ.isNotEmpty() && normalizeCode(it.symbol).contains(normQ))
        }
        return matched.sortedWith(
            compareBy<KnownAsset>(
                { !it.symbol.lowercase().startsWith(q) },
                { !(normQ.isNotEmpty() && normalizeCode(it.symbol).startsWith(normQ)) },
                { !it.displayName.lowercase().startsWith(q) },
                { it.symbol }
            )
        ).take(limit)
    }

    fun inferSector(symbol: String, category: AssetCategory): StockSector {
        if (!category.isStock) return StockSector.NONE
        return findMatch(symbol, category)?.sector ?: StockSector.OTHER
    }
}
