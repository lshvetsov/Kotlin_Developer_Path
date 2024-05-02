import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

const val HYPER_METRO = "HyperMetro"
const val MATRIX = "Matrix"
const val PHONE_BOOK = "PhoneBook"
const val EXIT = "Exit"

fun main(args: Array<String>) {
    val parser = ArgParser("main")
    val project by parser.option(ArgType.String, description = "Project name").required()
    parser.parse(args)
    when (project) {
        HYPER_METRO -> HyperMetro.run("src/main/resources/london.json")
        MATRIX -> MatrixManipulator.run()
        PHONE_BOOK -> PhoneBookRunner.run()
        EXIT -> println("Bye!")
    }
}