package tech.ai_robotics.drone_shooter_2.common

import java.util.LinkedList

/**
 * @author ivan.klymenko@fuib.com on 25/07/2025
 */
class LimitedSizeList<T>(private val maxSize: Int) : LinkedList<T>() {

    override fun add(element: T): Boolean {
        if (size == maxSize) {
            removeFirst()
        }
        return super.add(element)
    }
}