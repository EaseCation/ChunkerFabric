package net.easecation.fabric.chunker.core

import kotlin.math.pow

enum class PositionLayout {
    COMMA,
    COMMA_CAMERA,
    COMMA_MOVE,
    COLON;

    fun format(loc: Location): String {
        var loc = loc
        if (this == COMMA) {
            //形如 89.5, 64, -228.5, 0, 0
            loc = getRoundPos(loc, 1, true)
            loc.yaw = getRoundYaw(loc.yaw)
            loc.pitch = getRoundPitch(loc.pitch)
            return formatNumber(loc.x, 1) + ", " + formatNumber(loc.y, 1) + ", " + formatNumber(
                loc.z,
                1
            ) + ", " + formatNumber(loc.yaw, 1) + ", " + formatNumber(loc.pitch, 1)
        } else if (this == COMMA_CAMERA) {
            //形如 89.5, 64, -228.5, 0, 0
            return formatNumber(loc.x, 2) + ", " + formatNumber(
                loc.y + 1.62f,
                2
            ) + ", " + formatNumber(loc.z, 2) + ", " + formatNumber(
                loc.yaw,
                2
            ) + ", " + formatNumber(loc.pitch, 2)
        } else if (this == COLON) {
            loc = getRoundPos(loc, 1, true)
            loc.yaw = getRoundYaw(loc.yaw)
            loc.pitch = getRoundPitch(loc.pitch)
            //形如 89.5:64:-228.5:0:0，如果为整数，则不显示小数
            return formatNumber(loc.x, 1) + ":" + formatNumber(loc.y, 1) + ":" + formatNumber(
                loc.z,
                1
            ) + ":" + formatNumber(loc.yaw, 1) + ":" + formatNumber(loc.pitch, 1)
        } else if (this == COMMA_MOVE) {
            //形如 89.5, 64, -228.5
            return formatNumber(loc.x, 2) + ", " + formatNumber(loc.y, 2) + ", " + formatNumber(loc.z, 2)
        }
        return "Unsupported PositionLayout " + this.name
    }

    fun formatPos(loc: Location): String {
        if (this == COMMA) {
            //形如 89.5, 64, -228.5, 0, 0
            return formatNumber(loc.x, 0) + ", " + formatNumber(loc.y, 0) + ", " + formatNumber(loc.z, 0)
        } else if (this == COLON) {
            //形如 89.5:64:-228.5:0:0，如果为整数，则不显示小数
            return formatNumber(loc.x, 0) + ":" + formatNumber(loc.y, 0) + ":" + formatNumber(loc.z, 0)
        }
        return "Unsupported PositionLayout " + this.name
    }

    companion object {
        fun formatNumber(number: Float, decimalPlaces: Int): String {
            return if (number == number.toInt().toFloat()) {
                String.format("%d", number.toInt())
            } else {
                String.format("%." + decimalPlaces + "f", number)
            }
        }

        fun getRoundPos(pos: Location, decimalPlaces: Int, roundToHalf: Boolean): Location {
            if (roundToHalf) {
                return Location(
                    roundToHalf(pos.x),
                    roundToHalf(pos.y),
                    roundToHalf(pos.z),
                    roundToHalf(pos.yaw),
                    roundToHalf(pos.pitch)
                )
            } else {
                val multiplier = 10.0f.pow(decimalPlaces)
                return Location(
                    Math.round(pos.x * multiplier) / multiplier,
                    Math.round(pos.y * multiplier) / multiplier,
                    Math.round(pos.z * multiplier) / multiplier,
                    (Math.round(pos.yaw * multiplier) / multiplier),
                    (Math.round(pos.pitch * multiplier) / multiplier)
                )
            }
        }

        fun roundToHalf(d: Float): Float {
            return Math.round(d * 2) / 2.0f
        }

        fun round(d: Float, precision: Int): Float {
            return Math.round(d * 10.0f.pow(precision)) / 10.0f.pow(precision)
        }

        fun getRoundYaw(yaw: Float): Float {
            var value = (Math.round(yaw / 45) * 45 % 360).toFloat()
            if (value < -180) {
                value += 360f
            }
            return value
        }

        fun getRoundPitch(pitch: Float): Float {
            return (Math.round(pitch / 45) * 45).toFloat()
        }
    }
}
