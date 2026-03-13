package one.empty3.apps.masks

import one.empty3.apps.facedetect.jvm.ConvexHull
import one.empty3.apps.facedetect.jvm.Dimension
import one.empty3.apps.facedetect.jvm.DistanceAB
import one.empty3.apps.facedetect.jvm.DistanceIdent
import one.empty3.apps.facedetect.jvm.DistanceProxLinear1
import one.empty3.apps.facedetect.jvm.DistanceProxLinear2
import one.empty3.apps.facedetect.jvm.DistanceProxLinear3
import one.empty3.apps.facedetect.jvm.DistanceProxLinear4
import one.empty3.apps.facedetect.jvm.DistanceProxLinear42
import one.empty3.apps.facedetect.jvm.DistanceProxLinear43
import one.empty3.apps.facedetect.jvm.DistanceProxLinear5
import one.empty3.apps.facedetect.jvm.DistanceProxLinear6
import one.empty3.apps.testobject.Resolution
import one.empty3.library.Camera
import one.empty3.library.Config
import one.empty3.library.Point3D
import one.empty3.library.Scene
import one.empty3.library.ZBufferImpl
import one.empty3.library.ZBufferImpl.IncrementOptimizer
import one.empty3.library.objloader.E3Model
import one.empty3.libs.Image
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Scanner
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.Delegates

