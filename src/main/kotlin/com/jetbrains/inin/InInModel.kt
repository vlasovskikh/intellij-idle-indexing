/*
 * Copyright 2020-2022 Andrey Vlasovskikh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.inin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger

class InInModel {
  var state = State.initial
    private set

  private val listeners = mutableListOf<Listener>()

  fun enteredDumbMode() {
    send(Input.ENTERED_DUMB_MODE)
  }

  fun exitedDumbMode() {
    send(Input.EXITED_DUMB_MODE)
  }

  fun userIdle() {
    send(Input.USER_IDLE)
  }

  fun userActivity() {
    send(Input.USER_ACTIVITY)
  }

  fun indexingResumed() {
    send(Input.INDEXING_RESUMED)
  }

  fun indexingPaused() {
    send(Input.INDEXING_PAUSED)
  }

  fun initialIndexingTimeout() {
    send(Input.INITIAL_INDEXING_TIMEOUT)
  }

  fun addListener(listener: Listener) {
    listeners += listener
  }

  fun removeListener(listener: Listener) {
    listeners -= listener
  }

  private fun send(input: Input) {
    val oldState = state
    state = state.transition(input)
    if (state != oldState) {
      log.debug("[$oldState] -$input-> [$state]")
      notifyListeners()
    }
  }

  private fun notifyListeners() {
    listeners.forEach {
      it.stateChanged()
    }
  }

  interface Listener {
    fun stateChanged()
  }

  enum class State {
    ACTIVE_SMART,
    IDLE_SMART,
    ACTIVE_INITIAL_INDEXING,
    IDLE_INITIAL_INDEXING,
    ACTIVE_INDEXING_SUSPENDED,
    IDLE_INDEXING,
    ACTIVE_INDEXING_RESUMED,
    ACTIVE_INDEXING_PAUSED;

    fun transition(input: Input): State =
        when (this to input) {
          ACTIVE_SMART to Input.USER_IDLE -> IDLE_SMART
          ACTIVE_SMART to Input.ENTERED_DUMB_MODE -> ACTIVE_INITIAL_INDEXING

          IDLE_SMART to Input.USER_ACTIVITY -> ACTIVE_SMART
          IDLE_SMART to Input.ENTERED_DUMB_MODE -> IDLE_INITIAL_INDEXING

          ACTIVE_INITIAL_INDEXING to Input.USER_IDLE -> IDLE_INITIAL_INDEXING
          ACTIVE_INITIAL_INDEXING to Input.EXITED_DUMB_MODE -> ACTIVE_SMART
          ACTIVE_INITIAL_INDEXING to Input.INITIAL_INDEXING_TIMEOUT -> ACTIVE_INDEXING_SUSPENDED

          IDLE_INITIAL_INDEXING to Input.USER_ACTIVITY -> ACTIVE_INITIAL_INDEXING
          IDLE_INITIAL_INDEXING to Input.EXITED_DUMB_MODE -> IDLE_SMART
          IDLE_INITIAL_INDEXING to Input.INITIAL_INDEXING_TIMEOUT -> IDLE_INDEXING

          ACTIVE_INDEXING_SUSPENDED to Input.USER_IDLE -> IDLE_INDEXING
          ACTIVE_INDEXING_SUSPENDED to Input.INDEXING_RESUMED -> ACTIVE_INDEXING_RESUMED

          IDLE_INDEXING to Input.EXITED_DUMB_MODE -> IDLE_SMART
          IDLE_INDEXING to Input.USER_ACTIVITY -> ACTIVE_INDEXING_SUSPENDED

          ACTIVE_INDEXING_RESUMED to Input.EXITED_DUMB_MODE -> ACTIVE_SMART
          ACTIVE_INDEXING_RESUMED to Input.INDEXING_PAUSED -> ACTIVE_INDEXING_PAUSED

          ACTIVE_INDEXING_PAUSED to Input.INDEXING_RESUMED -> ACTIVE_INDEXING_RESUMED

          else -> this
        }

    companion object {
      val initial = ACTIVE_SMART
    }
  }

  enum class Input {
    USER_IDLE,
    USER_ACTIVITY,
    ENTERED_DUMB_MODE,
    EXITED_DUMB_MODE,
    INDEXING_RESUMED,
    INDEXING_PAUSED,
    INITIAL_INDEXING_TIMEOUT,
  }

  companion object {
    val instance: InInModel
      get() = ApplicationManager.getApplication().getService(InInModel::class.java)

    val log = Logger.getInstance(InInModel::class.java)
  }
}