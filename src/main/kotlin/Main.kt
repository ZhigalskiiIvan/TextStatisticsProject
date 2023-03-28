import java.io.File
import kotlin.system.exitProcess
import org.jetbrains.letsPlot.*
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.intern.Plot
import kotlin.reflect.typeOf


object StatisticBuilder {
    /**
     *  Singleton object which gives statistics of saved texts.
     */


    fun askAndExecuteSelfCommands() {
        /**
         *  Recognizes the name of the text, which
         *  user want to see statistics about and type of output.
         *  It calls methods of graphic building or printing data in console,
         *  build required for it objects.
         */

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
        /**
         *  It builds bar chart with data of mapOfSentenceNumToItsSize.
         *  textName: name of texts, which user want to see statistics about.
         *  mapOfSentenceNumToItsSize: map of pairs of sentence numbers and their words count.
         */

        val plot: Plot =
            ggplot(mapOfSentenceNumToItsSize) + ggsize(1000, 600) + geomBar { x = "sentence number"; y = "words count" }

        TODO("вил би сун")

    }

    private fun printStatisticsInConsole(textName: String, mapOfSentenceNumToItsSize: Map<Int, Int>) {
        /**
         *  Prints statistics according to data from mapOfSentenceNumToItsSize.
         *
         *  textName: name of texts, which user want to see statistics about.
         *  mapOfSentenceNumToItsSize: map of pairs of sentence numbers and their words count.
         */

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

}


object TextReader {
    /** Singleton object used for reading text from a file or console. */


    fun askAndExecuteSelfCommands() {
        /**
         * Asks the user which where they want to read the text from and
         * calls special for file- and console- reading methods according the answer.
         */


        println("Where do you want to add the text: from the console or a file?")

        val kindOfSource = Main.requestInput(listOf("console", "file"))
        kindOfSource.first.exe()

        when (kindOfSource.second) {
            "console" -> readFromConsole()
            "file" -> readFromFile()
        }
    }


    private fun readFromConsole() {
        /**
         * Read from console the name of text, checks that it hasn't saved yet and its contents,
         * asks if entered text is not correct and re-calls itself or
         * calls method of adding received text to data.
         */

        println("Input name of a text:")
        if (TextData.haveText()) println("Unavailable(existing) names: ${TextData.getTextNamesInString()}.")
        val nameRequest = Main.requestInput(unavailableInputs = TextData.getTextNamesList())
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

        val correctInputRequest = Main.requestInput(listOf("yes", "no"))
        correctInputRequest.first.exe()

        if (correctInputRequest.second == "yes") addTextToData(name, content)
        else readFromConsole()
    }

    private fun readFromFile() {
        /**
         * Asks for the name of text, path to file to read text from,
         * checks for its existing, reads contents and,
         * asks if entered names is not correct and re-calls itself or
         * calls method of adding received text to data.
         */


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
        /**
         * Calls method of TextData class to add new text in set
         * of tracked texts.
         * textName: name of new text.
         * content: content of new text.
         */

        TextData.addNewText(textName, content)
    }

}


object TextData {
    /**
     * Object used for storing data about tracking texts,
     * includes methods for work with them.
     * textsList: list of monitored texts.
     */


    private val textsList = mutableListOf<Text>()


    fun haveText(): Boolean = textsList.isNotEmpty()


    fun removeText() {
        /**
         * Method for removing text from watch list. Asks for a name of a text
         * and if it's being tracked, remove it from the textsList.
         */


        if (!haveText()) {
            println("No saved texts")
            return
        }

        val removingText = getTextData("Input name of text which you want to remove.")
        textsList.remove(removingText)

        println("Text ${removingText.getName()} removed.")
    }


    private fun getTextByName(searchingName: String): Text? {
        /**
         * Returns Text object whose name matches searchingName or null
         * if there is no text with same name in the textsList.
         * searchingName: name of the text to be found.
         * return: the text with searchingName name or null.
         */

        for (text in textsList) if (text.getName() == searchingName) return text
        return null
    }

    fun getTextNamesInString(delimiter: String = ", "): String {
        /** Returns string with names of tracking texts separated by delimiter. */
        return textsList.joinToString(delimiter) { it.getName() }
    }

    fun getTextNamesList(): List<String> {
        /** Returns list with names of tracking texts. */
        return textsList.map { it.getName() }
    }


    fun addNewText(textName: String, content: String) {
        /**
         * Calls method of textAnalyzer for getting Text object from
         * textName and content and adds it in textsList.
         */
        textsList.add(TextAnalyzer.getTextObjFromContents(textName, content))
    }


    fun getTextData(message: String = "Input name of a text"): Text {
        /**
         * Reads name of the text to be found and returns it text if
         * it exists.
         * message: message which prints with calling this method.
         * return: text
         */

        println(message)
        println("Saved texts: ${getTextNamesInString()}.")

        val nameRequest = Main.requestInput(getTextNamesList())
        nameRequest.first.exe()

        return getTextByName(nameRequest.second.toString())!!
    }


    object TextAnalyzer {
        /** Object used for getting text from the string information. */

