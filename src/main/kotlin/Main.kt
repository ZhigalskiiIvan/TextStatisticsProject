import java.util.Scanner
import kotlin.system.exitProcess

val scanner = Scanner(System.`in`)

object StatisticBuilder {

    fun buildGraphic() {}

    fun printStatisticsInConsole() {}

}


object TextReader {
    fun readFromConsole(): String {
        TODO()
    }
}


class TextData() {
    private val textsList = mutableListOf<Text>()

    fun addNewText(textName: String, content: String) {}

    object TextAnalyzer {
        private val DELIMITERS = listOf('.', '!', '?')

        fun returnListOfSentences(text: Text): List<Text.Sentence> {
            TODO()
        }

    }

    class Text(private val name: String, private val sentencesCount: Int) {

        val sentencesList = listOf<Sentence>()

        fun getSentencesCount() {}

        class Sentence(val words_count: Int) {}

    }

}


object CommandCenter {

    private enum class Commands(val executingFun: () -> Unit) {
        EXIT({
            println("bye!")
            exitProcess(0)
        }),
        ADD_TEXT({ println("add") }),
        SHOW_STATISTICS({ println("sow") }),
        REMOVE_TEXT({ println("remove") })
    }

    fun readCommandFromConsole(): () -> Unit {
        println("Input one Of the commands: ${Commands.values().joinToString { it.name.lowercase() }}:")
        val funNameInUpCase = scanner.nextLine().uppercase()

        return if (funNameInUpCase in Commands.values().map { it.name })
            Commands.valueOf(funNameInUpCase).executingFun
        else ::readCommandFromConsole
    }

    fun executeCommand() {}

}


fun main() {

    mainCycle@ while (true) {

        (CommandCenter.readCommandFromConsole())()

    }
}