import java.util.Scanner
import kotlin.math.pow

object MatrixManipulator {
    fun run() = Matrix.menu()
}

class Matrix(private val rows: Int, private val columns: Int, indices: (Int, Int) -> Double) {
    private val matrix = List(rows) { i -> MutableList(columns) { j -> indices(i, j) } }

    object Exceptions {
        val OPERATON_EXCEPTION = "The operation cannot be performed."
        val MATRIX_INVERSION_EXCEPTION = "This matrix doesn't have an inverse."
        val GENERAL_ERROR = "Error"
    }

    companion object {
        fun menu() {
            while (true) {
                println("1. Add matrices\n2. Multiply matrix by a constant\n3. Multiply matrices\n4. Transpose matrix\n" +
                        "5. Calculate a determinant\n6. Inverse matrix\n0. Exit\nYour choice:")
                println(when (readln().toIntOrNull()) {
                    1 -> (readMatrix("first") + readMatrix("second"))
                    2 -> (readMatrix() * println("Enter constant:").let { readln().toDouble() })
                    3 -> (readMatrix("first") * readMatrix("second"))
                    4 -> println("1. Main diagonal\n2. Side diagonal\n3. Vertical line\n4. Horizontal line")
                        .also { println(when (readln().toInt()) {
                            1 -> readMatrix().mainTranspose()
                            2 -> readMatrix().sideDiagonalTranspose()
                            3 -> readMatrix().verticalTranspose()
                            4 -> readMatrix().horizontalTranspose()
                            else -> Exceptions.GENERAL_ERROR } ) }
                    5 -> readMatrix().determinant()
                    6 -> readMatrix().inverse()
                    0 -> break
                    else -> Exceptions.GENERAL_ERROR
                })
            }
        }
        private fun readMatrix(prefix: String? = null): Matrix {
            val message1 = if (prefix == null) "Enter size of matrix (rows columns):" else "Enter size of $prefix matrix(rows columns):"
            val message2 = if (prefix == null) "Enter matrix (row by row):" else "Enter $prefix matrix (row by row):"
            val (rows, columns) = println(message1)
                .run { readln().split(" ").map { it.toInt() } }
                .also { println(message2) }
            val scanner = Scanner(System.`in`)
            return Matrix(rows, columns) { _,_ -> scanner.nextDouble() }
        }
    }

    override fun toString() = "The result is:\n".plus(matrix.joinToString("\n") {
        it.joinToString(" ") { String.format("%.5f", it) }
    })

    operator fun plus(other: Matrix): Matrix {
        check(rows == other.rows && columns == other.columns) { Exceptions.OPERATON_EXCEPTION }
        return Matrix(rows, columns) { i,j -> matrix[i][j] + other.matrix[i][j] }
    }

    operator fun times(factor: Double) = Matrix(rows, columns) { i,j -> matrix[i][j] * factor }

    operator fun times(other: Matrix): Matrix {
        check(columns == other.rows) { Exceptions.OPERATON_EXCEPTION }
        return Matrix(rows, other.columns) { i,j ->
            other.matrix.indices.fold(0.0) { acc, c -> acc + matrix[i][c] * other.matrix[c][j] } }
    }

    private fun mainTranspose() = Matrix(rows, columns) { i, j -> matrix[j][i] }
    private fun horizontalTranspose() = Matrix(rows, columns) { i, j -> matrix[matrix.lastIndex - i][j] }
    private fun verticalTranspose() = Matrix(rows, columns) { i, j -> matrix[i][matrix.first().lastIndex - j] }
    private fun sideDiagonalTranspose() = this.horizontalTranspose().verticalTranspose().mainTranspose()

    private fun inverse(): Matrix {
        check(rows == columns) { Exceptions.MATRIX_INVERSION_EXCEPTION }
        val determinant = this.determinant()
        check(determinant != 0.0) { Exceptions.MATRIX_INVERSION_EXCEPTION }
        return this.getMinorMatrix().mainTranspose() * (1.0 / determinant)
    }

    fun determinant(): Double = this.calculateDeterminant(this.matrix)

    private fun calculateDeterminant(matrix: List<MutableList<Double>>, baseRow: Int = 0): Double {
        return when {
            matrix.isEmpty() -> throw Exception(Exceptions.GENERAL_ERROR)
            matrix.size == 1 -> matrix[0][0]
            matrix.size == 2 -> matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
            else -> matrix[baseRow].indices
                .map { Triple(it, matrix[baseRow][it], this.getCofactorMatrix(matrix, baseRow, it)) }
                .sumOf { this.calculateDeterminant(it.third) * (this.getSign(baseRow, it.first) * it.second) }
        }
    }

    private fun getSign(i: Int, j: Int) = (-1.0).pow(i + j)

    private fun getMinorMatrix(): Matrix =
        Matrix(rows, columns) { i,j -> this.calculateDeterminant(this.getCofactorMatrix(this.matrix, i, j)) * getSign(i,j) }

    private fun getCofactorMatrix(matrix: List<MutableList<Double>>, baseRow: Int, columnToExclude: Int): List<MutableList<Double>> {
        val subMatrix = List(matrix.size - 1) { MutableList(matrix.first().size - 1) { 0.0 } }
        for (i in matrix.indices) {
            for (j in matrix[0].indices) {
                if (i != baseRow && j != columnToExclude)
                    subMatrix[i - if (i > baseRow) 1 else 0][j - if (j > columnToExclude) 1 else 0] = matrix[i][j]
            }
        }
        return subMatrix
    }
}
