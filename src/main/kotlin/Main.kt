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


private val DELIMITERS = Regex("[!?.]+\\s+")
private val WHITESPACES = Regex("\\s+")
private val WHITESPACES_OR_EMPTY = Regex("(\\s+)?")


/**
 * Builds bar chart with data of listOfSentenceSizes and saves image in file.
 *  @param textName name of text, which user want to see statistics about.
 *  @param listOfSentenceSizes list of sentences words count.
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

/**
 * Prints statistics according to data from mapOfSentenceNumToItsSize.
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
}


/**
 *  Read from console the text and calls function of adding new text to data.
 *  @param textData TextData object to add new text to.
 *  @param textName name of new text.
 */
fun readFromConsoleAndAddToData(textData: TextData, textName: String) {

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

/**
 *  Read text from file and calls function of adding text to data.
 *  @param textData TextData object to add new text to.
 *  @param textName name of new text.
 *  @param path path to the text file.
 */
fun readFromFileAndAddToData(textData: TextData, textName: String, path: String) {
    val contentsFile = File(path)
    val content = contentsFile.readText()

    addTextToData(textData, textName, content)
}

/**
 *  Calls method of TextData class to add new text in set
 *  of tracked texts.
 *  @param textData TextData object to add new text to.
 *  @param textName name of new text.
 *  @param content content of new text.
 */
fun addTextToData(textData: TextData, textName: String, content: String) =
    textData.addNewText(textName, content)


/**
 *  Stores data about tracking texts,
 *  includes methods for work with them due the adding.
 */
class TextData {

    /** List of monitored texts. */
    private val textsList: MutableSet<Text>

    init {
        // Initialization of previously saved and saved in JSON file texts
        textsList = JSONReaderWriter.readSavedTexts()
    }


    /**
     *  Removes text with name from watch list if it was there previously.
     *  @param name the name of text to be deleted.
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

    /**
     *  Calls method of textAnalyzer for getting Text object from
     *  textName and content, adds it in textsList and calls method of writing it in JSON.
     *  @param textName name of text to be added.
     *  @param content content of text.
     */
    fun addNewText(textName: String, content: String) {
        val newText = getTextObjFromContents(similarAvailableName(textName), content)
        textsList.add(newText)
        JSONReaderWriter.writeInJSON(newText)
    }

    /**
     *  If exists text in textList with name textName, chooses similar to it name with addition "(*)".
     *  @param textName the name to check for a match.
     *  @return name similar to textName.
     */
    private fun similarAvailableName(textName: String): String {
        val existingNames = textsList.map { it.getName() }
        if (textName !in existingNames) return textName

        val nameRegex = Regex("$textName\\(\\d+\\)")
        val busyNumbers = mutableSetOf<Int>()
        for (name in existingNames) {
            if (name.matches(nameRegex))
                busyNumbers.add(name.substring(name.indexOfLast { it == '(' }, name.indexOfFirst { it == ')' }).toInt())
        }

        var minimalAvailable = 1
        while (true) {
            if (minimalAvailable in busyNumbers) minimalAvailable++
            else break
        }

        return "$textName($minimalAvailable)"

    }


    /**
     *  Returns Text object from textList whose name matches searchingName or null
     *  if there is no text with same name in the textsList.
     *  @param searchingName name of the text to be found.
     *  @return the text with searchingName name or null.
     */
    fun getTextByName(searchingName: String): Text? {
        textsList.forEach { if (it.getName() == searchingName) return it }
        return null
    }

    /** @return string with names of tracking texts separated by delimiter. */
    fun getTextNamesInString(delimiter: String = ", "): String =
        textsList.joinToString(delimiter) { it.getName() }

    /** @return list with names of tracking texts. */
    fun getTextNamesList(): List<String> = textsList.map { it.getName() }


    /**
     * Receives text as input and splits it by sentence, calculate its lengths,
     * create list of Sentence objects and returns Text object, created with
     * this list, input field name, and count of sentences in content.
     * @param name name of text.
     * @param content string content of text.
     * @return Text object
     */
    private fun getTextObjFromContents(name: String, content: String): Text {

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


    /** Uses for writing and reading Text objects in JSON. */
    private object JSONReaderWriter {
        private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
        private val savedTextsDirectory: File = File("savedTexts")
        private val savedTextsFiles: Array<File>

        init {
            // initialization or creation directory with JSON files where Text objects were written.
            if (!savedTextsDirectory.exists()) savedTextsDirectory.mkdir()
            savedTextsFiles = savedTextsDirectory.listFiles { _, filename ->
                filename.split(".").last() == "json"
            } ?: emptyArray()
        }

        /**
         *  Removes file with name from directory with saved texts.
         *  @param name name of file to be deleted.
         */
        fun removeFromDirectory(name: String) =
            savedTextsFiles.find { it.name == name }?.delete() ?: println("No file with name $name in directory")

        /**
         *  Writes in JSON file text.
         *  @param text Text object to be written.
         */
        fun writeInJSON(text: Text) {
            val jsonFileText = gsonPretty.toJson(text)
            val jsonFile = File("${savedTextsDirectory.path}/${text.getName()}.json")

            jsonFile.createNewFile()
            jsonFile.writeText(jsonFileText)
        }

        /**
         *  @param jsonFile file to read from text object.
         *  @return Text object read from JSON file.
         */
        fun readFromJSON(jsonFile: File): Text = gsonPretty.fromJson(FileReader(jsonFile), Text::class.java)

        /**
         *  Calls method of reading from JSON for each json file from directory with saved texts.
         *  @return set of Text objects
         */
        fun readSavedTexts(): MutableSet<Text> {
            val textsList = mutableListOf<Text>()
            for (file in savedTextsFiles) textsList.add(readFromJSON(file))
            return textsList.toMutableSet()
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


/**
 *  Parses list of Strings in commands and its arguments,
 *  checks the correctness of the entered data and calls related functions.
 */
class CommandCenter(private val textData: TextData) {

    /**
     *  ArgParser object which can distribute arguments
     *  over declared options and subcommands.
     */
    private val parser = ArgParser(
        "Text Analyzer",
        useDefaultHelpShortName = true,
        strictSubcommandOptionsOrder = false
    )

    /** Subcommands initialization */
    private val addNewText = Add()
    private val showStatistics = ShowStatistics()
    private val showTextList = ShowTextsList()
    private val removeTexts = RemoveTexts()

    /** Calls function of removing texts from data. */
    private inner class RemoveTexts : Subcommand("remove", "Removing text from saved.") {

        /** Texts names list to be removed */
        private val removingList by argument(
            ArgType.String, "removing list",
            "Selection of texts to delete"
        ).vararg()

        /** @return list of available names from removingList */
        private fun selectAvailableNamesFromInputted(): List<String> {
            val availableInputs = mutableListOf<String>()

            removingList.forEach {
                if (it in textData.getTextNamesList()) availableInputs.add(it)
                else println("No text $it in data")
            }
            return availableInputs
        }


        /** Calls removing of texts with available names from data */
        private fun callRemovingFunctionForEachAvailableName() {
            val availableRemovingList = selectAvailableNamesFromInputted()
            availableRemovingList.forEach { textData.removeText(it) }
        }

        /** Called in time from parse function of Remove text object and calls removing */
        override fun execute() = callRemovingFunctionForEachAvailableName()
    }

    /** Calls printing list of saved texts in console. */
    private inner class ShowTextsList : Subcommand("list", "Showing tracking texts.") {

        /** Prints list of saved texts in string in console */
        private fun printTextListInConsole() = println("Saved texts list: ${textData.getTextNamesInString(", ")}")

        /** Called by parse function and calls printing of saved texts names */
        override fun execute() {
            printTextListInConsole()
        }
    }

    /** Calls functions of graphic building or showing statistics in console. */
    private inner class ShowStatistics : Subcommand("stat", "Showing statistics") {

        /** Names of texts for which you want to show statistics */
        private val textNames by argument(
            ArgType.String, "text names",
            "Names of texts which you want to show statistics about"
        ).vararg()

        /** Place where user can to see statistics */
        private val outputLocation by option(
            ArgType.Choice(listOf("graphic", "console", "both"), { it }), "output-location", "o",
            "Choice where to show the statistics"
        ).default("console")


        /** @return list of names from textNames which can be show statistics about */
        private fun selectAvailableInputFromInputted(): List<String> {
            val availableInputs = mutableListOf<String>()

            textNames.forEach {
                if (it in textData.getTextNamesList()) availableInputs.add(it)
                else println("No text $it in data")
            }
            return availableInputs
        }

        /**
         *  Calls showing of statistics depend on outputLocation
         *  for each text from texts with textNames
         */
        private fun callStatisticsShowingForEach(textNames: List<String>) {
            when (outputLocation) {
                "graphic" -> textNames.forEach { callBuildingGraphic(it) }
                "console" -> textNames.forEach { callConsoleStatisticsShowing(it) }
                "both" -> textNames.forEach {
                    callBuildingGraphic(it)
                    callConsoleStatisticsShowing(it)
                }
            }
        }

        /** Calls function of building graphic for text with textName */
        private fun callBuildingGraphic(textName: String) {
            val text = textData.getTextByName(textName)!!
            val listOfSentenceSizes = text.getSentencesList().map { it.getWordsCount() }
            buildGraphic(textName, listOfSentenceSizes)
        }

        /** Calls function of printing statistics for text with textName */
        private fun callConsoleStatisticsShowing(textName: String) {
            val text = textData.getTextByName(textName)!!
            var counter = 1
            val mapOfSentenceNumToItsSize = text.getSentencesList().associate { counter++ to it.getWordsCount() }
            printStatisticsInConsole(textName, mapOfSentenceNumToItsSize)
        }

        /** Calls function of calling for all available text names */
        private fun callStatistics() {
            val availableSelectedTextNames = selectAvailableInputFromInputted()
            callStatisticsShowingForEach(availableSelectedTextNames)
        }


        /** Calls showing statistics methods */
        override fun execute() {
            callStatistics()
        }
    }

    /** Calls functions of reading and adding text in saved. */
    private inner class Add : Subcommand("add", "Adding text from source") {

        /** Path to file for reading */
        private val path by option(
            ArgType.String, "source path", "s",
            "Inputting path to file for reading or \"console\" to reading from console"
        ).default("console")

        /** New text name */
        private val textName by argument(
            ArgType.String, "name",
            "Specifies the name of the new text"
        )

        /** Calculate method of input and calls reading functions */
        private fun callReading() = when {
            path == "console" -> callReadingFromConsole()
            !File(path).exists() -> throw IncorrectPathException(path)
            else -> callReadingFromFile()
        }

        /** Calls reading text with textName from console */
        private fun callReadingFromConsole() = readFromConsoleAndAddToData(textData, textName)

        /** Calls reading of text with textName from file with path */
        private fun callReadingFromFile() = readFromFileAndAddToData(textData, textName, path)

        /** Calls reading methods */
        override fun execute() = callReading()
    }


    /** Calls parse function of ArgParser object. */
    fun parseArgsAndExecute(args: Array<String>) = parser.parse(args)

    /** Calls initialization of subcommands. */
    fun subcommandsInit() = parser.subcommands(addNewText, showStatistics, showTextList, removeTexts)
}


/** Calls parsing of main args or if it is empty, read them from console */
fun readAndCallParsing(commandCenter: CommandCenter, mainArgs: Array<String>) {
    if (mainArgs.isNotEmpty()) commandCenter.parseArgsAndExecute(mainArgs)
    else {
        val args = readln().split(WHITESPACES).toTypedArray()
        try {
            commandCenter.parseArgsAndExecute(args)
        } catch (e: IncorrectPathException) {
            println(e.message)
            exit()
        }
    }
}


/** Custom exception being thrown if user enter incorrect path to file. */
class IncorrectPathException(path: String = "") : Exception("File at $path doesn't exist")

/** Function used for exiting out of program. */
fun exit(): Nothing {
    println("bye!")
    exitProcess(0)
}


fun main(args: Array<String>) {

    val textData = TextData()
    val commandCenter = CommandCenter(textData)
    commandCenter.subcommandsInit()

    readAndCallParsing(commandCenter, args)
}
