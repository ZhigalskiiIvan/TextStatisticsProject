import java.io.File
import java.util.Scanner
import kotlin.system.exitProcess
import org.jetbrains.letsPlot.*
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.intern.Plot
import kotlin.reflect.typeOf


val scanner = Scanner(System.`in`)


object StatisticBuilder {

    fun askAndExecuteSelfCommands() {

        if (!TextData.haveText()) {
            println("No saved texts")
            return
        }

        val text = TextData.getTextData("Input name of a text which you want to see statistics about.")

        var counter = 1
        val wordsCountsMap = text.getSentencesList().map { counter++ to it.getWordsCount() }

        println("Print \"console\" if you have see data in console, \"graphic\" if you have see histogram and \"both\" if you have see them together:")

        val request = Main.requestInput(listOf("console", "graphic", "both"))
        request.first.exe()

        when (request.second) {
            "console" -> printStatisticsInConsole(text.getName(), wordsCountsMap.toMap())
            "graphic" -> buildGraphic(text.getName(), wordsCountsMap.toMap())
            "both" -> {
                buildGraphic(text.getName(), wordsCountsMap.toMap())
                printStatisticsInConsole(text.getName(), wordsCountsMap.toMap())
            }
        }


    }

    private fun buildGraphic(textName: String, mapOfSentenceNumToItsSize: Map<Int, Int>) {
        val plot: Plot =
            ggplot(mapOfSentenceNumToItsSize) + ggsize(1000, 600) + geomBar { x = "sentence number"; y = "words count" }

    }

    private fun printStatisticsInConsole(textName: String, mapOfSentenceNumToItsSize: Map<Int, Int>) {
        val sectionTitle = "Text name: $textName"
        println("-".repeat(sectionTitle.length))
        println(sectionTitle)
        println("-".repeat(sectionTitle.length))

        val totalSentencesCount = mapOfSentenceNumToItsSize.size
        var totalWordsCount = 0
        for (sentInfo in mapOfSentenceNumToItsSize) {
            totalWordsCount += sentInfo.value
        }
        println(
            """
            |Statistics:
            |[count of sentences]: 
            |--- $totalSentencesCount
            |[total number of words]:
            |--- $totalWordsCount
            |[num of sentence: count of words in it]:
            |--- ${mapOfSentenceNumToItsSize.toList().joinToString("; ") { "${it.first} contains ${it.second}" }}.
            """.trimMargin()
        )
        println("-".repeat(sectionTitle.length))
        println("Done!\n")
    }

}


object TextReader {
    fun askAndExecuteSelfCommands() {

        println("Where do you want to add the text: from the console or a file?")

        val kindOfSource = Main.requestInput(listOf("console", "file"))
        kindOfSource.first.exe()

        when (kindOfSource.second) {
            "console" -> readFromConsole()
            "file" -> readFromFile()
        }
    }

    private fun readFromConsole() {

        println("Input name of a text:")
        val name = readln()

        println("Input a text content(after input text press enter twice):")
        var content = ""

        var itWasNewParYet = false
        readCycle@ while (true) {
            val nextPart = readln()
            if (nextPart.isEmpty()) {
                if (itWasNewParYet) break@readCycle
                else {
                    content += "\n"
                    itWasNewParYet = true
                }
            } else {
                itWasNewParYet = false
                content += "$nextPart\n"
            }
        }

        println("Was input data right?[yes, no]:")

        val correctInputRequest = Main.requestInput(listOf("yes", "no"))
        correctInputRequest.first.exe()

        if (correctInputRequest.second == "yes") addTextToData(name, content)
        else readFromConsole()
    }

    private fun readFromFile() {

        println("Input a name of a text:")
        val name = readln()

        val contentsFile: File

        pathReadCycle@ while (true) {
            val pathRequest = Main.requestInput<String>()
            pathRequest.first.exe()
            val filePath = pathRequest.second.toString()

            val testFile = File(filePath)

            if (!testFile.exists()) {
                println("Incorrect path. Repeat the input:")
                continue@pathReadCycle
            } else {
                contentsFile = testFile
                break@pathReadCycle
            }
        }

        val content = contentsFile.readText()

        print("Input was correct?[yes, no]: ")
        val correctInputRequest = Main.requestInput(listOf("yes", "no"))
        correctInputRequest.first.exe()

        when (correctInputRequest.second) {
            "yes" -> addTextToData(name, content)
            "no" -> readFromFile()
        }
    }

    private fun addTextToData(textName: String, content: String) {
        TextData.addNewText(textName, content)
    }

}


object TextData {
    private val textsList = mutableListOf<Text>()

    fun haveText(): Boolean = !textsList.isEmpty()

    fun removeText() {

        if (!haveText()) {
            println("No saved texts")
            return
        }

        val removingText = getTextData("Input name of text which you want to remove.")
        textsList.remove(removingText)

        println("Text ${removingText.getName()} removed.")
    }


    private fun getTextByName(searchingName: String): Text? {
        for (text in textsList) if (text.getName() == searchingName) return text
        return null
    }

