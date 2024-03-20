import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.io.path.isRegularFile

const val output = "/output"
const val append = "/append"
const val addHead = "/add-head"
const val remove = "/remove"
const val connect = "/connect"
const val route = "/route"
const val fastest = "/fastest-route"
const val exit = "/exit"
const val invalidArgument = "Invalid argument"
const val pathFindingException = "One of stations doesn't exist"

data class Station(val line: String, val name: String, val time: Int, val previous: List<String>, val next: List<String>, val transferTo: MutableList<Transfer>) {
    override fun toString(): String {
        val transfersString = transferTo.joinToString(transform = { "${it.destinationStation} (${it.destinationLine})" }, separator = ", ")
            .takeIf { it.isNotEmpty() }
            ?.let { " - $it" } ?: ""
        return "$name$transfersString"
    }
}
data class Transfer(val destinationLine: String, val destinationStation: String)

object HyperMetro {
    fun run(subway: String) {

        val result = runCatching { JsonParser.parseLinesFromJson(Path.of(subway)) }

        if (result.isFailure) {
            return
        }

        val subwaySystem = SubwaySystem(JsonParser.lines)

        while (true) {
            val input: List<String> = splitCommand(readlnOrNull() ?: return)
            when (input[0]) {
                output -> subwaySystem.printLine(input[1])
                append -> subwaySystem.addStationLast(
                    Station(
                        input[1],
                        input[2],
                        0,
                        mutableListOf(),
                        mutableListOf(),
                        mutableListOf()
                    )
                )

                addHead -> subwaySystem.addStationFirst(
                    Station(
                        input[1],
                        input[2],
                        0,
                        mutableListOf(),
                        mutableListOf(),
                        mutableListOf()
                    )
                )

                remove -> subwaySystem.removeStation(input[1], input[2])
                connect -> subwaySystem.connectLines(input[1], input[2], input[3], input[4])
                route -> subwaySystem.findPath(input[1], input[2], input[3], input[4])
                fastest -> subwaySystem.findFastestPath(input[1], input[2], input[3], input[4])
                exit -> return
                else -> println("Invalid command")
            }
        }
    }
}

object JsonParser {
    val lines: MutableMap<String, MutableMap<String, Station>> = mutableMapOf()
    fun parseLinesFromJson(file: Path) {
        if (!file.isRegularFile()) println("Error! Such a file doesn't exist!").also { throw Exception() }
        try {
            Files.newBufferedReader(file, StandardCharsets.UTF_8)
                .use { reader ->
                    val json: JsonElement = Gson().fromJson(reader, JsonElement::class.java)
                    json.asJsonObject.keySet().forEach { key -> lines[key] = parseLine(key, json.asJsonObject.get(key)) }
                }
        } catch (ex: Exception) {
            println("Incorrect file").also { throw Exception() }
        }
    }

    private fun parseLine(lineName: String, line: JsonElement): MutableMap<String, Station> {
        val result = mutableMapOf<String, Station>()
        line.asJsonArray.forEach{ stationJson ->
            val stationName = stationJson.asJsonObject.get("name").asString
            val time = when (val time = stationJson.asJsonObject.get("time")) {
                is JsonNull -> 0
                is JsonPrimitive -> time.asInt
                else -> 0
            }
            val previousStations = stationJson.asJsonObject.get("prev").asJsonArray.map { it.toString().trim('\"') }.toList()
            val nextStations = stationJson.asJsonObject.get("next").asJsonArray.map { it.toString().trim('\"') }.toList()
            val stationTransfers = stationJson.asJsonObject.get("transfer").asJsonArray
                ?.map { Transfer(it.asJsonObject.get("line").asString, it.asJsonObject.get("station").asString) }
                ?.toMutableList() ?: mutableListOf()
            result[stationName] = Station(lineName, stationName, time, previousStations, nextStations, stationTransfers)
        }
        return result
    }
}

