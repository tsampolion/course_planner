package gr.edusuite.pacer.ui

/** Each notification owns its lifetime, even when two notifications refer to the same undo step. */
internal class UndoGate {
    private var next = 0L
    private var current: Long? = null

    fun open(): Long = (++next).also { current = it }
    fun isCurrent(token: Long): Boolean = current == token
    fun close(token: Long): Boolean {
        if (!isCurrent(token)) return false
        current = null
        return true
    }
}
