data class OCRRequest(
    val img: String,
    val prob: Boolean,
    val charInfo: Boolean,
    val rotate: Boolean,
    val table: Boolean,
    val sortPage: Boolean,
    val noStamp: Boolean,
    val figure: Boolean,
    val row: Boolean,
    val paragraph: Boolean,
    val oricoord: Boolean
)