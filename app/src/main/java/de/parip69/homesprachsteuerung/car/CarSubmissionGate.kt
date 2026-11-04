package de.parip69.homesprachsteuerung.car

/** One submitted input screen = at most one attempt, including after errors.
 * A new screen supplies a new ID, so repeating an intentional command is allowed. */
internal class CarSubmissionGate {
    private val consumed = mutableSetOf<String>()
    private var active: String? = null

    @Synchronized fun begin(id: String, text: String): Boolean {
        if (text.isBlank() || text.length > 1000 || active != null || id in consumed) return false
        consumed.add(id)
        active = id
        return true
    }

    @Synchronized fun finish(id: String) {
        if (active == id) active = null
    }
}
