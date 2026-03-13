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

import one.empty3.apps.facedetect.jvm.DistanceAB
import one.empty3.apps.facedetect.jvm.DistanceIdent
import one.empty3.apps.facedetect.jvm.DistanceProxLinear1
import one.empty3.apps.facedetect.jvm.DistanceProxLinear2
import one.empty3.apps.facedetect.jvm.DistanceProxLinear3
import one.empty3.apps.facedetect.jvm.DistanceProxLinear4
import one.empty3.apps.facedetect.jvm.DistanceProxLinear43
import one.empty3.apps.facedetect.jvm.DistanceProxLinear5
import one.empty3.apps.facedetect.jvm.EditPolygonsMappings
import one.empty3.apps.facedetect.jvm.FinishInitListener
import one.empty3.library.objloader.E3Model
import one.empty3.libs.Image
import java.io.ByteArrayOutputStream
import java.util.logging.Level
import java.util.logging.Logger


public open class ImageProcessor3(
    image1: Image?,
    model: E3Model?,
    image3: Image?,
    txt1: String?,
    txt2: String?,
    txt3: String?,
    hd_texture: Boolean,
    selected_algorithm: Int,
    isBezier: Boolean
) : Runnable {
    @JvmField
    var image1: Image? = null
    @JvmField
    var model: E3Model? = null
    @JvmField
    var image3: Image? = null
    @JvmField
    var txt1: String? = null
    @JvmField
    var txt2: String? = null
    @JvmField
    var txt3: String? = null
    @JvmField
    var hd_texture: Boolean = false
    @JvmField
    var selected_algorithm: Int = 0
    @JvmField
    var isBezier: Boolean = false
    var isRunning: Boolean = false
        private set
    @JvmField
    var editPolygonsMappings: EditPolygonsMappings? = null
    @JvmField
    var editPolygonsMappings0: TextureData? = null
    @JvmField
    var image: Image? = null

    init {
        try {
            this.image1 = image1
            this.model = model
            this.image3 = image3
            this.txt1 = txt1
            this.txt2 = txt2
            this.txt3 = txt3
            this.hd_texture = hd_texture
            this.selected_algorithm = selected_algorithm
            this.isBezier = isBezier
        } catch (e: RuntimeException) {
            Logger.getLogger(javaClass.canonicalName!!.toString()).log(Level.WARNING, "unknown 1 error", e)
        }
    }


    override fun run() {
        try {

            editPolygonsMappings = EditPolygonsMappings()
            editPolygonsMappings!!.loadImage1(image1)
            editPolygonsMappings!!.loadImage3(image3)
            editPolygonsMappings!!.model = model
            editPolygonsMappings!!.loadTxtData(txt1, 0)
            editPolygonsMappings!!.loadTxtData(txt2, 1)
            editPolygonsMappings!!.loadTxtData(txt3, 2)
            editPolygonsMappings!!.hdTextures = hd_texture
            when (selected_algorithm) {
                0 -> editPolygonsMappings!!.distanceABClass =
                    DistanceProxLinear1::class.java

                1 -> editPolygonsMappings!!.distanceABClass = DistanceProxLinear2::class.java
                2 -> editPolygonsMappings!!.distanceABClass = DistanceProxLinear3::class.java
                3 -> editPolygonsMappings!!.distanceABClass = DistanceProxLinear4::class.java
                4 -> editPolygonsMappings!!.distanceABClass = DistanceProxLinear5::class.java
                5 -> editPolygonsMappings!!.distanceABClass = DistanceProxLinear43::class.java
                6 -> editPolygonsMappings!!.distanceABClass = DistanceProxLinear44_2::class.java
                7 -> editPolygonsMappings!!.distanceABClass = DistanceIdent::class.java
                else -> return
            }
            editPolygonsMappings!!.typeShape =
                if (isBezier) DistanceAB.TYPE_SHAPE_BEZIER else DistanceAB.TYPE_SHAPE_QUADR //!Objects.equals(data.get("selected_texture_type"), "Bezier texture") ?  : DistanceAB.TYPE_SHAPE_BEZIER;
            editPolygonsMappings!!.testHumanHeadTexturing.maxFrames = 200

            val runApp = Thread(editPolygonsMappings)


            val phase = intArrayOf(0)


            runApp.start()


            while (editPolygonsMappings!!.iTextureMorphMove == null
                || editPolygonsMappings!!.iTextureMorphMove!!.distanceAB == null
            ) {
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }


            try {
                editPolygonsMappings!!.iTextureMorphMove!!.distanceAB!!.addFinishInitListener(object :
                    FinishInitListener() {
                    override fun fire() {
                        phase[0] = 1
                    }
                })
                Logger.getLogger(javaClass.canonicalName!!).log(Level.INFO, "Compute texture ...")
            } catch (ex: NullPointerException) {
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (phase[0] == 1) {
                Logger.getLogger(javaClass.canonicalName!!)
                    .log(Level.INFO, "Compute texture ... DONE")
            }
            while ((editPolygonsMappings!!.testHumanHeadTexturing != null && editPolygonsMappings!!.testHumanHeadTexturing.zBufferImage() == null && editPolygonsMappings!!.isRunning && editPolygonsMappings!!.testHumanHeadTexturing.frame() <= editPolygonsMappings!!.testHumanHeadTexturing.maxFrames && isBlankImage(
                    editPolygonsMappings!!.testHumanHeadTexturing.zBufferImage()
                ))
                || editPolygonsMappings!!.testHumanHeadTexturing.frame() <= 3
            ) {
                Logger.getLogger(javaClass.canonicalName)
                    .log(Level.INFO, "Running ImageProcessor wait loop ...")
                try {
                    Thread.sleep(100)
                } catch (ignored: InterruptedException) {
                    ignored.printStackTrace()
                }

                if (editPolygonsMappings!!.testHumanHeadTexturing.zBufferImage() != null) {
                    Logger.getLogger(javaClass.canonicalName)
                        .log(Level.INFO, "Running ImageProcessor wait loop ... DONE")
                    setImage(editPolygonsMappings!!.testHumanHeadTexturing.zBufferImage())
                    editPolygonsMappings!!.testHumanHeadTexturing.loop(false)
                    editPolygonsMappings!!.testHumanHeadTexturing.stop()
                    break
                }
            }
            /*if ((editPolygonsMappings.testHumanHeadTexturing.zBufferImage()) != (null)) {
                setImage(editPolygonsMappings.testHumanHeadTexturing.zBufferImage());
                editPolygonsMappings.stopThreadDisplay();
            }*/
            editPolygonsMappings!!.testHumanHeadTexturing.stop()
            editPolygonsMappings!!.testHumanHeadTexturing.maxFrames = 0
            editPolygonsMappings!!.stopRenderer()
            editPolygonsMappings!!.isRunning = false
        } catch (e: RuntimeException) {
            Logger.getLogger(javaClass.canonicalName).log(Level.WARNING, "unknown 2 (run) error", e)
        }
        editPolygonsMappings!!.isRunning = false
        this.isRunning = false


        Logger.getLogger(javaClass.canonicalName).log(Level.WARNING, "Rendering stopped")
    }

    fun stopAll() {
        editPolygonsMappings!!.stopRenderer()
        editPolygonsMappings!!.stopThreadDisplay()
        val image2 = editPolygonsMappings!!.testHumanHeadTexturing.zBufferImage()
        if (image2 != (null)) {
            setImage(editPolygonsMappings!!.testHumanHeadTexturing.zBufferImage())
        }
        editPolygonsMappings!!.testHumanHeadTexturing.stop()
        editPolygonsMappings!!.testHumanHeadTexturing.maxFrames = 0
        editPolygonsMappings!!.stopRenderer()
        editPolygonsMappings!!.isRunning = false
    }

    private fun isBlankImage(zBufferImage: Image): Boolean {
        if (image == null) return true

        val c = image!!.getRgb(0, 0)
        for (i in 0 until image!!.width) {
            for (j in 0 until image!!.height) {
                if (image!!.getRgb(i, j) != c) return false
            }
        }
        return true
    }

    private fun setImage(zBufferImage: Image) {
        this.image = zBufferImage
    }

    fun getImage(): Image? {
        return image
    }

    val resultMapImage: ByteArray?
        get() {
            val image2 = getImage()
            if (image2 != null) {
                val byteArrayOutputStream =
                    ByteArrayOutputStream()
                if (image2.toOutputStream(byteArrayOutputStream)) {
                    return byteArrayOutputStream.toByteArray()
                }
            }
            return null
        }
}