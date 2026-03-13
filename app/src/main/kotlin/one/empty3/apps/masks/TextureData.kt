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
import one.empty3.library.Point3D
import one.empty3.library.objloader.E3Model
import one.empty3.libs.Image

class TextureData()  {
    var distanceAB: DistanceAB? = null
    var isBezier : Boolean = true
    var algorithm: Int = 0
    var convexHull3: ConvexHull? = null
    var imageFileRight: Image? = null
    var pointsInImage: Map<String, Point3D>? = null
    var convexHull1: ConvexHull? = null
    var pointsInModel: Map<String, Point3D>? = null
    var convexHull2: ConvexHull? = null
    var hdTextures: Boolean = false
    var points3: Map<String, Point3D>? = null
    var image: Image? = null
    var dimPictureBox: Dimension? = null
    var opt1: Boolean = false
    var optimizeGrid: Boolean = false
    var distanceABClass: Class<out DistanceAB?>? = null
    var model: E3Model? = null

}