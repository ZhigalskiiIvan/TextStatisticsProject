import java.io.File
import java.util.Scanner
import kotlin.system.exitProcess
import org.jetbrains.letsPlot.*
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.intern.Plot
import kotlin.reflect.KType
import kotlin.reflect.typeOf


val scanner = Scanner(System.`in`)

object StatisticBuilder {

    fun askAndExecuteSelfCommands() {

        val text = getTextData() ?: return

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
        println("-".repeat(textName.length))
        println("Text name: $textName")
        println("-".repeat(textName.length))
        println(
            "Statistics[num of sentence: count of words in it]: ${
                mapOfSentenceNumToItsSize.toList().joinToString("; ") { "${it.first}: ${it.second}" }
            }.")
        println("-".repeat(textName.length))
        println("Done!\n")
    }

    private fun getTextData(): TextData.Text? {
        println("Input name of text which you have to get statistics about:")
        println("Saved texts: ${TextData.getTextsNamesInString()}")

        val nameRequest = Main.requestInput(TextData.getTextsNamesList())
        nameRequest.first.exe()

        return TextData.getTextByName(nameRequest.second.toString())
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


interface InputOutcomeCommand {
    val exe: () -> Unit
}

object ContinueCommand : InputOutcomeCommand {
    override val exe: () -> Unit = {}
}

object ReturnCommand : InputOutcomeCommand {
    override val exe: () -> Unit = { Main.work() }
}


class InvalidInputTypeException(expectedType: String) : Exception(expectedType) {
    override val message: String = expectedType
        get() = "Was expected type: $field, but it's impossible to convert input in it."

}

class InvalidElemInInputException() : Exception()


object Main {

    fun work() {
        mainCycle@ while (true) {
            (CommandCenter.readCommandFromConsole())()
        }
    }

    inline fun <reified T> requestInput(availableInputs: List<T>? = null): Pair<InputOutcomeCommand, Any> {

        var inputT: Any

        readingAndChangingTypeCycle@ while (true) {

            val input = readln()
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
                    if (inputT.toString() in availableInputs.map { it.toString() }) Pair(ContinueCommand, inputT)
                    else throw InvalidElemInInputException()
                } else Pair(ContinueCommand, inputT)

            } catch (e: NumberFormatException) {
                println(e.message)
                println("")
                continue@readingAndChangingTypeCycle
            } catch (e: InvalidInputTypeException) {
                println(e.message)
                println("")
                continue@readingAndChangingTypeCycle
            } catch (e: InvalidElemInInputException) {
                println("There isn't this elem in list of available inputs. Try to repeat or enter return to exit in main menu: ")
                continue@readingAndChangingTypeCycle
            }
        }
    }

}

fun main() {
    Main.work()
}