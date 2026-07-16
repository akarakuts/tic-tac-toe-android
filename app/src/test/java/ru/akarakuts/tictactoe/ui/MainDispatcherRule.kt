package ru.akarakuts.tictactoe.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule that swaps [Dispatchers.Main] for a [StandardTestDispatcher] for the duration of a
 * test, so coroutines launched into `viewModelScope` (which runs on Main) are controllable and
 * deterministic. Use [runWith] inside a `runTest { }` block to drain the dispatcher's queue.
 *
 * A [StandardTestDispatcher] (rather than an unconfined one) is chosen deliberately: the
 * ViewModel's AI-move path relies on real suspension points (delay + dispatcher hops), and an
 * unconfined scheduler would eagerly execute them in surprising orders.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestRule {

    /** Advance the test dispatcher until its queue is empty — call from within `runTest`. */
    fun runWith(scope: TestScope) {
        scope.advanceUntilIdle()
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                Dispatchers.setMain(testDispatcher)
                try {
                    base.evaluate()
                } finally {
                    Dispatchers.resetMain()
                }
            }
        }
}
