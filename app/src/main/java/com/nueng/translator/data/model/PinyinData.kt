package com.nueng.translator.data.model

data class PinyinItem(
    val pinyin: String,
    val character: String = "",
    val tones: List<String> = emptyList(),
    val toneChars: List<String> = emptyList(), // Chinese chars for each tone to feed TTS
    val example: String = "",
    val examplePinyin: String = ""
)

data class StrokeItem(
    val name: String,
    val chinese: String,
    val pinyin: String,
    val unicode: String = "",
    val description: String = ""
)

object PinyinData {

    // ========== SIMPLE VOWELS (单韵母) — 6 vowels, all with 4 tones ==========
    val simpleVowels = listOf(
        PinyinItem("a", "", listOf("ā","á","ǎ","à"),
            listOf("阿","啊","啊","啊"), "啊", "ā"),
        PinyinItem("o", "", listOf("ō","ó","ǒ","ò"),
            listOf("喔","哦","哦","哦"), "哦", "ó"),
        PinyinItem("e", "", listOf("ē","é","ě","è"),
            listOf("鹅","额","恶","饿"), "鹅", "é"),
        PinyinItem("i", "", listOf("ī","í","ǐ","ì"),
            listOf("衣","姨","椅","意"), "衣", "yī"),
        PinyinItem("u", "", listOf("ū","ú","ǔ","ù"),
            listOf("乌","无","五","物"), "乌", "wū"),
        PinyinItem("ü", "", listOf("ǖ","ǘ","ǚ","ǜ"),
            listOf("迂","鱼","雨","玉"), "鱼", "yú")
    )

    // ========== COMPOUND VOWELS (复韵母) — all with 4 tones ==========
    val compoundVowels = listOf(
        PinyinItem("ai", "", listOf("āi","ái","ǎi","ài"),
            listOf("哀","挨","矮","爱"), "爱", "ài"),
        PinyinItem("ei", "", listOf("ēi","éi","ěi","èi"),
            listOf("诶","诶","诶","诶"), "诶", "éi"),
        PinyinItem("ao", "", listOf("āo","áo","ǎo","ào"),
            listOf("凹","熬","袄","奥"), "奥", "ào"),
        PinyinItem("ou", "", listOf("ōu","óu","ǒu","òu"),
            listOf("欧","偶","偶","偶"), "偶", "ǒu"),
        PinyinItem("an", "", listOf("ān","án","ǎn","àn"),
            listOf("安","安","俺","暗"), "安", "ān"),
        PinyinItem("en", "", listOf("ēn","én","ěn","èn"),
            listOf("恩","恩","恩","恩"), "恩", "ēn"),
        PinyinItem("ang", "", listOf("āng","áng","ǎng","àng"),
            listOf("昂","昂","昂","昂"), "昂", "áng"),
        PinyinItem("eng", "", listOf("ēng","éng","ěng","èng"),
            listOf("鞥","鞥","鞥","鞥"), "鞥", "ēng"),
        PinyinItem("ong", "", listOf("ōng","óng","ǒng","òng"),
            listOf("翁","翁","翁","翁"), "翁", "wēng"),
        PinyinItem("er", "", listOf("ēr","ér","ěr","èr"),
            listOf("耳","儿","耳","二"), "二", "èr"),
        PinyinItem("ia", "", listOf("iā","iá","iǎ","ià"),
            listOf("呀","呀","呀","呀"), "呀", "ya"),
        PinyinItem("ie", "", listOf("iē","ié","iě","iè"),
            listOf("耶","爷","也","夜"), "也", "yě"),
        PinyinItem("iu", "", listOf("iū","iú","iǔ","iù"),
            listOf("优","由","有","又"), "有", "yǒu"),
        PinyinItem("ian", "", listOf("iān","ián","iǎn","iàn"),
            listOf("烟","延","眼","验"), "烟", "yān"),
        PinyinItem("iang", "", listOf("iāng","iáng","iǎng","iàng"),
            listOf("央","阳","养","样"), "阳", "yáng"),
        PinyinItem("in", "", listOf("īn","ín","ǐn","ìn"),
            listOf("音","银","引","印"), "音", "yīn"),
        PinyinItem("ing", "", listOf("īng","íng","ǐng","ìng"),
            listOf("英","迎","影","硬"), "英", "yīng"),
        PinyinItem("ua", "", listOf("uā","uá","uǎ","uà"),
            listOf("花","华","化","话"), "花", "huā"),
        PinyinItem("uo", "", listOf("uō","uó","uǒ","uò"),
            listOf("窝","国","我","卧"), "我", "wǒ"),
        PinyinItem("ui", "", listOf("uī","uí","uǐ","uì"),
            listOf("威","围","伟","位"), "威", "wēi"),
        PinyinItem("uan", "", listOf("uān","uán","uǎn","uàn"),
            listOf("弯","完","晚","万"), "弯", "wān"),
        PinyinItem("uang", "", listOf("uāng","uáng","uǎng","uàng"),
            listOf("汪","王","往","旺"), "王", "wáng"),
        PinyinItem("un", "", listOf("ūn","ún","ǔn","ùn"),
            listOf("温","文","稳","问"), "温", "wēn"),
        PinyinItem("üe", "", listOf("üē","üé","üě","üè"),
            listOf("约","月","月","月"), "月", "yuè"),
        PinyinItem("ün", "", listOf("ǖn","ǘn","ǚn","ǜn"),
            listOf("晕","云","允","运"), "云", "yún")
    )

