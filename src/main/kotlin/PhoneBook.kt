import java.io.File
import java.util.Objects
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

object PhoneBookRunner {
    fun run() {
        val directoryPath = println("Please write the path to the phonebook directory").let { readln() }
        val searchListPath = println("Please write the path to the search list").let { readln() }
        PhoneBook(File(searchListPath).readLines(), directoryPath).run()
    }
}

class PhoneBook(listToFind: List<String>, directoryPath: String) {

    private val workingDirectory = directoryPath.substringBeforeLast("//")
    private val originalPhonebook: List<Contact> = File(directoryPath)
        .readLines()
        .map { Contact(it) }
        .toList()
    private var phonebook = originalPhonebook.toMutableList()
    private val searchList: List<String> = listToFind

    fun run() {
        val linearSearchTime = linearSearch()
        bubbleSortAndJumpSearch(linearSearchTime * 10)
        resetData()
        quickSortAndBinarySearch()
        resetData()
        searchHashTable()
    }

    private fun linearSearch(): Long {
        printInitialMessage(LinearSearchAlgorithm.name)
        val searchResult = search(LinearSearchAlgorithm)
        printSearchResult(searchResult.first, searchResult.second)
        return searchResult.second
    }

    private fun bubbleSortAndJumpSearch(timeout: Long) {
        printInitialMessage("${BubbleSortAlgorithm.name} + ${JumpSearchAlgorithm.name}")
        val sortTime: Long
        val searchResult: Pair<Int, Long>
        try {
            sortTime = sort(BubbleSortAlgorithm, timeout, false)
        } catch (ex: SortingTimeOutException) {
            searchResult = search(LinearSearchAlgorithm)
            printSortAndSearchResult(searchResult, ex.sortingTime, ex)
            return
        }
        searchResult = search(JumpSearchAlgorithm)
        printSortAndSearchResult(searchResult, sortTime)
    }

    private fun quickSortAndBinarySearch() {
        printInitialMessage("${QuickSortAlgorithm.name} + ${BinarySearchAlgorithm.name}")
        val sortTime = sort(QuickSortAlgorithm, null, false)
        val searchResult = search(BinarySearchAlgorithm)
        printSortAndSearchResult(searchResult, sortTime)
    }

    private fun searchHashTable() {
        printInitialMessage("hash table")
        val hashTable: ContactMap
        var counter = 0
        val sortTime = measureTimeMillis { hashTable = ContactMap(phonebook) }
        val searchTime = measureTimeMillis {
            searchList.forEach { hashTable.get(it)?.let { counter++ } }
        }
        printSortAndSearchResult(searchResult = Pair(counter, searchTime), sortTime = sortTime, sortPrefix = "Creating")
    }

    private fun search(searchAlgorithm: SearchAlgorithm): Pair<Int, Long> {
        var counter = 0
        val time = measureTimeMillis {
            searchList.forEach { searchAlgorithm.search(phonebook, it)?.let { counter++ } }
        }
        return Pair(counter, time)
    }

    private fun sort(sortAlgorithm: SortAlgorithm, timeout: Long?, safeToFile: Boolean): Long {
        val sortTime = measureTimeMillis { sortAlgorithm.sort(phonebook, timeout) }
        if (safeToFile) {
            File("$workingDirectory/sorted_directory.txt").writeText(phonebook.joinToString("\n"))
        }
        return sortTime
    }

    private fun resetData() {
        this.phonebook = originalPhonebook.toMutableList()
    }

    private fun printInitialMessage(type: String) = println("Start searching ($type)...")
    private fun printSearchResult(counter: Int, time: Long) = println("Found $counter / ${searchList.size} entries. Time taken: ${time.toMinutes()}.")
    private fun printTime(prefix: String, time: Long) = println("$prefix time: ${time.toMinutes()}")
    private fun printTimeWithException(prefix: String, time: Long, ex: Exception) = println("$prefix time: ${time.toMinutes()} - ${ex.message}")
    private fun printSortAndSearchResult(searchResult: Pair<Int, Long>, sortTime: Long, ex: Exception? = null, sortPrefix: String = "Sorting") {
        printSearchResult(searchResult.first, sortTime + searchResult.second)
        if (ex != null) printTimeWithException(sortPrefix, sortTime, ex) else printTime(sortPrefix, sortTime)
        printTime("Searching", searchResult.second)
    }
}

fun Long.toMinutes(): String = String.format("%02d min. %02d sec. %02d ms",
    TimeUnit.MILLISECONDS.toMinutes(this),
    TimeUnit.MILLISECONDS.toSeconds(this) % TimeUnit.MINUTES.toSeconds(1),
    TimeUnit.MILLISECONDS.toMillis(this) % TimeUnit.SECONDS.toMillis(1))

data class Contact (val phone: String, val name: String) {
    constructor(str: String) : this(str.substringBefore(" "), str.substringAfter(" "))
    override fun toString(): String {
        return "$phone $name"
    }
}

interface SearchAlgorithm {
    val name: String
    fun search(array: List<Contact>, name: String): Contact?
}