        private val DELIMITERS = Regex("[!?.]+\\s+")
        private val WHITESPACES = Regex("\\s+")
        private val WHITESPACES_OR_EMPTY = Regex("(\\s+)?")

        fun getTextObjFromContents(name: String, content: String): Text {
            /**
             * Receives text as input and splits it by sentence, calculate its lengths,
             * create list of Sentence objects and returns Text object, created with
             * this list, input field name, and count of sentences in content.
             */

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
        /** Stores information about conjugated text. */

        fun getName() = name

        fun getSentencesList() = sentencesList


        data class Sentence(private val wordsCount: Int) {
            /** stores information about count of words. */
            fun getWordsCount() = wordsCount
        }
    }

}


fun exit(): Nothing {
    /** function used for exiting out of program. */
    println("bye!")
    exitProcess(0)
}


object CommandCenter {
    /**
     * Singleton used for reading commands from console and storing data about
     * the functions they should execute.
     */

    private enum class Commands(val executingFun: () -> Unit) {
        /**
         * Enumerating of available commands names and functions conjugated
         * with them.
         */
        EXIT(::exit),
        ADD_TEXT({ TextReader.askAndExecuteSelfCommands() }),
        SHOW_STATISTICS({ StatisticBuilder.askAndExecuteSelfCommands() }),
        REMOVE_TEXT({ TextData.removeText() })
    }

    fun readCommandFromConsole(): () -> Unit {
        /**
         * Prints list of names of available commands, requests the name and
         * returns corresponding to entered name function.
         */
        println("Input one of the commands: ${Commands.values().joinToString { it.name }}:")

        val commandNameRequest = Main.requestInput(Commands.values().map { it.name })
        commandNameRequest.first.exe()

        val funName = commandNameRequest.second.toString()

        return Commands.valueOf(funName).executingFun
    }

}


interface InputOutcomeCommand {
    /**
     * Interface used to existing commands objects, whose exe value
     * stores function which calls after request due the program.
     * exe: function returned in a Main.requestInput method for returned to
     * main menu of application or continuation of the process.
     */
    val exe: () -> Unit
}

object ContinueCommand : InputOutcomeCommand {
    /** If called exe of this object, program continue executing
     * without changes.
     */
    override val exe: () -> Unit = {}
}

object ReturnCommand : InputOutcomeCommand {
    /**
     * If called exe of this object, a custom ReturnException is thrown
     * and process of executing some called command interrupted. Exception
     * catches in mainCycle and program command request is repeated.
     */
    override val exe: () -> Unit = {
        throw ReturnException()
    }
}

class ReturnException : Exception() {
    /** Custom exception being thrown if user want to return to the menu. */
}

class InvalidInputTypeException : Exception() {
    /**
     * Custom exception being thrown if input can't be converted
     * to the requested type.
     */
}

class InvalidElemInInputException : Exception() {
    /**
     * Custom exception being thrown if input doesn't match the list
     * of possible values.
     */
}


object Main {
    /**
     * Object which contains request processing method and working body of
     * application.
     */

    fun workCycle() {
        /**
         * Method repeating the process of calling functions returned by
         * CommandCenter. If ReturnException was thrown, it is caught here,
         * last iteration breaks and new one is called.
         */
        mainCycle@ while (true) {
            try {
                (CommandCenter.readCommandFromConsole())()
            } catch (e: ReturnException) {
                println("You have been returned in main menu.")
                continue@mainCycle
            }
        }
    }

    inline fun <reified T> requestInput(
        availableInputs: List<T>? = null,
        unavailableInputs: List<T>? = null
    ): Pair<InputOutcomeCommand, Any> {
        /**
         * Method reads from console input, check if it isn't available, repeat the request from the console
         * in this case and convert input in type T if possible, else throws an exception and repeat the
         * request.
         *
         * availableInputs: list of available values, the choice among which
         * is requested from the console at some stage of the program.
         * unavailableInputs: list of unavailable values.
         * return: a pair of function, which could be called after the request from this function to return
         * if user want it or continue and value that the user has selected from the list of available.
         */

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
                        } else throw InvalidInputTypeException()
                    }

                    typeOf<String>() -> input
                    else -> throw InvalidInputTypeException()
                }

                return when {
                    availableInputs != null && unavailableInputs != null -> {
                        if (inputT.toString() in availableInputs.map { it.toString() } &&
                            inputT.toString() !in unavailableInputs.map { it.toString() }) Pair(ContinueCommand, inputT)
                        else throw InvalidElemInInputException()
                    }

                    availableInputs != null -> {
                        if (inputT.toString() in availableInputs.map { it.toString() }) Pair(ContinueCommand, inputT)
                        else throw InvalidElemInInputException()
                    }

                    unavailableInputs != null -> {
                        if (inputT.toString() !in unavailableInputs.map { it.toString() }) Pair(ContinueCommand, inputT)
                        else throw InvalidElemInInputException()
                    }

                    else -> Pair(ContinueCommand, inputT)
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

}


fun main() {
    Main.workCycle()
}