import tornadofx.*

class Style : Stylesheet() {
    companion object {
        val enqueued by cssclass()
        val dequeued by cssclass()
    }

    init {
        text {
            fontSize = 15.px
        }
        enqueued {
            backgroundColor += c("#4CAF50", .5)
        }
        dequeued {
            backgroundColor += c("#FF5722", .5)
        }
    }
}