@file:OptIn(ExperimentalCli::class)

import java.io.File
import kotlin.system.exitProcess
import org.jetbrains.letsPlot.*
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.*
import kotlinx.cli.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.FileReader


val WHITESPACES = Regex("\\s+")


/** Builds bar chart with data of mapOfSentenceNumToItsSize and saves image in file.
 *  @param textName name of texts, which user want to see statistics about.
 *  @param mapOfSentenceNumToItsSize map of pairs of sentence numbers and their words count.
 */
fun buildGraphic(textName: String, listOfSentenceSizes: List<Int>) {

    val data = mapOf("words count" to listOfSentenceSizes.map { it.toFloat() })

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
fun printStatisticsInConsole(textName: String, mapOfSentenceNumToItsSize: Map<Int, Int>) {

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


/** Read from console the name of text, checks that it hasn't saved yet and its contents,
 *  asks if entered text is not correct and re-calls itself or
 *  calls method of adding received text to data.
 */
fun readFromConsole(textData: TextData, textName: String) {

    println("Input a text content(after input text press enter twice):")
    var content = ""

    var itWasNewParYet = false
    while (true) {
        val nextPart = readln()
        if (nextPart.isEmpty()) {
            if (itWasNewParYet) break
            else {
                content += "\n"
                itWasNewParYet = true
            }
        } else {
            itWasNewParYet = false
            content += "$nextPart\n"
        }
    }

    addTextToData(textData, textName, content)
}


/** Asks for the name of text, path to file to read text from,
 *  checks for its existing, reads contents and,
 *  asks if entered names is not correct and re-calls itself or
 *  calls method of adding received text to data.
 */
fun readFromFile(textData: TextData, textName: String, path: String) {

    println("Input path to file:")
    val contentsFile = File(path)
    val content = contentsFile.readText()

    addTextToData(textData, textName, content)
}

/**
 * Calls method of TextData class to add new text in set
 * of tracked texts.
 * @param textName name of new text.
 * @param content content of new text.
 */
fun addTextToData(textData: TextData, textName: String, content: String) =
    textData.addNewText(textName, content)


/** Stores data about tracking texts,
 *  includes methods for work with them due the adding.
 */
class TextData {

    /** list of monitored texts. */
    private val textsList: MutableSet<Text>

    init {
        textsList = JSONReaderWriter.readSavedTexts()
    }

    private fun haveText(): Boolean = textsList.isNotEmpty()

    /** Method for removing text from watch list. Asks for a name of a text
     *  and if it's being tracked, remove it from the textsList.
     */
    fun removeText(name: String) {

        val removingText = getTextByName(name)
        if (removingText == null) println("No text with name $name")
        else {
            textsList.remove(removingText)
            println("Text ${removingText.getName()} removed.")
            JSONReaderWriter.removeFromDirectory(name)
        }

    }


    /** Returns Text object whose name matches searchingName or null
     *  if there is no text with same name in the textsList.
     *  @param searchingName name of the text to be found.
     *  @return the text with searchingName name or null.
     */
    fun getTextByName(searchingName: String): Text? {

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
        val newText = TextAnalyzer.getTextObjFromContents(textName, content)
        textsList.add(newText)
        JSONReaderWriter.writeInJSON(newText)
    }


    private object JSONReaderWriter {
        private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
        private val savedTextsDirectory: File = File("savedTexts")
        private val savedTextsFiles: Array<File>

        init {
            if (!savedTextsDirectory.exists()) savedTextsDirectory.mkdir()
            savedTextsFiles =
                savedTextsDirectory.listFiles { _, filename ->
                    filename.split(".").last() == "json"
                } ?: emptyArray()
        }

        fun removeFromDirectory(name: String) {
            savedTextsFiles.find { it.name == name }?.delete() ?: println("No file with name $name in directory")
        }

        fun writeInJSON(text: Text) {
            val jsonFileText = gsonPretty.toJson(text)

            val jsonFile = File("${savedTextsDirectory.path}/${text.getName()}.json")
            jsonFile.createNewFile()
            jsonFile.writeText(jsonFileText)
        }

        fun readFromJSON(jsonFile: File): Text {
            return gsonPretty.fromJson(FileReader(jsonFile), Text::class.java)
        }

        fun readSavedTexts(): MutableSet<Text> {
            val textsList = mutableListOf<Text>()
            for (file in savedTextsFiles) {
                textsList.add(readFromJSON(file))
            }
            return textsList.toMutableSet()
        }

    }

    /** Object used for getting text from the string information. */
    private object TextAnalyzer {

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
class CommandCenter(private val textData: TextData) {


    private val parser = ArgParser(
        "Text Analyzer",
        useDefaultHelpShortName = true,
        strictSubcommandOptionsOrder = false
    )


    private val addNewText = Add()
    private val showStatistics = ShowStatistics()
    private val showTextList = ShowTextsList()
    private val removeTexts = RemoveTexts()

    private inner class RemoveTexts : Subcommand("remove", "Removing text from saved.") {
        private val removingList by argument(
            ArgType.String, "removing list",
            "Selection of texts to delete"
        ).vararg()

        override fun execute() {
            if (removingList.isEmpty()) {
                println("remove command needs arguments to work")
                return
            }
            val availableRemovingList = mutableListOf<String>()
            removingList.forEach {
                if (it in textData.getTextNamesList()) availableRemovingList.add(it)
                else println("No text $it in data")
            }

            availableRemovingList.forEach { textData.removeText(it) }

        }
    }

    private inner class ShowTextsList : Subcommand("list", "Showing tracking texts.") {
        override fun execute() {
            println("Saved texts list: ${textData.getTextNamesInString(", ")}")
        }
    }


    private inner class ShowStatistics : Subcommand("stat", "Showing statistics.") {

        private val textNames by argument(
            ArgType.String, "text names",
            "Names of texts which you want to show statistics about"
        ).vararg()

        private val outputLocation by option(
            ArgType.Choice(listOf("graphic", "console", "both"), { it }), "output-location", "o",
            "Choice where to show the statistics"
        ).default("console")

        override fun execute() {

            val availableSelectedTextNames = mutableListOf<String>()
            textNames.forEach {
                if (it in textData.getTextNamesList()) availableSelectedTextNames.add(it)
                else println("No text $it in data")
            }

            when (outputLocation) {
                "graphic" -> availableSelectedTextNames.forEach { callBuildingGraphic(it) }
                "console" -> availableSelectedTextNames.forEach { callConsoleStatisticsShowing(it) }
                "both" -> availableSelectedTextNames.forEach {
                    callBuildingGraphic(it)
                    callConsoleStatisticsShowing(it)
                }
            }
        }

        private fun callBuildingGraphic(textName: String) {
            val text = textData.getTextByName(textName)!!
            val listOfSentenceSizes = text.getSentencesList().map { it.getWordsCount() }
            buildGraphic(textName, listOfSentenceSizes)
        }

        private fun callConsoleStatisticsShowing(textName: String) {
            val text = textData.getTextByName(textName)!!
            var counter = 1
            val mapOfSentenceNumToItsSize = text.getSentencesList().associate { counter++ to it.getWordsCount() }
            printStatisticsInConsole(textName, mapOfSentenceNumToItsSize)
        }
    }


    private inner class Add : Subcommand("add", "Adding text from source") {
        private val path by option(
            ArgType.String, "source path", "s",
            "Inputting path to file for reading or \"console\" to reading from console"
        ).default("console")
        private var textName by argument(
            ArgType.String, "name",
            "Specifies the name of the new text"
        )

        override fun execute() {

            if (textName == "") textName = "untitled"
            when {
                path == "console" -> callReadingFromConsole()
                !File(path).exists() -> throw ReturnException("No file at $path")
                else -> callReadingFromFile()
            }
        }

        private fun callReadingFromConsole() = readFromConsole(textData, textName)

        private fun callReadingFromFile() = readFromFile(textData, textName, path)
    }


    fun parseArgsAndExecute(args: Array<String>) {
        parser.parse(args)
    }

    fun subcommandsInit() {
        parser.subcommands(addNewText, showStatistics, showTextList, removeTexts)
    }
}


/** Function repeating the process of calling functions returned by
 *  CommandCenter. If ReturnException was thrown, it is caught here,
 *  last iteration breaks and new one is called.
 */
fun readAndCallParsing(commandCenter: CommandCenter, mainArgs: Array<String>) {
    if (mainArgs.isNotEmpty()) commandCenter.parseArgsAndExecute(mainArgs)
    else {
        val args = readln().split(WHITESPACES).toTypedArray()
        try {
            commandCenter.parseArgsAndExecute(args)
        } catch (e: ReturnException) {
            println("message")
            exit()
        }
    }
}


/** Custom exception being thrown if user want to return to the menu. */
class ReturnException(message: String = "") : Exception(message)


fun main(args: Array<String>) {

    val textData = TextData()
    val commandCenter = CommandCenter(textData)
    commandCenter.subcommandsInit()

    readAndCallParsing(commandCenter, args)
}
