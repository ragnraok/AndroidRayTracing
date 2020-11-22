package com.ragnarok.raytracing.renderer.bvh

import android.util.Log
import com.ragnarok.raytracing.utils.currentTick
import com.ragnarok.raytracing.utils.tickToNowMs
import de.javagl.obj.ObjData
import de.javagl.obj.ReadableObj
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

class BVH(private val obj: ReadableObj) {

    companion object {
        private const val TAG = "BVH"
    }

    data class BVHNode(val bound: Bound, val left: BVHNode?, val right: BVHNode?, val triangle: Triangle?, val area: Float, val depth: Int = 0)

    // Obj base properties
    private var faceIndices: IntArray = IntArray(0)
    private var normalIndices: IntArray = IntArray(0)
    private var texCoordIndices: IntArray = IntArray(0)
    private var vertices: FloatArray = FloatArray(0)
    private var texCoords: FloatArray = FloatArray(0)
    private var normals: FloatArray = FloatArray(0)

    private val triangleList = ArrayList<Triangle>()

    // bvh flat data
    // triangle bound min/max coordinates
    val minBoundsArray = ArrayList<Vec3>()
    val maxBoundsArray = ArrayList<Vec3>()
    // triangle vertices coordinates
    val verticesArray = ArrayList<Vec3>()
    // triangle normal coordinates
    val normalArray = ArrayList<Vec3>()
    // triangle texture coordinates
    val texCoordArray = ArrayList<Vec2>()
    // store bvh node tree, for node in a[k], left node is in a[k * 2], right node is in a[2 * k + 1]
    var bvhMinFlatArray = ArrayList<Vec3>()
    // store bvh node tree
    var bvhMaxFlatArray = ArrayList<Vec3>()
    // store bvh node correspond triangle index in bvh, -1 if the current bvh node is not leaf node
    // use this value to search correspond triangle coordinates in verticesArray
    // for example, if bvhTriangleIndexArray[k] = x, then the triangle coordinates in verticesArray
    // is store in (verticesArray[3 * x], verticesArray[3 * x + 1], verticesArray[3 * x + 2])
    var bvhTriangleIndexArray = ArrayList<Int>()

    val hasNormals
        get() = normalIndices.isNotEmpty() && normalIndices.size == faceIndices.size

    val hasTexCoords
        get() = texCoordIndices.isNotEmpty() && texCoordIndices.size == faceIndices.size

    private var bvhNodeCount = 0
    private var leafNodeCount = 0
    private var bvhDepth = 0

    fun buildBVH() {
        var tick = currentTick()
        val startTick = tick
        faceIndices = ObjData.getFaceVertexIndicesArray(obj)
        if (obj.numNormals > 0) {
            normalIndices = ObjData.getFaceNormalIndicesArray(obj)
            normals = ObjData.getNormalsArray(obj)
        }
        if (obj.numTexCoords > 0) {
            texCoordIndices = ObjData.getFaceTexCoordIndicesArray(obj)
            texCoords = ObjData.getTexCoordsArray(obj, 2, true)
        }
        vertices = ObjData.getVerticesArray(obj)


        Log.i(TAG, "indices.size:${faceIndices.size}, vertices.size:${vertices.size}, texCoords.size:${texCoords.size}, texCoordIndices.size:${texCoordIndices.size}, " +
                "normals.size:${normals.size}, normalIndices.size:${normalIndices.size}")

        if (faceIndices.isEmpty() || vertices.isEmpty()) {
            Log.e(TAG, "faceIndices or vertices is empty, maybe error obj file")
            return
        }

        val buildNormals = normalIndices.isNotEmpty() && normalIndices.size == faceIndices.size
        val buildTexCoords = texCoordIndices.isNotEmpty() && texCoordIndices.size == faceIndices.size
        Log.i(TAG, "buildNormals:$buildNormals, buildTexCoords:$buildTexCoords")

        for (i in faceIndices.indices step 3) {
            val p0Index = faceIndices[i]
            val p0x = vertices[p0Index]
            val p0y = vertices[p0Index + 1]
            val p0z = vertices[p0Index + 2]

            val p1Index = faceIndices[i + 1]
            val p1x = vertices[p1Index]
            val p1y = vertices[p1Index + 1]
            val p1z = vertices[p1Index + 2]

            val p2Index = faceIndices[i + 2]
            val p2x = vertices[p2Index]
            val p2y = vertices[p2Index + 1]
            val p2z = vertices[p2Index + 2]

            verticesArray.add(Vec3(p0x, p0y, p0z))
            verticesArray.add(Vec3(p1x, p1y, p1z))
            verticesArray.add(Vec3(p2x, p2y, p2z))

            val triangle = Triangle(Vec3(p0x, p0y, p0z), Vec3(p1x, p1y, p1z), Vec3(p2x, p2y, p2z))
            triangleList.add(triangle)

            minBoundsArray.add(triangle.bound.min)
            maxBoundsArray.add(triangle.bound.max)

            if (buildNormals) {
                val p0NormalIndex = normalIndices[i]
                val p0Normalx = normals[p0NormalIndex]
                val p0Normaly = normals[p0NormalIndex + 1]
                val p0Normalz = normals[p0NormalIndex + 2]

                val p1NormalIndex = normalIndices[i + 1]
                val p1Normalx = normals[p1NormalIndex]
                val p1Normaly = normals[p1NormalIndex + 1]
                val p1Normalz = normals[p1NormalIndex + 2]

                val p2NormalIndex = normalIndices[i + 2]
                val p2Normalx = normals[p2NormalIndex]
                val p2Normaly = normals[p2NormalIndex + 1]
                val p2Normalz = normals[p2NormalIndex + 2]

                normalArray.add(Vec3(p0Normalx, p0Normaly, p0Normalz))
                normalArray.add(Vec3(p1Normalx, p1Normaly, p1Normalz))
                normalArray.add(Vec3(p2Normalx, p2Normaly, p2Normalz))
            }

            if (buildTexCoords) {
                val p0TexCoordIndex = texCoordIndices[i]
                val p0TexCoordx = texCoords[p0TexCoordIndex]
                val p0TexCoordy = texCoords[p0TexCoordIndex + 1]
//                val p0TexCoordz = texCoords[p0TexCoordIndex + 2]

                val p1TexCoordIndex = texCoordIndices[i + 1]
                val p1TexCoordx = texCoords[p1TexCoordIndex]
                val p1TexCoordy = texCoords[p1TexCoordIndex + 1]
//                val p1TexCoordz = texCoords[p1TexCoordIndex + 2]

                val p2TexCoordIndex = texCoordIndices[i + 2]
                val p2TexCoordx = texCoords[p2TexCoordIndex]
                val p2TexCoordy = texCoords[p2TexCoordIndex + 1]
//                val p2TexCoordz = texCoords[p2TexCoordIndex + 2]

                texCoordArray.add(Vec2(p0TexCoordx, p0TexCoordy))
                texCoordArray.add(Vec2(p1TexCoordx, p1TexCoordy))
                texCoordArray.add(Vec2(p2TexCoordx, p2TexCoordy))
            }

        }
        Log.i(TAG, "total triangle size:${triangleList.size}")
        val rootNode = recursiveBuild(triangleList)
        Log.i(TAG, "build bvh cost:${tickToNowMs(tick)}ms")
        tick = currentTick()
        flatBVH(rootNode)
        Log.i(TAG, "flat bvh cost:${tickToNowMs(tick)}ms")

        Log.i(TAG, "bvhNodeCount:$bvhNodeCount, leafNodeCount:$leafNodeCount, bvhDepth:$bvhDepth")

        Log.i(TAG, "bvh flat array size:[${bvhMaxFlatArray.size} ${bvhMinFlatArray.size} ${bvhTriangleIndexArray.size}], " +
                "vertices.size:${verticesArray.size}, bounds.size:[${minBoundsArray.size} ${maxBoundsArray.size}], normals.size:${normalArray.size}, " +
                "texCoordArray.size:${texCoordArray.size}, totally cost:${tickToNowMs(startTick)}ms")
    }

