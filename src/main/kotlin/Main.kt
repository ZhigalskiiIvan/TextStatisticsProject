import java.io.File
import kotlin.system.exitProcess
import org.jetbrains.letsPlot.*
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.*
import kotlin.reflect.typeOf


/** Recognizes the name of the text, which
 *  user want to see statistics about and type of output.
 *  It calls methods of graphic building or printing data in console,
 *  build required for it objects.
 */
fun getStatistics(textData: TextData) {

    if (!textData.haveText()) {
        println("No saved texts")
        return
    }

    val text = textData.getTextData("Input name of a text which you want to see statistics about.")

    var counter = 1
    val wordsCountsMap = text.getSentencesList().map { counter++ to it.getWordsCount() }

    println("Print \"console\" if you have see data in console, \"graphic\" if you have see histogram and \"both\" if you have see them together:")

    val request = requestInput(listOf("console", "graphic", "both"))
    request.first.exe()

    when (request.second) {
        "console" -> printStatisticsInConsole(text.getName(), wordsCountsMap.toMap())
        "graphic" -> buildGraphic(text.getName(), wordsCountsMap.toMap())
        "both" -> {
            printStatisticsInConsole(text.getName(), wordsCountsMap.toMap())
            buildGraphic(text.getName(), wordsCountsMap.toMap())
        }
    }

}

/** Builds bar chart with data of mapOfSentenceNumToItsSize and saves image in file.
 *  @param textName name of texts, which user want to see statistics about.
 *  @param mapOfSentenceNumToItsSize map of pairs of sentence numbers and their words count.
 */
private fun buildGraphic(textName: String, mapOfSentenceNumToItsSize: Map<Int, Int>) {

    val data = mapOf(
        "words count" to mapOfSentenceNumToItsSize.map { it.value.toFloat() / mapOfSentenceNumToItsSize.size },
    )

    val fig = ggplot(data) +
            geomBar(
                color = "white",
                fill = "red"
            ) { x = "words count" } +
            geomArea(
                stat = Stat.density(),
                color = "white",
                fill = "pink",
                alpha = 0.4
            ) { x = "words count" } +
            ggsize(1400, 800)

    println("Graphic was save in ${ggsave(fig, "$textName.png")}")
    fig.show()

}

