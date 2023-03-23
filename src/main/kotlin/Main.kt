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

        fun returnListOfSentences(text: Text): List<Text.Sentence> { TODO() }

    }

    class Text(private val name: String, private val sentencesCount: Int) {

        val sentencesList = listOf<Sentence>()

        fun getSentencesCount() {}

        class Sentence(val words_count: Int) {}

    }

}


object CommandCenter {

    enum class Commands(val commandName: String, val executingFun: (Any) -> Any) {
        Exit("exit", TODO()),
        Add("add", TODO())
    }

    fun readCommandFromConsole() {}

    fun executeCommand() {}

}


fun main(args: Array<String>) {

    mainCycle@ while (true) {
    }
}