    private fun recursiveBuild(triangles: List<Triangle>, depth: Int = 0): BVHNode {
        var bound = Bound.emptyBound
        for (triangle in triangles) {
            bound = bound.union(triangle.bound)
        }
        if (triangles.size == 1) {
            bvhNodeCount++
            bvhDepth = max(bvhDepth, depth)
            return BVHNode(bound = triangles[0].bound, left = null, right = null, triangle = triangles[0], area = triangles[0].area, depth = depth)
        } else if (triangles.size == 2) {
            val leftNode = recursiveBuild(triangles.subList(0, 1), depth = depth + 1)
            val rightNode = recursiveBuild(triangles.subList(1, 2), depth = depth + 1)
            bvhNodeCount++
            return BVHNode(bound = leftNode.bound.union(rightNode.bound), left = leftNode, right = rightNode, triangle = null, area = leftNode.area + rightNode.area, depth = depth)
        } else {
            var centerBound = Bound.emptyBound
            for (triangle in triangles) {
                centerBound = centerBound.union(triangle.bound.center())
            }
            when (centerBound.maxExtent()) {
                Axis.X -> triangles.sortedBy { it.bound.center().x }
                Axis.Y -> triangles.sortedBy { it.bound.center().y }
                Axis.Z -> triangles.sortedBy { it.bound.center().z }
            }
            val leftTriangles = triangles.subList(0, triangles.size / 2)
            val rightTriangles = triangles.subList(triangles.size / 2, triangles.size)
            val leftNode = recursiveBuild(leftTriangles, depth = depth + 1)
            val rightNode = recursiveBuild(rightTriangles, depth = depth + 1)
            bvhNodeCount++
            return BVHNode(bound = leftNode.bound.union(rightNode.bound), left = leftNode, right = rightNode, triangle = null, area = leftNode.area + rightNode.area, depth = depth)
        }
    }

    private fun flatBVH(rootNode: BVHNode) {
        bvhMinFlatArray.add(Vec3(0))
        bvhMaxFlatArray.add(Vec3(0))
        bvhTriangleIndexArray.add(-1)
        val nodeQueue = LinkedList<BVHNode>()
        nodeQueue.add(rootNode)

        // breadth first traverse to store in array
        while (nodeQueue.isNotEmpty()) {
            val node = nodeQueue.removeFirst()
            bvhMinFlatArray.add(node.bound.min)
            bvhMaxFlatArray.add(node.bound.max)
//            Log.i(TAG, "flat ${node.bound} store in ${bvhFlatArray.size - 1}, queue.size:${nodeQueue.size}")

            if (node.left != null) {
                nodeQueue.add(node.left)
            }
            if (node.right != null) {
                nodeQueue.add(node.right)
            }
            if (node.left == null && node.right == null) {
                val triangle = node.triangle
                val triangleIndex = triangleList.indexOf(triangle)
//                Log.i(TAG, "store leaf triangle $triangleIndex in ${bvhTriangleIndexArray.size}")
                leafNodeCount++
                bvhTriangleIndexArray.add(triangleIndex)
            } else {
                bvhTriangleIndexArray.add(-1)
            }
        }
        Log.i(TAG, "flat bvh finished")
    }
}