class MovieMorphing(imagesCount:Int,

                    private var image1: Image,
                    private var model: E3Model,
                    private var image3: Image,
                    private var txt1: Map<String, Point3D>,
                    private var txt3: Map<String, Point3D>,
                    var hd_textures: Boolean,
                    var algorithm: Int,
                    private var service: Boolean,
                    private var settings: Map<String, *>?,

                    ) {
    private var maxMeshIncrement by Delegates.notNull<Int>()
    private var minMeshIncrement by Delegates.notNull<Int>()
    private var useConstantMeshIncrement by Delegates.notNull<Boolean>()
    var text1: Map<String, Point3D>? = null
    var text2: Map<String, Point3D>? = null
    var text3: Map<String, Point3D>? = null
    var isRunning: Boolean = true
    var editData: TextureData? = null
    var image: Image? = null

    val TAG: String = "one.empty3.apps.masks.MovieMorphing"
    private var isService: Boolean = true

    //private lateinit var myRenderService: MyRenderService
    lateinit var zBuffer: ZBufferImpl
    private lateinit var textureMorphMove: TextureMorphMove2
    private var initialized: Boolean = false

    init {
        try {
            this.text1 = txt1
            this.text3 = txt3
            this.hd_textures = hd_textures
            this.algorithm = algorithm
            this.isService = service
            if (editData == null)
                editData = TextureData()
            if (hd_textures) {
                editData!!.dimPictureBox = one.empty3.apps.facedetect.jvm.Dimension(
                    Resolution.HD1080RESOLUTION.x(),
                    Resolution.HD1080RESOLUTION.y()
                )
            } else if (settings != null) {
                if (settings!!["useMaxRes"] != null && (settings!!["useMaxRes"] == true
                            || settings!!["useMaxRes"] == "true")
                ) {
                    if (settings!!["maxResWidth"] != null && settings!!["maxResHeight"] != null) {
                        editData!!.dimPictureBox =
                            Dimension(
                                settings!!["maxResWidth"].toString().toInt(),
                                settings!!["maxResHeight"].toString().toInt()
                            )
                    } else {
                        editData!!.dimPictureBox =
                            Dimension(200, 200)
                    }
                } else {
                    //editData!!.dimPictureBox =
                    //Dimension(200, 200)
                    editData!!.dimPictureBox =
                        Dimension(image1!!.width, image1.height)
                }
            } else {
                editData!!.dimPictureBox =
                    Dimension(200, 200)
            }
            settings?.let {
                if (it["useConstantMeshIncrement"] == null) {
                    this.useConstantMeshIncrement = false
                } else {
                    this.useConstantMeshIncrement =
                        settings!!["useConstantMeshIncrement"]!! as Boolean
                    this.minMeshIncrement = (settings!!["minMeshIncrement"] ?: 0) as Int
                    this.maxMeshIncrement = (settings!!["maxMeshIncrement"] ?: 0) as Int
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initialized = true
    }
    private val imagesCount: Int = imagesCount

    fun run() : File {
        for (i in 0 until imagesCount) {
            val percent = i.toDouble() / imagesCount.toDouble()
            createTexture(percent)
            this.text2 = HashMap<String, Point3D>()

            text1?.forEach { (string1, d1) ->
                run {
                    text3?.forEach { (string3, d3) ->
                        run {
                            if (string3.equals(string1)) {
                                (this.text2 as HashMap)[string3] = d1.plus(
                                    d3.moins(d1).mult(i.toDouble() / imagesCount.toDouble())
                                )
                            }
                        }
                    }
                }
            }

            runZbuffer(image1!!, model, image3!!, text1!!, text2!!, text3!!, hd_textures, algorithm, true, service,  settings, percent)
        }
        return File("")
    }

    private fun runZbuffer(
        image1: Image,
        model: E3Model,
        image3: Image,
        text1: Map<String, Point3D>,
        text2: Map<String, Point3D>,
        text3: Map<String, Point3D>,
        hdTextures: Boolean,
        algorithm: Int,
        bool2: Boolean,
        service: Boolean,
        settings: kotlin.collections.Map<String, *>?,
        percent: Double
    ) : Image {
        if (!initialized) {
            throw RuntimeException("Not initialized")
        }
        zBuffer = ZBufferImpl(
            editData!!.dimPictureBox!!.width.toInt(),
            editData!!.dimPictureBox!!.height.toInt()
        )
        println("ZBufferImpl created. Size;  ${zBuffer.la()} x ${zBuffer.ha()}")
        var v = 2000.0
        if (model != null) {
            var numFaces: Int =
                (model as E3Model).objects.listRepresentable.size
            if (numFaces <= 0) {
                numFaces = 1
            }
            v = 2000.0 / numFaces
            if (editData != null) {
                val min: Point3D = Point3D(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
                val max: Point3D = Point3D(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);
                val diff: Point3D = max.moins(min);
                model!!.getBounds(min, max);
                val surfaceBoundingCube =
                    2 * (diff.getX() * diff.getY() + diff.getY() * diff.getZ() + diff.getZ() * diff.getX());
                v = 2.0 * zBuffer.la() * zBuffer.ha() / numFaces / surfaceBoundingCube
                if (useConstantMeshIncrement) {
                    zBuffer.setIncrementOptimizer(
                        IncrementOptimizer(
                            minMeshIncrement.toDouble(), maxMeshIncrement.toDouble()
                        )
                    )
                    Logger.getAnonymousLogger()
                        .info("MinMaxOptimium set " + v + " constant mesh increment :" + useConstantMeshIncrement)

                } else {
                    var numFaces = (model as E3Model)
                        .objects.listRepresentable.size;
                    if (numFaces <= 0) {
                        numFaces = 1;
                    }
                    val min = Point3D(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
                    val max = Point3D(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);
                    model!!.getBounds(min, max);
                    val diff = max.moins(min);
                    val surfaceBoundingCube =
                        2 * (diff.getX() * diff.getY() + diff.getY() * diff.getZ() + diff.getZ() * diff.getX());
                    //double v = 1.0/Math.sqrt(1.0/(64.0 *z().la()*z().ha() / numFaces/Math.pow(surfaceBoundingCube, 2./3.)));
                    v = 2.0 * Math.pow(1.0 * zBuffer.la() * zBuffer.ha() * numFaces, .5) + 1.0;
                    if (v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY || v == 0.0) {
                        v = (((zBuffer.la() * zBuffer.ha())) / numFaces + 1).toDouble()
                    }
                    v = 1 / (v * 10.0)
                    /*
                    zBuffer.setIncrementOptimizer(
                        ZBufferImpl.IncrementOptimizer(ZBufferImpl.IncrementOptimizer.Strategy.ENSURE_MAXIMUM_PERFORMANCE, .001)
                    )

                     */
                    zBuffer.setIncrementOptimizer(
                        IncrementOptimizer(IncrementOptimizer.Strategy.ENSURE_MINIMUM_DETAIL, v)
                    )
                    Logger.getAnonymousLogger().info("MinMaxOptimium set " + v + " increment value constructor : " + v)
                }

            }
        }


        Logger.getAnonymousLogger().info("MinMaxOptimium set " + v + " (final)")
        zBuffer.setDisplayType(ZBufferImpl.DISPLAY_ALL)
        zBuffer.idzpp()
        val scene = Scene()
        scene.add(model)
        model!!.texture(textureMorphMove)
        if (image1 != null)
            Logger.getAnonymousLogger()
                .log(Level.INFO, "image1: ${image1!!.width} x ${image1!!.height}")
        //zBuffer.texture(ImageTexture(image1))
        //zBuffer.setMinMaxOptimium(zBuffer.MinMaxOptimium(MinMaxOptimium.MinMaxIncr.Min, 2000.0))
        zBuffer.displayType = ZBufferImpl.DISPLAY_ALL


        var imageRes: Image =
            Image(editData!!.dimPictureBox!!.width.toInt(), editData!!.dimPictureBox!!.height.toInt())
        if (model != null) {
            val camera = createCamera(scene, model!!, zBuffer)
            scene.cameraActive(camera)
            zBuffer.scene(scene)
            zBuffer.camera(camera)


            if (editData != null && editData!!.convexHull1 == null && editData!!.pointsInImage != null && editData!!.image != null) {
                editData!!.convexHull1 = ConvexHull(
                    editData!!.pointsInImage!!.values.toList(),
                    Dimension(
                        editData!!.dimPictureBox!!.width.toInt(),
                        editData!!.dimPictureBox!!.height.toInt()
                    )
                )
            }
            if (editData != null && editData!!.convexHull2 == null && editData!!.pointsInModel != null) {
                editData!!.convexHull2 = ConvexHull(
                    editData!!.pointsInModel!!.values.toList(),
                    Dimension(
                        editData!!.dimPictureBox!!.width.toInt(),
                        editData!!.dimPictureBox!!.height.toInt()
                    )
                )
            }
            zBuffer.draw(model)
            imageRes = zBuffer.image2()

            System.out.println("imageRes: " + imageRes != null)
            System.out.println("Rendering complete. Check code for debugging.")

            val df: DateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
            val date: String = df.format(Date())


            val filename = "autosave_" + date + "_" + imageRes.width + "x" + imageRes.height + ".png"
            val saveImageToPicturesLegacy1: File? =
                saveImageToPicturesLegacy(Config().defaultFileOutput.absolutePath+File.separator+filename, imageRes, filename)

            //NOTIFY SUCCESS
            return imageRes
        } else {
            System.out.println("Rendering failed because model is null.")
            //NOTIFY FAILURE
            return Image(1,1,0)

        }
    }


    fun loadTxt(selectedFile: String): Map<String, Point3D> {

        val points = HashMap<String, Point3D>()
        Logger.getAnonymousLogger().log(Level.INFO, "selectedFile: ${selectedFile.length}")

        try {
            val bufferedReader: Scanner = Scanner(selectedFile)
            var line: String = ""
            while (bufferedReader.hasNextLine()) {
                line = bufferedReader.nextLine().trim()
                var landmarkType: String
                var x: Double
                var y: Double
                if (!line.isEmpty()) {
                    landmarkType = line
                    // X
                    line = bufferedReader.nextLine().trim()
                    x = line.toDouble()
                    // Y
                    line = bufferedReader.nextLine().trim()
                    y = line.toDouble()
                    // Blank line
                    line = bufferedReader.nextLine().trim()

                    points.put(landmarkType, Point3D(x, y, 0.0))
                }
            }
            return points
            /*
                        try {
                        var split = selectedFile.split(System.lineSeparator())
                        var line = ""
                        val iterator: Iterator<String> = split.iterator()
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
                            .log(Level.INFO, "Loaded points in image : " + points.size)

                        if (points.size == 0) {
                            Logger.getAnonymousLogger().log(Level.SEVERE, selectedFile.toString())
                        }

                        return points
                    } catch (ex: RuntimeException) {
                        Logger.getAnonymousLogger().log(Level.SEVERE, "Seems file is not good ", ex)
                    }
                    return points
              */
        } catch (ex: RuntimeException) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Seems file is not good ", ex)
        }
        return points
    }

    private fun createCamera(scene: Scene, model: E3Model, zBuffer: ZBufferImpl): Camera {
        zBuffer.scene(scene)
        val c = Camera()
        scene.cameraActive(c)
        val minBox: Point3D = Point3D(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
        val maxBox: Point3D = Point3D(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE)

        model.getBounds(minBox, maxBox)


        if (editData!!.model != null) {
            editData!!.model!!.getBounds(minBox, maxBox);

            if (hd_textures) {
                c.getEye().x = maxBox.x / 2 + minBox.x / 2;
                c.getEye().y = maxBox.y / 2 + minBox.y / 2;
                c.getEye().z = maxBox.z + (Math.max(
                    maxBox.getX() - minBox.getX(),
                    maxBox.getY() - minBox.getY()
                )
                        ) * 1.1
                c.lookat.x = maxBox.x / 2 + minBox.x / 2;
                c.lookat.y = maxBox.y / 2 + minBox.y / 2;
                c.lookat.z = 0.0
                c.calculerMatrice(Point3D.Y.mult(-1.0));
                //c.setAngleYr(60, 1.0 * z().ha() / z().la());
            } else {
                c.getEye().z = maxBox.z + (Math.max(
                    maxBox.getX() - minBox.getX(),
                    maxBox.getY() - minBox.getY()
                )
                        ) * 1.1
                c.getEye().x = maxBox.x / 2 + minBox.x / 2;
                c.getEye().y = maxBox.y / 2 + minBox.y / 2;
                c.lookat.x = maxBox.x / 2 + minBox.x / 2;
                c.lookat.y = maxBox.y / 2 + minBox.y / 2;
                c.lookat.z = 0.0
                c.setLookat(Point3D.O0);
                c.calculerMatrice(Point3D.Y.mult(-1.0));
                //c.setAngleYr(60, 1.0 * z().ha() / z().la());
            }
        }
        return c
    }

    private fun createTexture(percent: Double) {
        val distanceABClass: Class<out DistanceAB?>
        Logger.getAnonymousLogger().log(Level.INFO, "selected_algorithm: $algorithm")
        when (algorithm) {
            0 -> distanceABClass = DistanceProxLinear1::class.java
            1 -> distanceABClass = DistanceProxLinear2::class.java
            2 -> distanceABClass = DistanceProxLinear3::class.java
            3 -> distanceABClass = DistanceProxLinear4::class.java
            4 -> distanceABClass = DistanceProxLinear5::class.java
            5 -> distanceABClass = DistanceProxLinear6::class.java
            6 -> distanceABClass = DistanceProxLinear42::class.java
            7 -> distanceABClass = DistanceProxLinear43::class.java
            8 -> distanceABClass = DistanceProxLinear44_2::class.java
            9 -> distanceABClass = DistanceIdent::class.java
            else -> {
                throw RuntimeException("Unknown algorithm")
            }
        }

        Logger.getAnonymousLogger().log(
            Level.INFO, "distanceABClass: $distanceABClass"
        )
        Logger.getAnonymousLogger().log(
            Level.INFO, "Model : " + model
        )
        textureMorphMove = TextureMorphMove2(
            image1, model, image3,
            text1, text2, text3, hd_textures, distanceABClass, true, editData!!
        )
        textureMorphMove.percent = percent
        //editData = textureMorphMove.editData
        Logger.getAnonymousLogger().log(Level.INFO, "Model in createTexture: $model")
        model!!.texture(textureMorphMove)
    }

}