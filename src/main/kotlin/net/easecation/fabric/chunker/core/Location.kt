package net.easecation.fabric.chunker.core

import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3i

data class Location(var x: Float, var y: Float, var z: Float, var yaw: Float, var pitch: Float) {
    constructor(e: Entity) : this(e.x.toFloat(), e.y.toFloat(), e.z.toFloat(), e.yaw, e.pitch)
    constructor(b: Vec3i) : this(b.x.toFloat(), b.y.toFloat(), b.z.toFloat(), 0.0f, 0.0f)
}