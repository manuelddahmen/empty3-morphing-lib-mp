import one.empty3.apps.masks.MovieMorphing
import one.empty3.apps.testobject.TestObjetStub
import one.empty3.library.Point3D
import one.empty3.library.core.nurbs.FctXY
import one.empty3.library.objloader.E3Model
import one.empty3.libs.Image
import java.io.BufferedReader
import java.io.InputStream
import java.io.StringReader
import java.util.logging.Level
import java.util.logging.Logger

class TestMorphing : TestObjetStub() {
    override fun finit() {
        super.finit()
    }

    override fun ginit() {
        super.ginit()

        var image1 = Image(200, 200)
        var image3 = Image(200, 200)
        try {
            image1 = Image.getFromInputStream(javaClass.getResourceAsStream("belle-femme-modele-posant-dans-une-robe-elegante.jpg")!!) as Image
            image3 = Image.getFromInputStream(javaClass.getResourceAsStream("adorable-fille-aux-cheveux-boucles-sur-fond-bleu-photo-de-haute-qualite.jpg")!!) as Image
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.getLogger(javaClass.canonicalName).log(
                Level.WARNING,
                "Error loading image 1 or 3",
                e
            )
        }
        try {
            val resource: InputStream? = javaClass.getResourceAsStream("plane blender2.obj")
            val modelString: String = resource!!.readAllBytes().toString(Charsets.UTF_8)
            var model: E3Model? = null
            StringReader(modelString).use { reader ->
                BufferedReader(reader).use { bufferedReader ->
                    model = E3Model(bufferedReader, true, "plane blender2.obj")
                }
            }
            assert(model != null)
            val txt1 = HashMap<String, Point3D>()
            val txt3 = HashMap<String, Point3D>()
            val percent = FctXY().setFormulaX("x")

            val settings = HashMap<String, Any>()
            settings["useMaxRes"] = true
            settings["maxResWidth"] = 200
            settings["maxResHeight"] = 200
            settings["useConstantMeshIncrement"] = true
            
            val morphing = MovieMorphing(
                10, image1, model!!, image3, txt1, txt3, false, 0, false, settings, percent
            )

            val resultFile = morphing.run()
            println("Morphing completed. Result: ${resultFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

fun main() {
    val morphing = TestMorphing()
    val thread = Thread(morphing)
    thread.start()
}
