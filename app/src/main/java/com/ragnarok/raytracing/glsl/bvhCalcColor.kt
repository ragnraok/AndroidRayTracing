package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

@Language("glsl")
val createBoundNode = """
    vec3 fetchVec3FromBuffer(samplerBuffer bufferTex, int index) {
        float x = texelFetch(bufferTex, index * 3).r;
        float y = texelFetch(bufferTex, index * 3 + 1).r;
        float z = texelFetch(bufferTex, index * 3 + 2).r;
        return vec3(x, y, z);
    }
    BoundNode createBoundNode(int treePtr, Material material) {
        vec3 nodeMin = fetchVec3FromBuffer(bvhMinBoundsBuffer, treePtr);
        vec3 nodeMax = fetchVec3FromBuffer(bvhMaxBoundsBuffer, treePtr);
        int triangleIndex = int(texelFetch(bvhTriangleIndexBuffer, treePtr).r);
        Bound bound = Bound(nodeMin, nodeMax);
        BoundNode boundNode;
        boundNode.bound = bound;
        boundNode.boundIndex = treePtr;
        boundNode.triangleIndex = -1;
        if (triangleIndex != ${PassVariable.invalidTriangleIndex}) {
            boundNode.triangleIndex = triangleIndex;
            vec3 p0 = fetchVec3FromBuffer(verticesBuffer, 3 * triangleIndex);
            vec3 p1 = fetchVec3FromBuffer(verticesBuffer, 3 * triangleIndex + 1);
            vec3 p2 = fetchVec3FromBuffer(verticesBuffer, 3 * triangleIndex + 2);
            Triangle triangle = Triangle(p0, p1, p2, material);
            boundNode.triangle = triangle;
            boundNode.triangleIndex = triangleIndex;
        }
        return boundNode;
    }
""".trimIndent()

@Language("glsl")
val bvhIntersect = """
    Intersection intersectScene(Ray ray) {
        BoundNode rootNode = createBoundNode(1, material);
        BoundNode leftNode, rightNode;
        
        Intersection intersection;
        intersection.t = ${PassVariable.infinity};
        intersection.nearFar = vec2(${PassVariable.infinity}, ${PassVariable.infinity});
        intersection.material = material;
        
        const int maxStackDepth = 30;
        
        BoundNode stack[maxStackDepth];
        int stackPtr = 1;
        stack[0] = rootNode;
        
        while (stackPtr > 0 && stackPtr < maxStackDepth) {
            stackPtr--;
            BoundNode boundNode = stack[stackPtr];
            int leftIndex = 2 * boundNode.boundIndex;
            int rightIndex = 2 * boundNode.boundIndex + 1;
            
            bool hasLeft = true;
            bool hasRight = true;
            if (leftIndex >= BVH_NODE_NUM) {
                // not have left node
                hasLeft = false;
            }
            if (rightIndex >= BVH_NODE_NUM) {
                // not have right node
                hasRight = false;
            }
            
            if (boundNode.triangleIndex != -1) {
                intersection = intersectTriangle(ray, boundNode.triangle);
                intersection.normal = -normalForTriangle(boundNode.triangle);
                intersection.material = material;
                break;    
            }
            
          
            if (hasLeft || hasRight) {
            
                bool isIntersectLeft = false;
                bool isIntersectRight = false;
          
                if (hasLeft) {
                    leftNode = createBoundNode(leftIndex, material);
                    isIntersectLeft = isIntersectBound(ray, leftNode.bound);
                }
                if (hasRight) {
                    rightNode = createBoundNode(rightIndex, material);
                    isIntersectRight = isIntersectBound(ray, rightNode.bound);
                }
                
                if (hasLeft && isIntersectLeft) {
                    stack[stackPtr++] = leftNode;
                }
                if (hasRight && isIntersectRight) {
                    stack[stackPtr++] = rightNode;
                }
            }
        }
        
        return intersection;
    }
""".trimIndent()

@Language("glsl")
val bvhCalcColor = """
    $createBoundNode
    $bvhIntersect
    
    float getShadow(Intersection intersection, vec3 lightDir) {
        return 1.0;
    }
    
//    Intersection intersectScene(Ray ray) {
//        int boundIndex = 3;
//        Intersection intersection;
//        BoundNode boundNode = createBoundNode(boundIndex, material);
//        if (boundNode.triangleIndex != -1) {
//            intersection = intersectTriangle(ray, boundNode.triangle);
//            intersection.normal = normalForTriangle(boundNode.triangle);
//        } else {
//            intersection = intersectBound(ray, boundNode.bound);
//        }
//        intersection.material = material;
//        return intersection;
//    }

    $calcColor
    
//    vec3 calcColor(Ray ray) {
//        float x = texelFetch(verticesBuffer, 3).x;
//        float y = texelFetch(verticesBuffer, 4).x;
//        float z = texelFetch(verticesBuffer, 5).x;
//        return vec3(x, y, z);
//    }
""".trimIndent()