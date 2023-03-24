import java.io.File
import java.util.Scanner
import kotlin.system.exitProcess
import org.jetbrains.letsPlot.*
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.intern.Plot


val scanner = Scanner(System.`in`)

object StatisticBuilder {

    fun askAndExecuteSelfCommands() {

        val text = getTextData() ?: return

        var counter = 1
        val wordsCountsMap = text.getSentencesList().map { counter++ to it.getWordsCount() }

        println("Print \"console\" if you have see data in console, \"graphic\" if you have see histogram and \"both\" if you have see them together:")
        whenStatShowCycle@ while (true) {
            when (readln()) {
                "console" -> printStatisticsInConsole(text.getName(), wordsCountsMap.toMap())
                "graphic" -> buildGraphic(text.getName(), wordsCountsMap.toMap())
                "both" -> {
                    buildGraphic(text.getName(), wordsCountsMap.toMap())
                    printStatisticsInConsole(text.getName(), wordsCountsMap.toMap())
                }

                "return" -> return
                else -> {
                    println("Repeat input or enter \"return\" to return in main menu")
                    continue@whenStatShowCycle
                }
            }
            break@whenStatShowCycle
        }

    }

    private fun buildGraphic(textName: String, mapOfSentenceNumToItsSize: Map<Int, Int>) {
        val plot: Plot =
            ggplot(mapOfSentenceNumToItsSize) + ggsize(1000, 600) + geomBar { x = "sentence number"; y = "words count" }


    }

    private fun printStatisticsInConsole(textName: String, mapOfSentenceNumToItsSize: Map<Int, Int>) {
        println("-".repeat(textName.length))
        println("Text name: $textName")
        println("-".repeat(textName.length))
        println(
            "Statistics[num of sentence: count of words in it]: ${
                mapOfSentenceNumToItsSize.toList().joinToString("; ") { "${it.first}: ${it.second}" } }.")
        println("-".repeat(textName.length))
        println("Done!\n")
    }

    private fun getTextData(): TextData.Text? {

        println("Input name of text which you have to get statistics about:")
        println("Saved texts: ${TextData.getTextsNamesInString()}")

        cycleSearchText@ while (true) {
            return when (val name = readln().trim()) {
                in TextData.getTextsNamesList() -> {
                    TextData.getTextByName(name)!!
                }

                "return" -> null
                else -> {
                    println("No text with name $name. Repeat input or enter return to return in main menu.")
                    continue@cycleSearchText
                }
            }
        }

    }


}


object TextReader {
    fun askAndExecuteSelfCommands() {

        println("Where do you want to add the text: from the console of a file?")
        when (readln().lowercase()) {
            "console" -> readFromConsole()
            "file" -> readFromFile()
            "return" -> return
            else -> {
                println("Repeat input or print \"return\" to return in main menu:")
                askAndExecuteSelfCommands()
            }
        }
    }

    private fun readFromConsole() {

        println("Input name of a text:")
        val name = readln()

        println("Input a text content:")
        var content = ""
        readCycle@ while (true) {
            val nextPart = scanner.next()
            if (nextPart.isEmpty()) content += nextPart
            else break@readCycle
        }

        correctInputQuestion@ while (true) {
            print("Input was correct?[yes, no]: ")
            when (readln()) {
                "yes" -> addTextToData(name, content)
                "no" -> readFromConsole()
                "return" -> return
                else -> {
                    println("Input only \"yes\" or \"no\" or \"return\" if you want to exit in main menu:")
                }
            }
        }
    }

    private fun readFromFile() {

        println("Input a name of a text:")
        val name = readln()

        val contentsFile: File
        pathReadCycle@ while (true) {
            println("Input a path to file:")
            val filePath = readln()
            val testFile = File(filePath)

            when {
                filePath == "return" -> return
                !testFile.exists() -> {
                    println("File path incorrect. Repeat input or enter \"return\" to return in main menu:")
                    continue@pathReadCycle
                }

                else -> {
                    contentsFile = testFile
                    break@pathReadCycle
                }
            }
        }
        val content = contentsFile.readText()

        correctInputQuestion@ while (true) {
            print("Input was correct?[yes, no]: ")
            when (readln()) {
                "yes" -> addTextToData(name, content)
                "no" -> readFromFile()
                "return" -> return
                else -> {
                    println("Input only \"yes\" or \"no\" or \"return\" if you want to exit in main menu:")
                }
            }
        }

    }

    private fun addTextToData(textName: String, content: String) {
        TextData.addNewText(textName, content)
    }

}


object TextData {
    private val textsList = mutableListOf<Text>()

    fun getTextByName(searchingName: String): Text? {
        for (text in textsList) if (text.getName() == searchingName) return text
        return null
    }

    fun getTextsNamesInString(delimiter: String = ", "): String {
        return textsList.joinToString(delimiter) { it.getName() }
    }

    fun getTextsNamesList(): List<String> {
        return textsList.map { it.getName() }
    }

    fun addNewText(textName: String, content: String) {
        textsList.add(TextAnalyzer.getTextObjFromContents(textName, content))
    }

    object TextAnalyzer {
        private val DELIMITERS = Regex("([!?.]|(\\.\\.\\.))\\s")
        private val WHITESPACES_WITHOUT_SPACE = Regex("(?=\\s+)(?!=\\s)")


        fun getTextObjFromContents(name: String, content: String): Text {
            var sentencesCount = 0
            val listOfSentences = mutableListOf<Text.Sentence>()

            val sentencesStringList = content.split(DELIMITERS).map { it.replace(WHITESPACES_WITHOUT_SPACE, " ") }
            for (sentenceText in sentencesStringList) {
                val wordsList = sentenceText.split(" ").toMutableList()
                wordsList.removeIf { it == "" }
                if (wordsList.isNotEmpty()) {
                    sentencesCount++
                    listOfSentences.add(Text.Sentence(wordsList.size))
                }
            }
            println(sentencesStringList)

            return Text(name, sentencesCount, listOfSentences.toList())

        }

    }

    data class Text(
        private val name: String,
        private val sentencesCount: Int,
        private val sentencesList: List<Sentence>,
    ) {

        fun getSentencesCount() = sentencesCount
        fun getName() = name

        fun getSentencesList(): List<Sentence> {
            return sentencesList
        }

        data class Sentence(private val wordsCount: Int) {
            fun getWordsCount() = wordsCount
        }

    }

}


object CommandCenter {

    private enum class Commands(val executingFun: () -> Unit) {
        EXIT({
            println("bye!")
            exitProcess(0)
        }),
        ADD_TEXT({ TextReader.askAndExecuteSelfCommands() }),
        SHOW_STATISTICS({ StatisticBuilder.askAndExecuteSelfCommands() }),
        REMOVE_TEXT({ println("remove") })
    }

    fun readCommandFromConsole(): () -> Unit {
        println("Input one Of the commands: ${Commands.values().joinToString { it.name.lowercase() }}:")
        val funNameInUpCase = scanner.nextLine().uppercase()

        return if (funNameInUpCase in Commands.values().map { it.name })
            Commands.valueOf(funNameInUpCase).executingFun
        else ::readCommandFromConsole
    }

}


fun main() {
    mainCycle@ while (true) {
        (CommandCenter.readCommandFromConsole())()
    }
}