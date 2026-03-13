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

import one.empty3.libs.Image
import one.empty3.libs.commons.IImageMp
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.IIOImage

fun saveImageToPicturesLegacy(filename: String, bitmap: Image, path: String): File? {
    try {
        val directory = File("out", "Empty3_Mesh_Masks")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "${filename}_${directory.listFiles()?.size ?: 0}_${System.currentTimeMillis()}.png")
        Logger.getAnonymousLogger().info("saveImageToPicturesLegacy: ${file.absolutePath}")
        
        if (file.exists()) {
            return null
        }

        FileOutputStream(file).use { outputStream ->
            (bitmap as IImageMp).toOutputStream(outputStream)
            outputStream.flush()
        }

        return file
    } catch (e: Exception) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "Error writing image: ${e.message}", e)
        return null
    }
}
