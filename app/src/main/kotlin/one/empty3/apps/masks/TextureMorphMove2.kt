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

import one.empty3.apps.facedetect.jvm.ConvexHull
import one.empty3.apps.facedetect.jvm.Dimension
import one.empty3.apps.facedetect.jvm.DistanceAB
import one.empty3.apps.facedetect.jvm.DistanceBezier3
import one.empty3.apps.facedetect.jvm.DistanceIdent
import one.empty3.apps.facedetect.jvm.DistanceProxLinear1
import one.empty3.apps.facedetect.jvm.DistanceProxLinear2
import one.empty3.apps.facedetect.jvm.DistanceProxLinear3
import one.empty3.apps.facedetect.jvm.DistanceProxLinear4
import one.empty3.apps.facedetect.jvm.DistanceProxLinear42
import one.empty3.apps.facedetect.jvm.DistanceProxLinear43
import one.empty3.apps.facedetect.jvm.PolygonDistance
import one.empty3.library.CopyRepresentableError
import one.empty3.library.ITexture
import one.empty3.library.Lumiere
import one.empty3.library.MatrixPropertiesObject
import one.empty3.library.Point3D
import one.empty3.library.objloader.E3Model
import one.empty3.libs.Color
import one.empty3.libs.Image
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class TextureMorphMove2(
    var image1: Image?,
    var model: E3Model?,
    var image3: Image?,
    var txt1: Map<String, Point3D>?,
    var txt2: Map<String, Point3D>?,
    var txt3: Map<String, Point3D>?,
    var hdTextures: Boolean,
    pDistanceAbClass: Class<out DistanceAB?>?,
    isBezier: Boolean,
    editPanel: TextureData,
) : ITexture() {
    var percent: Double = 1.0
    val TAG: String = "one.empty3.apps.masks.TextureMorphMove2"
    var editData: TextureData = editPanel
    var distanceAbClass  : Class<out DistanceAB?>? = pDistanceAbClass

    var distanceAB: DistanceAB? = null

    init {
        try {
            if(editData==null)
                editData = TextureData()
            editData.image = image1
            editData.model = model
            editData.imageFileRight = image3
            editData.pointsInImage = txt1
            editData.pointsInModel = txt2
            editData.points3 = txt3
            editData.hdTextures = hdTextures
            editData.isBezier = isBezier
            if(pDistanceAbClass!=null) {
                editData.distanceABClass = pDistanceAbClass
                distanceAbClass = pDistanceAbClass
            }

            initTexture()
            Logger.getLogger(javaClass.canonicalName!!.toString()).log(Level.INFO, "Modele texturé avec la texture "+ distanceAbClass!!.simpleName)

            Logger.getLogger(javaClass.canonicalName!!.toString()).log(Level.INFO, "Text1 : "+ editData.pointsInImage?.size)
            Logger.getLogger(javaClass.canonicalName!!.toString()).log(Level.INFO, "Text2 : "+ editData.pointsInModel?.size)
            Logger.getLogger(javaClass.canonicalName!!.toString()).log(Level.INFO, "Text3 : "+ editData.points3?.size)

        } catch (e: RuntimeException) {
            Logger.getLogger(javaClass.canonicalName!!.toString()).log(Level.WARNING, "unknown 1 error", e)
        }
    }

    @Throws(
        CopyRepresentableError::class,
        IllegalAccessException::class,
        InstantiationException::class
    )
    override fun copy(): MatrixPropertiesObject? {
        return null
    }

    fun initTexture() {
        if(distanceAbClass==null) {
            throw RuntimeException("distanceAbClass is null")
        } else {
            try {
                setDistanceAbClass2(distanceAbClass!!)
            } catch (e:RuntimeException) {
                e.printStackTrace()
                return
            }
            if (editData.pointsInModel!!.isNotEmpty()) {
                editData.convexHull2 =
                    ConvexHull(
                        editData.pointsInModel!!.values.stream().toList(),
                        editData.dimPictureBox
                    )
            } else {
                Logger.getLogger(TAG)
                    .log(Level.INFO, "ConvexHull 2 is probably null because edit.pointsInModel.isEmpty() or editData.dimPictureBox==null")
                println("ConvexHull 2 is probably null because edit.pointsInModel.isEmpty() or editData.dimPictureBox==null")
            }
        }
    }


    override fun getColorAt(u: Double, v: Double): Int {
        if (editData == null || editData.distanceAB == null)
            return 0
        if (editData.distanceAB is DistanceIdent) {
            val ident = (editData.distanceAB as DistanceIdent).findAxPointInB(u, v)

            val point3D =
                Point3D(ident.x * editData.image!!.width, ident.y * editData.image!!.height, 0.0)

            val x = (max(0.0, min(point3D.x, editData.image!!.width.toDouble() - 1))).toInt()
            val y = (max(0.0, min((point3D.y), editData.image!!.height.toDouble() - 1))).toInt()

            /*if (x == 0 || y == 0 || x == editData.getWidth() - 1 || y == editData.getHeight() - 1) {
                return 0;
            }*/
            return editData.image!!.getRgb(x, y)
        }
        //if (distanceAB!!.isInvalidArray()) {
        //    return 0
        //}

        if (editData.image != null) {
            // Inside TextureMorphMove class (assuming you have a list of Point3D for your polygon)


            val x1 = (u * (editData.image!!.width - 1)).toInt()
            val y1 = (v * (editData.image!!.height - 1)).toInt()
            
            val isBezier = editData.distanceAB is DistanceBezier3
            // Check if required structures are present for non-Bezier distances
            if (!isBezier && (editData.distanceAB!!.sAij == null || editData.distanceAB!!.sBij == null)) {
                return 0
            }
            try {
                val axPointInB = editData.distanceAB!!.findAxPointInB(u, v)
                if (axPointInB != null) {
                    val p = Point3D(
                        axPointInB.x * editData.image!!.width,
                        axPointInB.y * editData.image!!.height,
                        0.0
                    )

                    var percentB =
                        distanceToConvexHull(distanceAB!!.bDimReal, editData.convexHull1!!, p) + 10

                    percentB = percentB / 40.0
                    if (percentB < 0) {
                        percentB = 0.0
                    }
                    if (percentB > 1) percentB = 1.0


                    //percentB = 0.5;
                    val xLeft = (max(0.0, min(p.x, editData.image!!.width.toDouble() - 1))).toInt()
                    val yLeft =
                        (max(0.0, min(p.y, editData.image!!.height.toDouble() - 1))).toInt()

                    var markA = false

                 if (editData.distanceAB is DistanceProxLinear43 && (editData.distanceAB as DistanceProxLinear43).jpgRight != null) {
                        var c = (editData.distanceAB as DistanceProxLinear43).findAxPointInBa13(u, v)
                        if (c != null) {
                            c = c.multDot(
                                Point3D(
                                    (editData.distanceAB as DistanceProxLinear43).jpgRight.width.toDouble(),
                                    (editData.distanceAB as DistanceProxLinear43).jpgRight.height.toDouble(),
                                    0.0
                                )
                            )
                            val x3 = (max(
                                0.0,
                                min(c.x, editData.imageFileRight!!.width.toDouble() - 1)
                            )).toInt()
                            val y3 = (max(
                                0.0,
                                min(c.y, editData.imageFileRight!!.height.toDouble() - 1)
                            )).toInt()
                            //if(dist4.checkedListC[x3][y3]) {
                            if ( /*editData.convexHull3!=null &&editData.convexHull3.isPointInHull(x3, y3)*/ /*&&*/editData.convexHull1 != null && editData.convexHull1!!.isPointInHull(xLeft.toDouble(), yLeft.toDouble()) /*&&editData.convexHull2!=null &&editData.convexHull2.isPointInHull(x1, y1)*/
                            ) {
                                markA = true
                                return (editData.distanceAB as DistanceProxLinear43).jpgRight.getRgb(x3, y3)
                            }

                        }
                    } else if (editData.distanceAB is DistanceProxLinear44_2 && (editData.distanceAB as DistanceProxLinear44_2).jpgRight != null) {
                        val dist4 = editData.distanceAB as  DistanceProxLinear44_2
                        var c : Point3D = dist4.findAxPointInBa13(u, v);
                            c = c.multDot(Point3D(dist4.jpgRight.width.toDouble(), dist4.jpgRight.getHeight().toDouble(), 0.0))
                            val x3 = 0.coerceAtLeast(
                                c.x.coerceAtMost(
                                    editData.imageFileRight!!.width.toDouble() - 1
                                ).toInt()
                            )
                            val y3 = 0.coerceAtLeast(
                                c.getY().coerceAtMost(
                                    editData.imageFileRight!!.getHeight().toDouble() - 1
                                ).toInt()
                            )
                            //if(dist4.checkedListC[x3][y3]) {
                            if (/*editPanel.convexHull3!=null &&editPanel.convexHull3.isPointInHull(x3, y3)*/
                            /*&&*/editData.convexHull1 != null && editData.convexHull1!!.isPointInHull(xLeft.toDouble(), yLeft.toDouble())
                            /*&&editPanel.convexHull2!=null &&editPanel.convexHull2.isPointInHull(x1, y1)*/) {
                                markA = true;
                                var color = DoubleArray(3);
                                val rgb3 = Lumiere.getDoubles(dist4.jpgRight.getRgb(x3, y3));
                                val rgb1 = Lumiere.getDoubles(editData.image!!.getRgb(xLeft, yLeft));
                                var k = 0
                                for(k in 0..2) {
                                    color[k] = rgb1[k] + (rgb3[k] - rgb1[k]) * percentB*percent;
                                }

                                //return one.empty3.libs.Color.newCol((float) color[0], (float) color[1], (float) color[2]).getRGB();
                                return Color.newCol (color[0].toFloat(),  color[1].toFloat(), (color[2].toFloat())).getRGB();
                            }
                    } else if (editData.distanceAB !is DistanceProxLinear44_2) {
                        return editData.image!!.getRgb(xLeft, yLeft)
                    }
                }
                return editData.image!!.getRgb(x1, y1)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }

        return Color.newCol(255f, 255f, 0f).getRgb()
    }

    private fun distanceToConvexHull(

        bDimReal: Dimension,
        convexHull2: ConvexHull,
        axPointInB: Point3D
    ): Double {
        val p: List<Point3D> = ArrayList()
        /*
        for (int i = 0; i < convexHull2.hullPoints.size(); i++) {
            p.add(convexHull2.hullPoints.get(i).multDot(new Point3D(bDimReal.getHeight(),bDimReal.getHeight()));
        }

*/


        if (convexHull2 != null && axPointInB != null) {
            val d = PolygonDistance.distanceToPolygon(axPointInB, convexHull2.hullPoints)+10
            val x = axPointInB.getX().toInt()
            val y = axPointInB.getY().toInt()

            if(x<0||y<0||x>=convexHull2.mask.width||y>=convexHull2.mask.height) {
                return 100000.0
            }
            return (d * (if (convexHull2.isPointInHull(x.toDouble(),y.toDouble())) 1 else -1) + 1) / 2.0
        }
        return 0.0
    }


    private fun setDistanceAbClass2(pDistanceAbClass: Class<out DistanceAB?>) {

        println("TextureMorphMove2.setDistanceAbClass2: Entering " + pDistanceAbClass!!.canonicalName)


        val aDimReal = Dimension(editData.image!!.width, editData.image!!.height)
        val bDimReal =  editData.dimPictureBox
        var cDimReal: Dimension? = null
        if (editData.imageFileRight != null) {
            cDimReal = Dimension(
                editData.imageFileRight!!.width,
                editData.imageFileRight!!.height
            )
        }
        /*if(editData.pointsInImage!=null&&editData.pointsInImage!!.isNotEmpty()) {
        editData.convexHull1 = ConvexHull(
            editData.pointsInImage!!.values.stream().toList(), aDimReal
        )
    }
    if(editData.pointsInModel!=null&&editData.pointsInModel!!.isNotEmpty()) {
    editData.convexHull2 = ConvexHull(
        editData.pointsInModel!!.values.stream().toList(), bDimReal
    )
    }*/
        if (pDistanceAbClass==DistanceProxLinear43::class.java && editData.imageFileRight != null)
            editData.convexHull3 =
                ConvexHull(
                    editData.points3!!.values.stream().toList(),
                    Dimension(
                        editData.imageFileRight!!.width,
                        editData.imageFileRight!!.height
                    )
                )
        if (pDistanceAbClass==DistanceProxLinear44_2::class.java && editData.imageFileRight != null)
            editData.convexHull3 =
                ConvexHull(
                    editData.points3!!.values.stream().toList(),
                    Dimension(
                        editData.imageFileRight!!.width,
                        editData.imageFileRight!!.height
                    )
                )
        if (editData.pointsInImage != null && !editData.pointsInImage!!.isEmpty())
            editData.convexHull1 = ConvexHull(
                editData.pointsInImage!!.values.stream().toList(), Dimension(
                    editData.image!!.width, editData.image!!.height
                )
            )
        if (editData.pointsInModel != null && !editData.pointsInModel!!.isEmpty())
            editData.convexHull2 = ConvexHull(
                editData.pointsInModel!!.values.stream().toList(), Dimension(
                    editData.dimPictureBox!!.width.toInt(),
                    editData.dimPictureBox!!.height.toInt()
                )
            )
        val lA: MutableList<Point3D?> = ArrayList()
        val lB: MutableList<Point3D?> = ArrayList()
        val lC: MutableList<Point3D?> = ArrayList()


        /**
         * Double A, B avec ai correspond à bi ( en se servant des HashMap)
         */

        synchronized(editData.pointsInImage!!) {
            editData.pointsInImage!!.forEach { sA, point3D ->
                editData.pointsInModel!!.forEach { sB, point3D ->
                    if (editData.imageFileRight == null) {
                        lA.add(editData.pointsInImage!![sA])
                        lB.add(editData.pointsInModel!![sB])
                    } else {
                        editData.points3!!.forEach { sC, point3D ->
                            if (sA == sB && sB == sC) {
                                lA.add(editData.pointsInImage!![sA])
                                lB.add(editData.pointsInModel!![sB])
                                if (editData.imageFileRight != null) lC.add(editData.points3!![sC])
                            }
                        }
                    }
                }
            }
        }
        Logger.getAnonymousLogger().fine("List sizes before render : " + lA.size + " " + lB.size + " "+ lC.size)
        println("List sizes before render : " + lA.size + " " + lB.size + " " + lC.size)
        /*
                if(lC.size>0 && lA.size>0 && lB.size>0) {
                    val max = Math.min(lA.size, Math.min(lB.size, lC.size));
                    lA = lA.subList(0, max);
                    lB = lB.subList(0, max);
                    lC = lC.subList(0, max);

                } else if(lA.size>0&&lB.size>0) {
                    val max = Math.min(lA.size, lB.size);
                    lA = lA.subList(0, max);
                    lB = lB.subList(0, max);
                } else {

                }
      */
        if (editData.image != null && editData.model != null) {
            val timeStarted = System.nanoTime()
            try {
                if (pDistanceAbClass==DistanceProxLinear1::class.java) {
                    editData.distanceAB = DistanceProxLinear1(lA, lB, aDimReal, bDimReal, editData.opt1, editData.optimizeGrid
                    )
                } else if (pDistanceAbClass==DistanceProxLinear2::class.java) {
                    editData.distanceAB = DistanceProxLinear2(lA, lB, aDimReal, bDimReal, editData.opt1, editData.optimizeGrid
                    )
                } else if (pDistanceAbClass==DistanceProxLinear3::class.java) {
                    editData.distanceAB = DistanceProxLinear3(lA, lB, aDimReal, bDimReal, editData.opt1, editData.optimizeGrid
                    )
                } else if (pDistanceAbClass==DistanceProxLinear4::class.java) {
                    editData.distanceAB = DistanceProxLinear4(
                        lA, lB, aDimReal, bDimReal!!, editData.opt1, editData.optimizeGrid
                    )
                    if (editData.imageFileRight != null) (editData.distanceAB as DistanceProxLinear4).jpgRight =
                        editData.imageFileRight
                } else if (pDistanceAbClass==DistanceProxLinear42::class.java) {
                    editData.distanceAB = DistanceProxLinear42(
                        lA, lB, aDimReal,
                        bDimReal, editData.opt1, editData.optimizeGrid
                    )
                    if (editData.imageFileRight != null) (editData.distanceAB as DistanceProxLinear42).jpgRight =
                        editData.imageFileRight

                } else if (pDistanceAbClass==DistanceProxLinear43::class.java) {
                    editData.distanceAB = DistanceProxLinear43(
                        lA, lB, lC, aDimReal,
                        bDimReal, cDimReal, editData.opt1, editData.optimizeGrid
                    )
                    (editData.distanceAB as DistanceProxLinear43).setComputeMaxTime(10E15)
                    (editData.distanceAB as DistanceProxLinear43).setJpgRight(editData.imageFileRight)
                    editData.convexHull3 = ConvexHull(
                        lC, aDimReal)
                    if (editData.imageFileRight != null) (editData.distanceAB as DistanceProxLinear43).jpgRight =
                        editData.imageFileRight
                } else if (pDistanceAbClass==DistanceProxLinear44_2::class.java) {
                    editData.distanceAB = DistanceProxLinear44_2(
                        lA, lB, lC, aDimReal, bDimReal!!, cDimReal!!,
                        editData.opt1, editData.optimizeGrid, editData.imageFileRight!!
                    )
                    distanceAB = editData.distanceAB
                    (editData.distanceAB as DistanceProxLinear44_2).jpgRight = editData.imageFileRight
                    editData.convexHull3 = ConvexHull(
                        lC, cDimReal
                    )
                    if (editData.imageFileRight != null) (editData.distanceAB as DistanceProxLinear44_2).jpgRight = editData.imageFileRight

                } else if (pDistanceAbClass==DistanceBezier3::class.java) {
                    editData.distanceAB = DistanceBezier3(
                        lA, lB, aDimReal,
                        bDimReal, editData.opt1, editData.optimizeGrid
                    )
                } else if (pDistanceAbClass==DistanceIdent::class.java) {
                    editData.distanceAB = DistanceIdent()
                } else {
                    return
                }
    /*
                                    if (pDistanceAbClass != null) {
                                        this.distanceAbClass = pDistanceAbClass as Class<out DistanceAB?>
                                        editData.distanceAB = DistanceIdent()
                                        editData.distanceABClass = pDistanceAbClass
                                    } else {
                                        throw NullPointerException("distanceMap is null in TextureMorphMove")
                                    }*/
            } catch (ex: RuntimeException) {
                ex.printStackTrace()
            }
            distanceAB = editData.distanceAB
            val nanoElapsed = System.nanoTime() - timeStarted
            val secs = (10E-9 * nanoElapsed).toInt()
            val mins = (secs / 60).toInt()
            val hours =( mins / 60).toInt()
            val days = (hours / 24).toInt()
            Logger.getAnonymousLogger().log(
                Level.INFO,
                "Temps écoulé à produire l'objet DistanceAB (" + pDistanceAbClass.canonicalName +
                        ") à : " + 10E-9 * nanoElapsed + secs + "s"+ mins + "m"+ hours + "h"+ days + "d")
            println("Temps écoulé à produire l'objet DistanceAB (" + pDistanceAbClass.canonicalName +
                    ") à : " + 10E-9 * nanoElapsed + secs + "s"+ mins + "m"+ hours + "h"+ days + "d")
        }
        this.distanceAB = editData.distanceAB
        Logger.getAnonymousLogger().log(Level.INFO, "distanceAb: " + editData.distanceAB!!.javaClass+ " "+ distanceAB!!.javaClass)

        if(editData.convexHull1!=null)
            println("convexHull1: "+editData.convexHull1!!.hullPoints.size+" "+editData.convexHull1!!.mask.width+" "+editData.convexHull1!!.mask.height)
        if(editData.convexHull2!=null)
            println("convexHull2: "+editData.convexHull2!!.hullPoints.size+" "+editData.convexHull2!!.mask.width+" "+editData.convexHull2!!.mask.height)
        if(editData.convexHull3!=null)
            println("convexHull3: "+editData.convexHull3!!.hullPoints.size+" "+editData.convexHull3!!.mask.width+" "+editData.convexHull3!!.mask.height)

    }

    fun loadTxt(index: Int, selectedFile: ByteArray?): HashMap<String?, Point3D?> {
        val points = HashMap<String?, Point3D?>()
        try {
            val split = String(selectedFile ?: return points, Charsets.UTF_8).split("\n")

            var line = ""
            val iterator : Iterator<String> = split.iterator();
            while (iterator.hasNext()) {
                line = iterator.next().trim { it <= ' ' }
                val point = Point3D()
                val landmarkType: String
                val x: Double
                val y: Double
                if (!line.isEmpty()) {
                    landmarkType = line
                    // X
                    line = iterator.next().trim { it <= ' ' }
                    x = line.toDouble()
                    // Y
                    line = iterator.next().trim { it <= ' ' }
                    y = line.toDouble()
                    // Blank line
                    line = iterator.next().trim { it <= ' ' }

                    points[landmarkType] = Point3D(x, y, 0.0)
                }
            }
            Logger.getAnonymousLogger()
                .log(Level.INFO, "Loaded points in image : "+points.size)

            if(points.size==0) {
                Logger.getAnonymousLogger().log(Level.SEVERE, selectedFile.toString())
            }

            return points
        } catch (ex: RuntimeException) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Seems file is not good ", ex)
        }
        return points
    }


    companion object {
        private val WHITE: Color = Color.newCol(1f, 1f, 1f)
    }
}