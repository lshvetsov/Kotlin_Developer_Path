const val HYPER_METRO = "HyperMetro"
const val EXIT = "Exit"

fun main(args: Array<String>) {
    println("Program arguments: ${args.joinToString()}")
    when (args[0]) {
        HYPER_METRO -> HyperMetro.run("src/main/resources/london.json")
        EXIT -> println("Bye!")
    }
}