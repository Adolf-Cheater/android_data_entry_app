import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONObject
import java.io.FileOutputStream

class OCRExcelExporter {

    fun exportToExcel(ocrResponse: String, outputPath: String) {
        val jsonObject = JSONObject(ocrResponse)
        val wordsInfo = jsonObject.getJSONArray("prism_wordsInfo")

        val categories = mutableListOf<String>()
        val values = mutableMapOf<String, MutableList<String>>()

        // Parse OCR response and identify categories and values
        for (i in 0 until wordsInfo.length()) {
            val wordInfo = wordsInfo.getJSONObject(i)
            val word = wordInfo.getString("word")
            val x = wordInfo.getInt("x")

            if (x < 500) { // Assuming categories are on the left side
                categories.add(word)
                values[word] = mutableListOf()
            } else {
                val matchedCategory = findMatchingCategory(categories, word)
                values[matchedCategory]?.add(word)
            }
        }

        // Create Excel file and write data
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("OCR Results")

        categories.forEachIndexed { index, category ->
            val row = sheet.createRow(index)
            row.createCell(0).setCellValue(category)
            values[category]?.forEachIndexed { valueIndex, value ->
                row.createCell(valueIndex + 1).setCellValue(value)
            }
        }

        FileOutputStream(outputPath).use { outputStream ->
            workbook.write(outputStream)
        }
    }

    private fun findMatchingCategory(categories: List<String>, word: String): String {
        return categories.minByOrNull { category ->
            levenshteinDistance(category.lowercase(), word.lowercase())
        } ?: word
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                )
            }
        }

        return dp[m][n]
    }
}