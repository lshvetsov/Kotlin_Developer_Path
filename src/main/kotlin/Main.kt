import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

const val HYPER_METRO = "HyperMetro"
const val EXIT = "Exit"

fun main(args: Array<String>) {
    val parser = ArgParser("main")
    val project by parser.option(ArgType.String, description = "Project name").required()
    parser.parse(args)
    when (project) {
        HYPER_METRO -> HyperMetro.run("src/main/resources/london.json")
        EXIT -> println("Bye!")
    }
}