    private fun getTextsNamesInString(delimiter: String = ", "): String {
        return textsList.joinToString(delimiter) { it.getName() }
    }

    private fun getTextsNamesList(): List<String> {
        return textsList.map { it.getName() }
    }


    fun addNewText(textName: String, content: String) {
        textsList.add(TextAnalyzer.getTextObjFromContents(textName, content))
    }


    fun getTextData(message: String = "Input name of a text"): Text {
        println(message)
        println("Saved texts: ${getTextsNamesInString()}")

        val nameRequest = Main.requestInput(getTextsNamesList())
        nameRequest.first.exe()

        return getTextByName(nameRequest.second.toString())!!
    }


    object TextAnalyzer {
        private val DELIMITERS = Regex("[!?.]+\\s+")
        private val WHITESPACES = Regex("\\s+")
        private val WHITESPACES_OR_EMPTY = Regex("(\\s+)?")

        fun getTextObjFromContents(name: String, content: String): Text {
            var sentencesCount = 1
            val listOfSentences = mutableListOf<Text.Sentence>()

            val sentencesStringList = content
                .split(DELIMITERS)
                .map { it.replace(WHITESPACES, " ") }.toMutableList()
            sentencesStringList.removeIf { it.matches(WHITESPACES_OR_EMPTY) }

            for (sentenceText in sentencesStringList) {
                val wordsList = sentenceText.split(WHITESPACES).toMutableList()
                wordsList.removeIf { it.matches(WHITESPACES_OR_EMPTY) }
                sentencesCount++
                listOfSentences.add(Text.Sentence(wordsList.size))
            }

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

        fun getSentencesList() = sentencesList

        data class Sentence(private val wordsCount: Int) {
            fun getWordsCount() = wordsCount
        }
    }

}


fun exit(): Nothing {
    println("bye!")
    exitProcess(0)
}


object CommandCenter {

    private enum class Commands(val executingFun: () -> Unit) {
        EXIT(::exit),
        ADD_TEXT({ TextReader.askAndExecuteSelfCommands() }),
        SHOW_STATISTICS({ StatisticBuilder.askAndExecuteSelfCommands() }),
        REMOVE_TEXT({ TextData.removeText() })
    }

    fun readCommandFromConsole(): () -> Unit {
        println("Input one of the commands: ${Commands.values().joinToString { it.name }}:")

        val commandNameRequest = Main.requestInput(Commands.values().map { it.name })
        commandNameRequest.first.exe()

        val funName = commandNameRequest.second.toString()

        return Commands.valueOf(funName).executingFun
    }

}


interface InputOutcomeCommand {
    val exe: () -> Unit
}

object ContinueCommand : InputOutcomeCommand {
    override val exe: () -> Unit = {}
}

object ReturnCommand : InputOutcomeCommand {
    override val exe: () -> Unit = {
        throw ReturnException()
    }
}


class InvalidInputTypeException(expectedType: String) : Exception(expectedType) {
    override val message: String = expectedType

    init {
        "Was expected type: $message, but it's impossible to convert input in it."
    }
}

class InvalidElemInInputException : Exception()

class ReturnException : Exception()


object Main {

    fun workCycle() {
        mainCycle@ while (true) {
            try {
                (CommandCenter.readCommandFromConsole())()
            } catch (e: ReturnException) {
                println("You have been returned in main menu.")
                continue@mainCycle
            }
        }
    }

    inline fun <reified T> requestInput(availableInputs: List<T>? = null): Pair<InputOutcomeCommand, Any> {

        var inputT: Any

        readingAndChangingTypeCycle@ while (true) {

            val input = readln().trim()
            if (input == "return") return Pair(ReturnCommand, "")

            try {
                inputT = when (typeOf<T>()) {
                    typeOf<Int>() -> input.toInt()
                    typeOf<Double>() -> input.toDouble()
                    typeOf<Boolean>() -> input.toBoolean()
                    typeOf<Byte>() -> input.toByte()
                    typeOf<Long>() -> input.toLong()
                    typeOf<Float>() -> input.toFloat()
                    typeOf<Char>() -> {
                        if (input.trim().length == 1) {
                            input.trim().toCharArray()[0]
                        } else throw InvalidInputTypeException(typeOf<Char>().toString())
                    }

                    typeOf<String>() -> input
                    else -> throw InvalidInputTypeException(typeOf<T>().toString())
                }

                return if (availableInputs != null) {
                    if (inputT.toString() in availableInputs
                            .map { it.toString() }
                    ) Pair(ContinueCommand, inputT)
                    else throw InvalidElemInInputException()
                } else Pair(ContinueCommand, inputT)

            } catch (e: NumberFormatException) {
                continue@readingAndChangingTypeCycle
            } catch (e: InvalidInputTypeException) {
                continue@readingAndChangingTypeCycle
            } catch (e: InvalidElemInInputException) {
                println(
                    "There isn't this elem in list of available inputs. " +
                            "Try to repeat or enter return to exit in main menu: "
                )
                continue@readingAndChangingTypeCycle
            }
        }
    }

}


fun main() {
    Main.workCycle()
}