/** Prints statistics according to data from mapOfSentenceNumToItsSize.
 *  @param textName name of texts, which user want to see statistics about.
 *  @param mapOfSentenceNumToItsSize map of pairs of sentence numbers and their words count.
 */
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
            |[total number of sentences]:
            |--- ${mapOfSentenceNumToItsSize.size}
            ||[total number of words]:
            |--- $totalWordsCount
            |[num of sentence: count of words in it]:
            |--- ${mapOfSentenceNumToItsSize.toList().joinToString("; ") { "${it.first} contains ${it.second}" }}.
            """.trimMargin()
    )
    println("-".repeat(sectionTitle.length))
    println("Done!\n")
}


/** Asks the user which where they want to read the text from and
 *  calls special for file- and console- reading methods according the answer.
 */
fun readNewText(textData: TextData) {

    println("Where do you want to add the text: from the console or a file?")

    val kindOfSource = requestInput(listOf("console", "file"))
    kindOfSource.first.exe()

    when (kindOfSource.second) {
        "console" -> readFromConsole(textData)
        "file" -> readFromFile(textData)
    }
}

/** Read from console the name of text, checks that it hasn't saved yet and its contents,
 *  asks if entered text is not correct and re-calls itself or
 *  calls method of adding received text to data.
 */
private fun readFromConsole(textData: TextData) {

    println("Input name of a text:")
    if (textData.haveText()) println("Unavailable(existing) names: ${textData.getTextNamesInString()}.")
    val nameRequest = requestInput(unavailableInputs = textData.getTextNamesList())
    nameRequest.first.exe()

    val name = nameRequest.second.toString()

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

    val correctInputRequest = requestInput(listOf("yes", "no"))
    correctInputRequest.first.exe()

    if (correctInputRequest.second == "yes") addTextToData(textData, name, content)
    else readFromConsole(textData)
}

/** Asks for the name of text, path to file to read text from,
 *  checks for its existing, reads contents and,
 *  asks if entered names is not correct and re-calls itself or
 *  calls method of adding received text to data.
 */
private fun readFromFile(textData: TextData) {

    println("Input a name of a text:")
    val name = readln()

    println("Input path to file:")
    val contentsFile: File

    pathReadCycle@ while (true) {
        val pathRequest = requestInput<String>()
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
    val correctInputRequest = requestInput(listOf("yes", "no"))
    correctInputRequest.first.exe()

    when (correctInputRequest.second) {
        "yes" -> addTextToData(textData, name, content)
        "no" -> readFromFile(textData)
    }
}

/**
 * Calls method of TextData class to add new text in set
 * of tracked texts.
 * @param textName name of new text.
 * @param content content of new text.
 */
private fun addTextToData(textData: TextData, textName: String, content: String) =
    textData.addNewText(textName, content)


/** Stores data about tracking texts,
 *  includes methods for work with them due the adding.
 */
class TextData {

    /** list of monitored texts. */
    private val textsList = mutableListOf<Text>()

    fun haveText(): Boolean = textsList.isNotEmpty()

    /** Method for removing text from watch list. Asks for a name of a text
     *  and if it's being tracked, remove it from the textsList.
     */
    fun removeText() {

        if (!haveText()) {
            println("No saved texts")
            return
        }

        val removingText = getTextData("Input name of text which you want to remove.")
        textsList.remove(removingText)

        println("Text ${removingText.getName()} removed.")
    }

    /** Returns Text object whose name matches searchingName or null
     *  if there is no text with same name in the textsList.
     *  @param searchingName name of the text to be found.
     *  @return the text with searchingName name or null.
     */
    private fun getTextByName(searchingName: String): Text? {

        for (text in textsList) if (text.getName() == searchingName) return text
        return null
    }

    /** @return string with names of tracking texts separated by delimiter. */
    fun getTextNamesInString(delimiter: String = ", "): String {
        return textsList.joinToString(delimiter) { it.getName() }
    }


    /** @return list with names of tracking texts. */
    fun getTextNamesList(): List<String> {
        return textsList.map { it.getName() }
    }


    /** Calls method of textAnalyzer for getting Text object from
     *  textName and content and adds it in textsList.
     */
    fun addNewText(textName: String, content: String) {
        textsList.add(TextAnalyzer.getTextObjFromContents(textName, content))
    }


    /**
     * Reads name of the text to be found and returns it text if
     * it exists.
     * @param message message which prints with calling this method.
     * @return Text type object
     */
    fun getTextData(message: String = "Input name of a text"): Text {


        println(message)
        println("Saved texts: ${getTextNamesInString()}.")

        val nameRequest = requestInput(getTextNamesList())
        nameRequest.first.exe()

        return getTextByName(nameRequest.second.toString())!!
    }


    /** Object used for getting text from the string information. */
    object TextAnalyzer {

        private val DELIMITERS = Regex("[!?.]+\\s+")
        private val WHITESPACES = Regex("\\s+")
        private val WHITESPACES_OR_EMPTY = Regex("(\\s+)?")

        /**
         * Receives text as input and splits it by sentence, calculate its lengths,
         * create list of Sentence objects and returns Text object, created with
         * this list, input field name, and count of sentences in content.
         */
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


    /** Stores information about conjugated text. */
    data class Text(
        private val name: String,
        private val sentencesCount: Int,
        private val sentencesList: List<Sentence>,
    ) {

        fun getName() = name

        fun getSentencesList() = sentencesList


        /** Stores information about count of words. */
        data class Sentence(private val wordsCount: Int) {
            fun getWordsCount() = wordsCount
        }
    }

}


/** Function used for exiting out of program. */
fun exit(): Nothing {
    println("bye!")
    exitProcess(0)
}


/** Reads commands from the console and storing data about
 *  the functions they should execute.
 */
class CommandCenter(
    private val textData: TextData,
) {

    private val exitCommand = Command("exit", ::exit)
    private val addCommand = Command("add text") { readNewText(textData) }
    private val showStatisticsCommand = Command("show statistics") { getStatistics(textData) }
    private val removeTextCommand = Command("remove text") { textData.removeText() }

    private val commandsList = listOf(exitCommand, addCommand, showStatisticsCommand, removeTextCommand)
    private val commandsNames = commandsList.map { it.name }

    /** Stores command and its name */
    class Command(val name: String, val executingFun: () -> Unit)

    /** Prints list of names of available commands, requests the name and
     *  returns corresponding to entered name function.
     */
    fun readCommandFromConsole(): () -> Unit {

        println("Input one of the commands: ${commandsNames.joinToString()}:")

        val commandNameRequest = requestInput(commandsNames)
        commandNameRequest.first.exe()

        val funName = commandNameRequest.second.toString()

        return commandsList.find { it.name == funName }!!.executingFun
    }

}


/** Custom exception being thrown if user want to return to the menu. */
class ReturnException : Exception()

/** Custom exception being thrown if input can't be converted
 *  to the requested type.
 */
class InvalidInputTypeException : Exception()

/** Custom exception being thrown if input doesn't match the list
 *  of possible values.
 */
class InvalidElemInInputException : Exception()


/** Function repeating the process of calling functions returned by
 *  CommandCenter. If ReturnException was thrown, it is caught here,
 *  last iteration breaks and new one is called.
 */
fun workCycle(commandCenter: CommandCenter) {
    mainCycle@ while (true) {
        try {
            (commandCenter.readCommandFromConsole())()
        } catch (e: ReturnException) {
            println("You have been returned in main menu.")
            continue@mainCycle
        }
    }
}


/** Function reads from console input, check if it isn't available, repeat the request from the console
 *  in this case and convert input in type T if possible, else throws an exception and repeat the
 *  request.
 *
 *  @param availableInputs list of available values, the choice among which
 *  is requested from the console at some stage of the program.
 *  @param unavailableInputs list of unavailable values.
 *  @return a pair of function, which could be called after the request from this function to return
 *  if user want it or continue and value that the user has selected from the list of available.
 */
inline fun <reified T> requestInput(
    availableInputs: List<T>? = null,
    unavailableInputs: List<T>? = null
): Pair<InputOutcomeCommand, Any> {

    var inputT: Any

    readingAndChangingTypeCycle@ while (true) {

        val input = readln().trim()
        if (input == "return") return Pair(ReturnCommand(), "")

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
                    } else throw InvalidInputTypeException()
                }

                typeOf<String>() -> input
                else -> throw InvalidInputTypeException()
            }

            return when {
                availableInputs != null && unavailableInputs != null -> {
                    if (inputT.toString() in availableInputs.map { it.toString() } &&
                        inputT.toString() !in unavailableInputs.map { it.toString() }) Pair(ContinueCommand(), inputT)
                    else throw InvalidElemInInputException()
                }

                availableInputs != null -> {
                    if (inputT.toString() in availableInputs.map { it.toString() }) Pair(ContinueCommand(), inputT)
                    else throw InvalidElemInInputException()
                }

                unavailableInputs != null -> {
                    if (inputT.toString() !in unavailableInputs.map { it.toString() }) Pair(ContinueCommand(), inputT)
                    else throw InvalidElemInInputException()
                }

                else -> Pair(ContinueCommand(), inputT)
            }


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


/** Interface used to existing commands objects, whose exe value
 *  stores function which calls after requesting from console in
 *  requestInput function.
 */
interface InputOutcomeCommand {
    /** Function returned in a Main.requestInput method for returned to
     *  main menu of application or continuation of the process.
     */
    val exe: () -> Unit
}

/** If called exe of this object, program continue executing
 * without changes.
 */
class ContinueCommand : InputOutcomeCommand {
    override val exe: () -> Unit = {}
}

/** If called exe of this object, a custom ReturnException is thrown
 *  and process of executing some called command interrupted. Exception
 *  catches in mainCycle and program command request is repeated.
 */
class ReturnCommand : InputOutcomeCommand {
    override val exe: () -> Unit = throw ReturnException()
}


fun main() {

    val textData = TextData()

    val commandCenter = CommandCenter(textData)

    workCycle(commandCenter)
}
