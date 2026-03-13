/*
 *     Copyright 2024 Manuel Daniel Dahmen
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package one.empty3.apps.masks

import one.empty3.apps.facedetect.jvm.Dimension
import one.empty3.apps.facedetect.jvm.DistanceBezier2
import one.empty3.library.Point3D
import one.empty3.libs.Image
import java.util.Arrays
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class DistanceProxLinear44_2(
    A: MutableList<Point3D?>?,
    B: MutableList<Point3D?>?,
    C: MutableList<Point3D?>?,
    aDimReal: Dimension?,
    bDimReal: Dimension,
    cDimReal: Dimension,
    opt1: Boolean,
    optimizeGrid: Boolean,
    jpgRight: Image
) : DistanceBezier2(A!!, B!!, aDimReal!!, bDimReal, opt1, optimizeGrid) {
    private val imageCB: Array<Array<Point3D>?>
    private val C: MutableList<Point3D?>
    private val cDimReal: Dimension
    private val imageAB: Array<Array<Point3D>?>
    private var pointsB: MutableList<Point3D?>? = null
    private var pointsA: MutableList<Point3D?>? = null
    lateinit var checkedListA: Array<BooleanArray?>
    private var pointsC: MutableList<Point3D?>? = null
    lateinit var checkedListC: Array<BooleanArray?>
    var computeTimeMax: Double = (3600 * 1000).toDouble()
    lateinit var checkedListB: Array<BooleanArray?>
    private lateinit var imageAB2: Array<Array<FloatArray>?>
    private lateinit var imageCB2: Array<Array<FloatArray>?>

    /***
     * Algorithme copier-coller de visages
     * @param A
     * @param B
     * @param aDimReal
     * @param bDimReal
     * @param opt1
     * @param optimizeGrid
     */
    init {

        var cDimReal: Dimension = cDimReal
        this.C = C!!

        //realSize(A!!, aDimReal!!)
        //realSize(B!!, bDimReal)
        //realSize(C!!, cDimReal)


        this.cDimReal = cDimReal
        imageAB = Array<Array<Point3D>?>((bDimReal.getWidth().toInt())) {
            arrayOfNulls<Point3D>(bDimReal.getHeight().toInt()) as Array<Point3D>?
        }
        imageCB = Array<Array<Point3D>?>((bDimReal.getWidth().toInt())) {
            arrayOfNulls<Point3D>(bDimReal.getHeight().toInt()) as Array<Point3D>?
        }
        setJpgRight(jpgRight)
        cDimReal = Dimension(jpgRight.width, jpgRight.height)
        if (cDimReal != null && !C!!.isEmpty()) init_1()
    }

    fun setComputeMaxTime(value: Double) {
        this.computeTimeMax = value
    }

    fun init_1() {

        pointsA = A.subList(0, A.size - 1)
        pointsB = B.subList(0, B.size - 1)
        pointsC = C.subList(0, C.size - 1)

        //realSize(pointsA!!, aDimReal);
        //realSize(pointsB!!, bDimReal);
        //realSize(pointsC!!, cDimReal);


        val newA: MutableList<Point3D?> = ArrayList<Point3D?>()
        val newB: MutableList<Point3D?> = ArrayList<Point3D?>()
        val newC: MutableList<Point3D?> = ArrayList<Point3D?>()
        val eps: Double = 1.0 / Math.min(aDimReal.getWidth(), aDimReal.getHeight())
        checkedListA = Array<BooleanArray?>(aDimReal.getWidth().toInt()) {
            BooleanArray(
                aDimReal.getHeight().toInt()
            )
        }
        checkedListB = Array<BooleanArray?>(bDimReal.getWidth().toInt()) {
            BooleanArray(
                bDimReal.getHeight().toInt()
            )
        }
        checkedListC = Array<BooleanArray?>(cDimReal.getWidth().toInt()) {
            BooleanArray(
                cDimReal.getHeight().toInt()
            )
        }
        val pointAddedA: Array<Array<Point3D?>?> =
            Array<Array<Point3D?>?>(aDimReal.getWidth().toInt()) {
                kotlin.arrayOfNulls<Point3D>(aDimReal.getHeight().toInt())
            }
        val pointAddedC: Array<Array<Point3D?>?> =
            Array<Array<Point3D?>?>(aDimReal.getWidth().toInt()) {
                kotlin.arrayOfNulls<Point3D>(aDimReal.getHeight().toInt())
            }
        val gen =
            Array<IntArray?>(aDimReal.getWidth().toInt()) { IntArray(aDimReal.getHeight().toInt()) }

        var maxDist = 1.0 / (1 / eps + 1)
        for (i in checkedListA.indices) {
            Arrays.fill(checkedListA[i], false)
        }
        for (i in checkedListC.indices) {
            Arrays.fill(checkedListC[i], false)
        }
        for (i in checkedListB.indices) {
            Arrays.fill(checkedListB[i], false)
        }
        var iteration = 1
        val N = 1 / eps
        var stepNewPoints = false
        var firstStep = true
        //while (maxDist > eps && (stepNewPoints || firstStep)) {
        var occ = -1
        var oldoccc = 0
        var surfaceOccupied = 0.1
        var surfaceOccupiedOld = 0.01
        var step = 0
        var sizeIndexStart = 0


        val timeStart = System.currentTimeMillis()

        var timeElapsed: Long = 0

        var ended = false

        while (occ != oldoccc && !ended) {
            oldoccc = occ
            stepNewPoints = false
            firstStep = false
            maxDist = 0.0
            val sizeA = pointsA!!.size
            val sizeB = pointsB!!.size
            val sizeC = pointsC!!.size
            for (i in 0..<sizeA) {
                if (iteration == 0) {
                    var i1_0 = (pointsA!!.get(i)!!.getX() * aDimReal.getWidth()).toInt()
                    var i2_0 = (pointsA!!.get(i)!!.getY() * aDimReal.getHeight()).toInt()
                    var k1_0 = (pointsC!!.get(i)!!.getX() * cDimReal.getWidth()).toInt()
                    var k2_0 = (pointsC!!.get(i)!!.getY() * cDimReal.getHeight()).toInt()
                    i1_0 = Math.max(0, Math.min(i1_0, aDimReal!!.width.toInt()  - 1))
                    i2_0 = Math.max(0, Math.min(i2_0, aDimReal!!.height.toInt() - 1))
                    k1_0 = Math.max(0, Math.min(k1_0, cDimReal!!.width.toInt()  - 1))
                    k2_0 = Math.max(0, Math.min(k2_0, cDimReal!!.height.toInt() - 1))

                    gen[i1_0]!![i2_0] = iteration
                    pointAddedA[i1_0]!![i2_0] = pointsA!!.get(i)
                    pointAddedC[k1_0]!![k2_0] = pointsC!!.get(i)
                    checkedListA[i1_0]!![i2_0] = true
                    var j1 = (pointsB!!.get(i)!!.getX() * bDimReal.getWidth()).toInt()
                    var j2 = (pointsB!!.get(i)!!.getY() * bDimReal.getHeight()).toInt()
                    j1 = Math.max(0, Math.min(j1, bDimReal.width.toInt()   - 1))
                    j2 = Math.max(0, Math.min(j2, bDimReal.height.toInt()  - 1))

                    imageAB[j1]!![j2] = pointsA!!.get(i)!!
                    imageCB[j1]!![j2] = pointsC!!.get(i)!!
                    newA.add(pointsA!!.get(i))
                    newB.add(pointsB!!.get(i))
                    newC.add(pointsC!!.get(i))
                    continue
                }
                val candidatesA: MutableList<Point3D?> = ArrayList<Point3D?>()
                val candidatesB: MutableList<Point3D?> = ArrayList<Point3D?>()
                val candidatesC: MutableList<Point3D?> = ArrayList<Point3D?>()
                var distCand = Double.Companion.MAX_VALUE
                // Find the nearest mapped point
                var norm = Double.Companion.MAX_VALUE
                for (k in sizeIndexStart..<sizeA) {
                    timeElapsed = System.currentTimeMillis() - timeStart
                    if (timeElapsed > computeTimeMax) { // 1 min max.
                        ended = true
                        Logger.getAnonymousLogger().severe("End of time DistanceProxLinear44")
                        break
                    }
                    if (k == i) continue
                    norm = pointsA!!.get(i)!!.moins(pointsA!!.get(k)).norme()
                    if (norm < distCand && norm > 0)  // &&
                    //gen[(int) pointsA.get(i).getX()][(int) pointsA.get(i).getY()] == gen[(int) pointsA.get(k).getX()][(int) pointsA.get(k).getY()]) {
                    {
                        distCand = norm
                        candidatesA.add(pointsA!!.get(k))
                        candidatesB.add(pointsB!!.get(k))
                        candidatesC.add(pointsC!!.get(k))
                    }
                }
                var m = 0
                var checked = 0
                val CHECKED_MAX = 3
                while (checked < CHECKED_MAX && !candidatesA.isEmpty() && candidatesA.size - m > 0) {
                    val indexA = candidatesA.size - m - 1
                    val indexB = candidatesB.size - m - 1
                    val indexC = candidatesC.size - m - 1
                    val candidateA: Point3D? = candidatesA.get(indexA)
                    val candidateB: Point3D? = candidatesB.get(indexB)
                    val candidateC: Point3D? = candidatesC.get(indexC)

                    val pC: Point3D = (pointsC!!.get(i)!!.plus(candidateC)).mult(0.5)
                    val pB: Point3D = (pointsB!!.get(i)!!.plus(candidateB)).mult(0.5)
                    val pA: Point3D = (pointsA!!.get(i)!!.plus(candidateA)).mult(0.5)


                    var i1 = (pA.getX() * aDimReal.getWidth()).toInt()
                    var i2 = (pA.getY() * aDimReal.getHeight()).toInt()
                    var j1 = (pB.getX() * bDimReal.getWidth()).toInt()
                    var j2 = (pB.getY() * bDimReal.getHeight()).toInt()
                    var k1 = (pC.getX() * cDimReal.getWidth()).toInt()
                    var k2 = (pC.getY() * cDimReal.getHeight()).toInt()

                    i1 = Math.max(0, Math.min(i1, aDimReal.getWidth().toInt() - 1))
                    i2 = Math.max(0, Math.min(i2, aDimReal.getHeight().toInt() - 1))
                    j1 = Math.max(0, Math.min(j1, bDimReal.getWidth().toInt() - 1))
                    j2 = Math.max(0, Math.min(j2, bDimReal.getHeight().toInt() - 1))
                    k1 = Math.max(0, Math.min(k1, cDimReal.getWidth().toInt() - 1))
                    k2 = Math.max(0, Math.min(k2, cDimReal.getHeight().toInt() - 1))


                    if (!checkedListB[j1]!![j2]) {
                        checkedListA[i1]!![i2] = true
                        checkedListC[k1]!![k2] = true
                        checkedListB[j1]!![j2] = true
                        //checkedListRight[]
                        stepNewPoints = true
                        newA.add(pA)
                        newB.add(pB)
                        newC.add(pC)
                        imageAB[j1]!![j2] = pA
                        imageCB[j1]!![j2] = pC
                        gen[i1]!![i2] = iteration // i1, i2 are correct here
                        pointAddedA[i1]!![i2] = pA // i1, i2 are correct here
                        pointAddedC[i1]!![i2] = pC // Use k1, k2 for pointAddedC
                        checked++
                    }
                    m++
                }
            }

            sizeIndexStart = pointsA!!.size

            pointsA!!.addAll(newA)
            pointsB!!.addAll(newB)
            pointsC!!.addAll(newC)

            newA.clear()
            newB.clear()
            newC.clear()

            iteration++
            occ = 0
            for (i in checkedListB.indices) {
                for (j in checkedListB[i]!!.indices) {
                    if (checkedListB[i]!![j]) {
                        occ++
                    }
                }
            }

            step++

            surfaceOccupiedOld = surfaceOccupied
            surfaceOccupied = 1.0 * occ / imageAB.size / imageAB[0]!!.size
            println(
                ("Number of points : " + pointsA!!.size + "\n\t" + "Iterations : " + iteration + "\n\toccupied (%) : " + surfaceOccupied)
            )
            println("Thread n°" + Thread.currentThread().getId())
        }
        if (occ == oldoccc) {
            println("occ==occMax Termination nature.")
        }
        if (ended) {
            println("realComputeTime>theoreticComputeTime Termination au temps.")
        }
        println("Compute texturing ended 1/2")
        println("Next: fills arrays avoiding blanks ")

        imageAB2 = fillArray(imageAB)
        imageCB2 = fillArray(imageCB)
        println("Compute texturing ended 2/2")
    }

    private fun realSize(x: MutableList<Point3D?>, dim: Dimension) {
        for (i in x.indices) {
            x.set(
                i, x.get(i)!!.multDot(Point3D(1/dim.width, 1/dim.height, 0.0))
            )
        }
    }

    fun fillArray(image: Array<Array<Point3D>?>): Array<Array<FloatArray>?> {
        val p2: Array<Array<FloatArray>?> = Array<Array<FloatArray?>?>(image.size) {
            Array<FloatArray?>(
                image[0]!!.size
            ) { FloatArray(2) }
        } as Array<Array<FloatArray>?>

        for (i in image.indices) {
            for (j in image[i]!!.indices) {
                if (image[i]!![j] != null) {
                    p2[i]!![j][0] = image[i]!![j].getX() .toFloat()
                    p2[i]!![j][1] = image[i]!![j].getY() .toFloat()
                } else {
                    val p: Point3D = searchNeighbours(i, j, image)
                    p2[i]!![j][0] = p.getX() .toFloat()
                    p2[i]!![j][1] = p.getY() .toFloat()
                }
            }
        }

        return p2
    }

    private fun searchNeighbours(i: Int, j: Int, image: Array<Array<Point3D>?>): Point3D {
        for (dist in 0..999) {
            for (c in 0..3) {
                var incrI = 0
                var incrJ = 0
                when (c) {
                    0 -> {
                        incrI = 1
                        incrJ = 0
                    }

                    1 -> {
                        incrI = 0
                        incrJ = 1
                    }

                    2 -> {
                        incrI = -1
                        incrJ = 0
                    }

                    3 -> {
                        incrI = 0
                        incrJ = -1
                    }
                }
                if (i + incrI >= 0 && i + incrI < image.size && j + incrJ >= 0 && j + incrJ < image[i]!!.size && image[i + incrI]!![j + incrJ] != null) {
                    return image[i + incrI]!![j + incrJ]
                }
            }
        }
        return Point3D.O0
    }

    public override fun findAxPointInB(u: Double, v: Double): Point3D {
        return findAxPointInBa12(u, v)
    }

    fun findAxPointInBa11(u: Double, v: Double): Point3D? {
        val floats =
            imageAB2[(u * bDimReal.getWidth()).toInt()]!![(v * bDimReal.getHeight()).toInt()]
        val ret: Point3D?
        ret = Point3D(floats[0].toDouble(), floats[1].toDouble(), 0.0)
        return ret
    }

    fun findAxPointInBa12(u: Double, v: Double): Point3D {
        if (u >= 0 && v >= 0 && u < 1 && v < 1) return imageAB[(u * bDimReal.getWidth()).toInt()]!![(v * bDimReal.getHeight()).toInt()]
        return Point3D.O0
    }

    fun findAxPointInBa13(u: Double, v: Double): Point3D {
        if (u >= 0 && v >= 0 && u < 1 && v < 1) return imageCB[(u * bDimReal.getWidth()).toInt()]!![(v * bDimReal.getHeight()).toInt()]
        return Point3D.O0
    }
}