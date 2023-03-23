import java.io.File
import java.util.Scanner
import kotlin.system.exitProcess

val scanner = Scanner(System.`in`)

object StatisticBuilder {

    fun askAndExecuteSelfCommands() {}

    private fun buildGraphic() {}

    private fun printStatisticsInConsole() {}

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
        var content: String = ""
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


data class Text(private val name: String, private val sentencesCount: Int) {

    val sentencesList = listOf<Sentence>()

    fun getSentencesCount() {}

    data class Sentence(val wordsCount: Int) {}

}


object TextData {
    private val textsList = mutableListOf<Text>()

    fun addNewText(textName: String, content: String) {}

    object TextAnalyzer {
        private val DELIMITERS = listOf('.', '!', '?')

        fun returnListOfSentences(text: Text): List<Text.Sentence> {
            TODO()
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