fun splitCommand(input: String): List<String> {
    val command = Regex("\\/\\S*").find(input)?.value ?: throw IllegalStateException("Invalid command")
    if (command == exit) return listOf(command)
    var stringRemain = input.substring(command.length + 1)
    val args: List<String> = when (command) {
        connect, route, fastest -> {
            stringRemain.split("\" ", " \"").map { it.trim('"') }
        }
        output -> listOf(Regex("\"([^\"]*)\"").find(stringRemain)?.groupValues?.get(1) ?: "", "")
        else -> if (stringRemain.contains('"')) {
            val line = Regex("\"([^\"]*)\"").find(stringRemain)?.groupValues?.get(1) ?: ""
            stringRemain = stringRemain.substring(line.length + 3)
            val stationAndTime = stringRemain.split("\" ").ifEmpty { stringRemain.split(" ") }
            listOf(line, stationAndTime[0].trim('"'), if (stationAndTime.size > 1) stationAndTime[1].trim(' ') else "")
        } else {
            stringRemain.split(" ")
        }
    }
    return validateCommandArguments(listOf(command, *args.toTypedArray()))
}

fun validateCommandArguments(args: List<String>): List<String> {
    when (args[0]) {
        append, addHead -> check (args.size == 4) { invalidArgument }
        remove -> check (args.size == 4 && args[3] == "") { invalidArgument }
        output -> check (args.size == 3 && args[2] == "") { invalidArgument}
        connect, route -> check (args.size == 5) { invalidArgument }
    }
    return args
}

private class SubwaySystem(private val lines: MutableMap<String, MutableMap<String, Station>>) {
    fun addStationLast(station: Station) = lines[station.line]?.put(station.name, station)
    fun addStationFirst(station: Station) = this.addStationLast(station)
    fun removeStation(line: String, station: String) = lines[line]?.remove(station)
    fun connectLines(line1: String, station1: String, line2: String, station2: String) {
        Transfer(line2, station2).also { transfer ->  lines[line1]?.get(station1)?.transferTo?.add(transfer)}
        Transfer(line1, station1).also { transfer ->  lines[line2]?.get(station2)?.transferTo?.add(transfer)}
    }
    fun printLine(lineName: String, from: String = "", to: String = "") {
        val stations = lines[lineName]?.values ?: emptyList()
        val start = if (from.isNotEmpty()) stations.first { it.name == from } else stations.first()
        val end = if (to.isNotEmpty()) stations.first { it.name == to } else stations.last()
        var station = start.also { println(it) }
        while (station.next.isNotEmpty() && station.next[0] != end.name) {
            station = lines[lineName]?.get(station.next[0]) ?: break
            println(station)
        }
    }
    fun findPath(sourceLine: String, sourceStationName: String, destinationLine: String, destinationStationName: String) {
        validatePathRequest(sourceLine, sourceStationName, destinationLine, destinationStationName)
        val path = calculateBFS(sourceLine, sourceStationName, destinationLine, destinationStationName)
        printPath(path, false)
    }

    fun findFastestPath(sourceLine: String, sourceStationName: String, destinationLine: String, destinationStationName: String) {
        validatePathRequest(sourceLine, sourceStationName, destinationLine, destinationStationName)
        val path = calculateDijkstra(sourceLine, sourceStationName, destinationLine, destinationStationName)
        printPath(path)
    }

    private fun validatePathRequest(sourceLine: String, sourceStationName: String, destinationLine: String, destinationStationName: String) {
        val sourceStation = lines[sourceLine]?.get(sourceStationName)
        val destinationStation = lines[destinationLine]?.get(destinationStationName)
        check (sourceStation != null || destinationStation != null) { "No way to find a route as one of stations doesn't exist" }
        check(sourceStation != destinationStation) { "You are already on the destination station" }
    }
    private fun calculateBFS(sourceLine: String, sourceStationName: String, destinationLine: String, destinationStationName: String): List<Station> {

        val sourceStation = lines[sourceLine]?.get(sourceStationName) ?: throw IllegalArgumentException(pathFindingException)
        val destinationStation = lines[destinationLine]?.get(destinationStationName) ?: throw IllegalArgumentException(pathFindingException)

        if (sourceStation == destinationStation) return listOf(sourceStation)

        val visitedStations = mutableSetOf<Station>()
        val queueToVisit = ArrayDeque(listOf(sourceStation))
        val pathToReconstruct = mutableMapOf<Station, Station?>(Pair(sourceStation, null))

        var currentStation: Station

        while (queueToVisit.isNotEmpty()) {
            currentStation = queueToVisit.removeFirst()
                .also { visitedStations.add(it) }

            this.getNeighbours(currentStation)
                .filter { !visitedStations.contains(it) }
                .filter { !queueToVisit.contains(it) }
                .onEach { pathToReconstruct[it] = currentStation }
                .onEach { if (it == destinationStation) return reconstructPath(pathToReconstruct, it) }
                .forEach { queueToVisit.add(it) }
        }
        return emptyList()
    }

