package edu.robotics.dibujot.gcode

class GcodeParser {

    fun parse(lines: List<String>): List<String> {
        return lines
            .map { line -> line.substringBefore(';').trimEnd() }
            .filter { line -> line.isNotBlank() }
    }
}