    // ========== CONSONANTS (声母) ==========
    val consonants = listOf(
        PinyinItem("b", "", emptyList(), emptyList(), "爸", "bà"),
        PinyinItem("p", "", emptyList(), emptyList(), "怕", "pà"),
        PinyinItem("m", "", emptyList(), emptyList(), "妈", "mā"),
        PinyinItem("f", "", emptyList(), emptyList(), "发", "fā"),
        PinyinItem("d", "", emptyList(), emptyList(), "大", "dà"),
        PinyinItem("t", "", emptyList(), emptyList(), "他", "tā"),
        PinyinItem("n", "", emptyList(), emptyList(), "那", "nà"),
        PinyinItem("l", "", emptyList(), emptyList(), "拉", "lā"),
        PinyinItem("g", "", emptyList(), emptyList(), "哥", "gē"),
        PinyinItem("k", "", emptyList(), emptyList(), "可", "kě"),
        PinyinItem("h", "", emptyList(), emptyList(), "喝", "hē"),
        PinyinItem("j", "", emptyList(), emptyList(), "鸡", "jī"),
        PinyinItem("q", "", emptyList(), emptyList(), "七", "qī"),
        PinyinItem("x", "", emptyList(), emptyList(), "西", "xī"),
        PinyinItem("zh", "", emptyList(), emptyList(), "知", "zhī"),
        PinyinItem("ch", "", emptyList(), emptyList(), "吃", "chī"),
        PinyinItem("sh", "", emptyList(), emptyList(), "十", "shí"),
        PinyinItem("r", "", emptyList(), emptyList(), "日", "rì"),
        PinyinItem("z", "", emptyList(), emptyList(), "字", "zì"),
        PinyinItem("c", "", emptyList(), emptyList(), "次", "cì"),
        PinyinItem("s", "", emptyList(), emptyList(), "四", "sì"),
        PinyinItem("y", "", emptyList(), emptyList(), "一", "yī"),
        PinyinItem("w", "", emptyList(), emptyList(), "五", "wǔ")
    )

    // ========== BASIC STROKES (基本笔画) — 6 ==========
    val basicStrokes = listOf(
        StrokeItem("héng", "横", "héng", "一", "Horizontal stroke, left to right"),
        StrokeItem("shù", "竖", "shù", "丨", "Vertical stroke, top to bottom"),
        StrokeItem("piě", "撇", "piě", "丿", "Left-falling stroke"),
        StrokeItem("nà", "捺", "nà", "㇏", "Right-falling stroke"),
        StrokeItem("diǎn", "点", "diǎn", "丶", "Dot stroke"),
        StrokeItem("tí", "提", "tí", "㇀", "Rising stroke, lower-left to upper-right")
    )