object LinearSearchAlgorithm : SearchAlgorithm {
    override val name = "linear search"
    override fun search(array: List<Contact>, name: String): Contact? = array.find { it.name == name }
}

object JumpSearchAlgorithm : SearchAlgorithm {
    override val name = "jump search"
    override fun search(array: List<Contact>, name: String): Contact? {
        val size = array.size
        val jump = sqrt(size.toDouble()).toInt()
        var prevIndex = 0
        for (i in jump until size step jump) {
            if (name < array[i].name) break
            prevIndex = i
        }
        for (i in prevIndex..minOf(prevIndex + jump, size - 1)) {
            if (array[i].name == name) return array[i]
        }
        return null
    }
}

object BinarySearchAlgorithm : SearchAlgorithm {
    override val name = "binary search"
    override fun search(array: List<Contact>, name: String): Contact? {
        if (array.isEmpty()) return null
        if (array.size == 1) return if (array[0].name == name) array[0] else null
        val key = array.size / 2
        if (array[key].name == name) return array[key]
        return if (name < array[key].name)
            search(array.subList(0, key), name)
        else
            search(array.subList(key + 1, array.size), name)
    }
}

interface SortAlgorithm {
    val name: String
    fun sort(array: MutableList<Contact>, timeout: Long?)
    fun swap(array: MutableList<Contact>, i: Int, j: Int) {
        if (i == j) return
        val temp = array[i]
        array[i] = array[j]
        array[j] = temp
    }
}

object BubbleSortAlgorithm : SortAlgorithm {
    override val name = "bubble sort"
    override fun sort(array: MutableList<Contact>, timeout: Long?) {
        val startTime = System.currentTimeMillis()
        val size = array.size
        for (i in 0 until size - 1) {
            for (j in 0 until size - i - 1) {
                if (array[j].name > array[j + 1].name) swap(array, j, j + 1)
                if (timeout != null && System.currentTimeMillis() - startTime > timeout){
                    val sortingTime = System.currentTimeMillis() - startTime
                    throw SortingTimeOutException(sortingTime, "STOPPED, moved to linear search")
                }
            }
        }
    }
}

object QuickSortAlgorithm : SortAlgorithm {
    override val name = "quick sort"
    override fun sort(array: MutableList<Contact>, timeout: Long?) {
        quickSort(array, 0, array.size - 1)
    }
    private fun quickSort(array: MutableList<Contact>, low: Int, high: Int) {
        if (high > low) {
            val pivot = partition(array, low, high)
            quickSort(array, low, pivot - 1)
            quickSort(array, pivot + 1, high)
        }
    }
    private fun partition(array: MutableList<Contact>, low: Int, high: Int): Int {
        val pivot = array[high]
        var storeIndex = low - 1
        for (j in low until high) {
            if (array[j].name < pivot.name) {
                storeIndex++
                swap(array, storeIndex, j)
            }
        }
        swap(array, storeIndex + 1, high)
        return storeIndex + 1
    }
}

class SortingTimeOutException(val sortingTime: Long, message: String) : Exception(message)

class ContactMap(phoneBook: List<Contact>) {
    private var buckets = 100000
    private var array = Array<ContactNode?>(buckets) { null }
    private var size = 0
    private fun getHashCode(key: Any): Int = Objects.hashCode(key)
    private fun getBucketIndex(key: Any): Int = abs(getHashCode(key) % buckets)
    private fun extend() {
        this.buckets *= 2
        this.size = 0
        val temp = array
        array = Array(buckets) { null }
        temp.filterNotNull().map { it.getChain() }.flatten().forEach { add(it.name, it) }
    }

    init {
        phoneBook.forEach { add(it.name, it)}
    }

    fun add(key: String, value: Contact) {
        val index = getBucketIndex(key)
        var node = array[index]
        if (node == null) array[index] = ContactNode(key, value)
        else {
            while (node!!.next != null) {
                if (node.key == key) {
                    node.value = value
                    return
                }
                node = node.next!!
            }
            node.next = ContactNode(key, value)
        }
        size++
        if (size / buckets > 0.7)
            extend()
    }

    fun get(key: String): Contact? {
        val index = getBucketIndex(key)
        var node: ContactNode? = array[index] ?: return null
        while (node != null) {
            if (node.key == key) return node.value
            node = node.next
        }
        return null
    }

    fun remove(key: String) {
        val index = getBucketIndex(key)
        var node = array[index] ?: return
        while (node.next != null) {
            if (node.key == key) {
                node.value = node.next!!.value
                node.next = node.next!!.next
                return
            }
            node = node.next!!
        }
        size--
    }

    fun size(): Int = size
    fun isEmpty(): Boolean = size == 0
    fun joinToString(separator: String) = array.joinToString(separator)
}

data class ContactNode(val key: String, var value: Contact, var next: ContactNode? = null) {
    fun getChain(): List<Contact> {
        val list = mutableListOf<Contact>()
        var node: ContactNode? = this
        while (node != null) {
            list.add(node.value)
            node = node.next
        }
        return list
    }
}