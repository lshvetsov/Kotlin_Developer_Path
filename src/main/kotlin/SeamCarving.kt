
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.math.BigDecimal
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

object SeamCarvingRunner {
    private val simCarver = SeamCarver()
    private val inputImage = println("Please enter the absolute path to you image").let { readln() }
    private val outputImage = println("Please enter the absolute path where to save the processed image").let { readln() }
    private val operation = println("Choose the operation with file:\\n1. Negativize;\\n2. Energize;\\n3. Draw a seam;\\n4. Resize")
        .let { readln().toIntOrNull() }

    fun run() {
        ImageIO.read(File(inputImage)).let {
            when (operation) {
                1 -> ImageIO.write(simCarver.negativePhoto(it), "png", File(outputImage))
                2 -> ImageIO.write(simCarver.energizePhoto(it), "png", File(outputImage))
                3 -> ImageIO.write(simCarver.drawSeam(it), "png", File(outputImage))
                4 -> {
                    val width = println("Please enter the number of seams to remove vertically").let { readln().toInt() }
                    val height = println("Please enter the number of seams to remove horizontally").let { readln().toInt() }
                    ImageIO.write(simCarver.resize(it, width, height), "png", File(outputImage))
                }
                else -> println("Wrong operation")
            }
        }
    }
}

class SeamCarver {

    fun drawScotland(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        val graphics = image.graphics
        graphics.color = Color(255,0,0)
        graphics.drawLine(0,0,width-1,height-1)
        graphics.drawLine(width-1,0,0,height - 1)

        return image
    }

    fun negativePhoto(photo: BufferedImage): BufferedImage {
        val image = BufferedImage(photo.width, photo.height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until photo.width) {
            for (y in 0 until photo.height) {
                image.setRGB(x, y, photo.getRGB(x, y).inv())
            }
        }
        return image
    }

    fun energizePhoto(photo: BufferedImage): BufferedImage {
        val image = BufferedImage(photo.width, photo.height, BufferedImage.TYPE_INT_RGB)
        val energy = energize(photo)
        val maxEnergy = energy.maxOf { row -> row.maxOf { it } }

        for (x in 0 until photo.width) {
            for (y in 0 until photo.height) {
                val intensity = ((BigDecimal.valueOf(255.0) * energy[x][y]) / maxEnergy).toInt()
                image.setRGB(x, y, Color(intensity, intensity, intensity).rgb)
            }
        }
        return image
    }

    /**
     * Seam with minimal energy is added to the image
     */
    fun drawSeam(inputImage: BufferedImage, transpose: Boolean = false): BufferedImage {
        val image = if (transpose) transpose(inputImage) else inputImage
        val resultImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        getSeam(image).forEach {
            resultImage.setRGB( it.first , it.second , Color(255, 0, 0).rgb)
        }
        return if (transpose) transpose(resultImage) else resultImage
    }

    fun resize(photo: BufferedImage, vSeamsToRemove: Int, hSeamsToRemove: Int): BufferedImage {
        var image = photo

        repeat(vSeamsToRemove) {
            getSeam(image).let {
                image = removeSeam(image, it)
            }
        }

        image = transpose(image)

        repeat(hSeamsToRemove) {
            getSeam(image).let {
                image = removeSeam(image, it)
            }
        }
        return transpose(image)
    }

    private fun removeSeam(image: BufferedImage, seam: List<Pair<Int, Int>>): BufferedImage {
        val newWidth = image.width - 1
        val newHeight = image.height
        val newImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until newHeight) {
            val (seamX, _) = seam[y]
            for (x in 0 until newWidth) {
                val newX = if (x < seamX) x else x + 1
                val color = image.getRGB(newX, y)
                newImage.setRGB(x, y, color)
            }
        }
        return newImage
    }

    private fun getSeam(inputImage: BufferedImage) : List<Pair<Int, Int>>  {
        val energy = energize(inputImage)
        val sum = getEnergySum(energy)
        return getSeamCoordinates(sum)
    }

    private fun transpose(photo: BufferedImage): BufferedImage {
        val image = BufferedImage(photo.height, photo.width, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                image.setRGB(x,y,photo.getRGB(y,x))
            }
        }
        return image
    }

    private fun energize(photo: BufferedImage): Array<Array<BigDecimal>> {
        val result = Array(photo.height) { Array(photo.width) { BigDecimal.ZERO } }
        for (i in 0 until photo.height)
            for (j in 0 until photo.width)
                result[i][j] = getEnergy(photo, j, i)
        return result
    }

    private fun getEnergySum(energy: Array<Array<BigDecimal>>): Array<Array<BigDecimal>> {
        if (energy.isEmpty()) return emptyArray()
        val height = energy.size
        val width = energy[0].size
        val sum = Array(height) { Array(width) { BigDecimal.ZERO } }

        sum[0] = energy[0]
        for (i in 1 until height) {
            for (j in 0 until width) {
                sum[i][j] = sum[i-1][j]
                if (j > 0 && sum[i-1][j-1] < sum[i][j]) sum[i][j] = sum[i-1][j-1]
                if (j < width - 1 && sum[i-1][j+1] < sum[i][j]) sum[i][j] = sum[i-1][j+1]
                sum[i][j] += energy[i][j]
            }
        }
        return sum
    }

    private fun getSeamCoordinates(sum: Array<Array<BigDecimal>>): List<Pair<Int, Int>> {
        if (sum.isEmpty()) return emptyList()
        val seamReversedCoordinates = mutableListOf<Pair<Int, Int>>()
        val height = sum.size
        val width = sum[0].size

        seamReversedCoordinates.add(sum[height - 1].indexOfFirst { it == sum[height - 1].minOrNull() } to height - 1)

        for (y in height - 2 downTo 0) {
            val prevX = seamReversedCoordinates.last().first
            val candidates = listOf(
                if (prevX > 0) sum[y][prevX - 1] else null,
                sum[y][prevX],
                if (prevX < width - 1) sum[y][prevX + 1] else null
            )
            val minIndex = candidates.filterNotNull()
                .minOrNull()
                .let { if (it == candidates[1]) prevX else if (it == candidates[0]) prevX - 1 else prevX + 1 }
            seamReversedCoordinates.add(minIndex to y)
        }

        return seamReversedCoordinates.reversed().toList()
    }

    private fun getEnergy(photo: BufferedImage, x: Int, y: Int): BigDecimal {
        fun getGradient(image: BufferedImage, x: Int, y: Int, isXGradient: Boolean): Double {
            val previousPixelColor = if (isXGradient) Color(image.getRGB(x - 1, y)) else Color(image.getRGB(x, y - 1))
            val nextPixelColor = if (isXGradient) Color(image.getRGB(x + 1, y)) else Color(image.getRGB(x, y + 1))
            return (previousPixelColor.red - nextPixelColor.red).toDouble().pow(2) +
                    (previousPixelColor.green - nextPixelColor.green).toDouble().pow(2) +
                    (previousPixelColor.blue - nextPixelColor.blue).toDouble().pow(2)
        }
        val xCoordinate = if (x == 0) 1 else if (x == photo.width - 1) photo.width - 2 else x
        val yCoordinate = if (y == 0) 1 else if (y == photo.height - 1) photo.height - 2 else y
        return BigDecimal(sqrt(getGradient(photo, xCoordinate, y, true) + getGradient(photo, x, yCoordinate, false)))
    }
}