    private fun reconstructPath(visited: Map<Station, Station?>, destinationStation: Station): List<Station> {
        var pointer = destinationStation
        val result = mutableListOf(destinationStation)
        while (visited[pointer] != null) {
            val current = visited[pointer] ?: throw IllegalStateException()
            if (current.line != pointer.line) result.add(
                Station(current.transferTo.stream().filter {it.destinationLine == pointer.line}.map { it.destinationLine }.findFirst().orElse(current.line),
                    "Transfer", 5, mutableListOf(), mutableListOf(), mutableListOf()))
            result.add(current)
            pointer = current
        }
        return result
    }

    private fun calculateDijkstra(sourceLine: String, sourceStationName: String, destinationLine: String, destinationStationName: String): List<Station> {

        val toVisit = PriorityQueue<Pair<Station, Int>>(compareBy { it.second })
        val visited = mutableMapOf<Station, Int>()

        // Initialize starting station
        val startStation = lines[sourceLine]?.get(sourceStationName) ?: throw IllegalArgumentException(pathFindingException)
        val destinationStation = lines[destinationLine]?.get(destinationStationName) ?: throw IllegalArgumentException(pathFindingException)
        toVisit.add(startStation to 0)
            .also { visited[startStation] = 0 }

        // Exploration loop
        while (toVisit.isNotEmpty()) {
            val (currentStation, currentDistance) = toVisit.poll()

            // Reached the destination
            if (currentStation == destinationStation) {
                return reconstructPath(visited, currentStation, startStation)
            }

            // Explore connected stations
            this.getNeighbours(currentStation).forEach {
                val distance = if (it.line == currentStation.line) currentDistance + it.time else currentDistance + 5
                if (distance < visited.getOrDefault(it, Int.MAX_VALUE)) {
                    visited[it] = distance
                    toVisit.add(it to distance)
                }
            }
        }
        // No path found
        return emptyList()
    }

    private fun reconstructPath(visited: MutableMap<Station, Int>, endStation: Station, sourceStation: Station): List<Station> {
        val path = mutableListOf(endStation)
        var currentStation = endStation

        while (visited.keys.contains(currentStation)) {
            val previous = this.getNeighbours(currentStation).minByOrNull { visited[it] ?: Int.MAX_VALUE } ?: break
            if (currentStation.line != previous.line)
                path.add(Station(previous.transferTo.stream().filter {it.destinationLine == currentStation.line}.map { it.destinationLine }.findFirst().orElse(currentStation.line), "Transfer", 5, mutableListOf(), mutableListOf(), mutableListOf()))
            path.add(previous)
            if (previous == sourceStation) break
            currentStation = previous
        }
        return path
    }

    private fun getNeighbours(station: Station): List<Station> {
        val next = station.next.stream().map { name -> lines[station.line]?.get(name) ?: throw IllegalStateException(pathFindingException) }.toList() ?: emptyList()
        val previous = station.previous.stream().map { name -> lines[station.line]?.get(name) ?: throw IllegalStateException(pathFindingException) }.toList() ?: emptyList()
        val transfers = station.transferTo.stream()
            .map { lines[it.destinationLine]?.get(it.destinationStation) ?: throw IllegalStateException(pathFindingException) }
            .toList() ?: emptyList()
        return listOf(transfers, next, previous).flatten()
    }

    private fun printPath(path: List<Station>, calculateTime: Boolean = true) {
        path.reversed().joinToString("\n") { if (it.name == "Transfer") "Transition to line ${it.line}" else it.name }.also { println(it) }
        if (calculateTime) println("Total: ${this.calculateTime(path)} minutes in the way")
    }
    private fun calculateTime(path: List<Station>): Int {
        var result = 0
        var previous = path.last()
        var skipNext = false
        for (index in path.lastIndex - 1 downTo 0) {
            if (skipNext) {
                skipNext = false
                continue
            }
            val current = path[index]
            skipNext = current.name == "Transfer"
            result += if (previous.next.contains(current.name)) previous.time else current.time
            previous = current
        }
        return result
    }
}

