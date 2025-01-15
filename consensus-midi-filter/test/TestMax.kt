package lerpmusic.consensus.device

import kotlinx.coroutines.DisposableHandle

object TestMax : Max {
    override fun post(vararg args: Any) {
        TODO("Not yet implemented")
    }

    override suspend fun outlet(vararg values: Any) {
        TODO("Not yet implemented")
    }

    override fun addHandler(
        selector: Any,
        handler: (Any) -> Unit
    ): DisposableHandle {
        TODO("Not yet implemented")
    }

    override fun addHandler2(
        selector: Any,
        handler: (Any, Any) -> Unit
    ): DisposableHandle {
        TODO("Not yet implemented")
    }

    override fun addHandler3(
        selector: Any,
        handler: (Any, Any, Any) -> Unit
    ): DisposableHandle {
        TODO("Not yet implemented")
    }
}