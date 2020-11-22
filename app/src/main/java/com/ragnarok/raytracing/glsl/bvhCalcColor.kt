package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language


@Language("glsl")
val bvhIntersect = """
    vec3 fetchVec3FromBuffer(samplerBuffer bufferTex, int index) {
        float x = texelFetch(bufferTex, index * 3).x;
        float y = texelFetch(bufferTex, index * 3 + 1).x;
        float z = texelFetch(bufferTex, index * 3 + 2).x;
        return vec3(x, y, z);
    }
    BoundNode createBoundNode(int treePtr, Material material) {
        vec3 nodeMin = fetchVec3FromBuffer(bvhMinBoundsBuffer, treePtr);
        vec3 nodeMax = fetchVec3FromBuffer(bvhMaxBoundsBuffer, treePtr);
        int triangleIndex = int(texelFetch(bvhTriangleIndexBuffer, treePtr).x);
        Bound bound = Bound(nodeMin, nodeMax);
        BoundNode boundNode;
        boundNode.bound = bound;
        boundNode.triangleIndex = triangleIndex;
        boundNode.boundIndex = treePtr;
        if (triangleIndex >= 0) {
            vec3 p0 = fetchVec3FromBuffer(verticesBuffer, 3 * triangleIndex);
            vec3 p1 = fetchVec3FromBuffer(verticesBuffer, 3 * triangleIndex + 1);
            vec3 p2 = fetchVec3FromBuffer(verticesBuffer, 3 * triangleIndex + 2);
            Triangle triangle = Triangle(p0, p1, p2, material);
            boundNode.triangle = triangle;
        }
        return boundNode;
    }
    
    Intersection intersectScene(Ray ray) {
        BoundNode boundNode = createBoundNode(1, material);
        BoundNode leftNode, rightNode;
        
        Intersection intersection;
        intersection.t = ${PassVariable.infinity};
        
        BoundNode stack[30];
        int stackPtr = 1;
        stack[0] = boundNode;
        
        while (stackPtr > 0) {
            stackPtr--;
            
            int leftIndex = 2 * boundNode.boundIndex;
            int rightIndex = 2 * boundNode.boundIndex + 1;
            
            bool hasLeft = true;
            bool hasRight = true;
            if (leftIndex >= BVH_NODE_NUM) {
                // not have left node
                hasLeft = false;
            }
            if (rightIndex >= BVH_NODE_NUM) {
                // not have right node num
                hasRight = false;
            }
            
            if (!hasLeft && !hasRight) {
                 // leaf node
                if (boundNode.triangleIndex >= 0) {
                    intersection = intersectTriangle(ray, boundNode.triangle);
                }
                break;
            }
            
            if (hasLeft && hasRight) {
                leftNode = createBoundNode(leftIndex, material);
                rightNode = createBoundNode(rightIndex, material);
                
                Intersection leftIntersection = intersectBound(ray, leftNode.bound);
                Intersection rightIntersection = intersectBound(ray, rightNode.bound);
                
                bool isHitLeft = leftIntersection.nearFar.x <= leftIntersection.nearFar.y && leftIntersection.nearFar.y >= 0.0;
                bool isHitRight = rightIntersection.nearFar.x <= rightIntersection.nearFar.y && rightIntersection.nearFar.y >= 0.0;
                
                if (isHitLeft && leftIntersection.t < rightIntersection.t) {
                    stack[stackPtr++] = leftNode;
                } else if (isHitRight) {
                    stack[stackPtr++] = rightNode;
                }
            }
        }
        
        return intersection;
    }
""".trimIndent()

val bvhCalcColor = { vertexNum: Int, bvhNodeNum: Int ->
    @Language("glsl")
    val shader = """
        $bvhIntersect
        
        float getShadow(Intersection intersection, vec3 lightDir) {
            return 1.0;
        }
        
        $calcColor
    """.trimIndent()


    shader
}