    // ========== TURNING/COMPOUND STROKES (转折笔画) — complete ==========
    val turningStrokes = listOf(
        StrokeItem("héngzhé", "横折", "héngzhé", "𠃍", "Horizontal then turn down: 口"),
        StrokeItem("hénggōu", "横钩", "hénggōu", "乛", "Horizontal then hook: 买"),
        StrokeItem("héngzhégōu", "横折钩", "héngzhégōu", "𠃌", "Horizontal + turn + hook: 月"),
        StrokeItem("héngzhétí", "横折提", "héngzhétí", "⺄", "Horizontal + turn + rising: 计"),
        StrokeItem("héngzhéwān", "横折弯", "héngzhéwān", "⺄", "Horizontal + turn + curve: 沿"),
        StrokeItem("héngzhéwāngōu", "横折弯钩", "héngzhéwāngōu", "乙", "Horizontal + turn + curve + hook: 乙"),
        StrokeItem("héngzhéxiégōu", "横折斜钩", "héngzhéxiégōu", "⻍", "Horizontal + turn + diagonal hook: 风"),
        StrokeItem("héngzhézhé", "横折折", "héngzhézhé", "𠄎", "Horizontal + turn + turn: 凹"),
        StrokeItem("héngzhézhépiě", "横折折撇", "héngzhézhépiě", "㇅", "Horizontal + turn + turn + left-fall: 及"),
        StrokeItem("héngzhézhézhé", "横折折折", "héngzhézhézhé", "㇎", "Triple turn: 凸"),
        StrokeItem("héngzhézhézhégōu", "横折折折钩", "héngzhézhézhégōu", "𠄏", "Triple turn + hook: 乃"),
        StrokeItem("héngpiě", "横撇", "héngpiě", "㇇", "Horizontal then left-falling: 又"),
        StrokeItem("héngpiěwāngōu", "横撇弯钩", "héngpiěwāngōu", "㇌", "Horizontal + left-fall + curve + hook: 那"),
        StrokeItem("shùgōu", "竖钩", "shùgōu", "亅", "Vertical then hook: 小"),
        StrokeItem("shùzhé", "竖折", "shùzhé", "𠃊", "Vertical then turn right: 山"),
        StrokeItem("shùtí", "竖提", "shùtí", "𠄌", "Vertical then rising: 以"),
        StrokeItem("shùwān", "竖弯", "shùwān", "㇄", "Vertical then curve: 四"),
        StrokeItem("shùwāngōu", "竖弯钩", "shùwāngōu", "乚", "Vertical + curve + hook: 几"),
        StrokeItem("shùzhégōu", "竖折钩", "shùzhégōu", "㇗", "Vertical + turn + hook: 马"),
        StrokeItem("shùzhézhé", "竖折折", "shùzhézhé", "𠃑", "Vertical + turn + turn: 鼎"),
        StrokeItem("shùzhézhépiě", "竖折折撇", "shùzhézhépiě", "ㄣ", "Vertical + turn + turn + left-fall: 专"),
        StrokeItem("shùzhézhégōu", "竖折折钩", "shùzhézhégōu", "㇉", "Vertical + turn + turn + hook: 与"),
        StrokeItem("piězhé", "撇折", "piězhé", "𠃋", "Left-falling then turn right: 云"),
        StrokeItem("piědiǎn", "撇点", "piědiǎn", "㇛", "Left-falling then dot: 女"),
        StrokeItem("wāngōu", "弯钩", "wāngōu", "㇚", "Curved hook: 手"),
        StrokeItem("xiégōu", "斜钩", "xiégōu", "㇂", "Diagonal hook: 我"),
        StrokeItem("wògōu", "卧钩", "wògōu", "㇃", "Lying hook: 心")
    )

    // ========== WRITING RULES (书写规则) ==========
    val writingRules = listOf(
        "先横后竖 (héng before shù) — Write horizontal before vertical: 十",
        "先撇后捺 (piě before nà) — Write left-falling before right-falling: 人",
        "从上到下 (top to bottom) — Write from top to bottom: 三",
        "从左到右 (left to right) — Write from left to right: 明",
        "先外后内 (outside before inside) — Write enclosure before contents: 回",
        "先进后关 (enter then close) — Fill inside, then close the bottom: 国",
        "先中间后两边 (center before sides) — Write center stroke first: 小"
    )
}
