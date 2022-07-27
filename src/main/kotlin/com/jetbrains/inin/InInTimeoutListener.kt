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

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Timer

class InInTimeoutListener : InInModel.Listener, ActionListener, Disposable {
  private var timer = Timer(getSuspendTimeout() * 1_000, this).apply {
    isRepeats = false
  }

  init {
    InInModel.instance.addListener(this)
  }

  override fun stateChanged() {
    when (InInModel.instance.state) {
      InInModel.State.ACTIVE_INITIAL_INDEXING,
      InInModel.State.IDLE_INITIAL_INDEXING -> startTimer()
      else -> stopTimer()
    }
  }

  override fun actionPerformed(e: ActionEvent?) {
    InInModel.instance.initialIndexingTimeout()
  }

  override fun dispose() {
    stopTimer()
    timer.removeActionListener(this)
    InInModel.instance.removeListener(this)
  }

  private fun startTimer() {
    if (!timer.isRunning) {
      timer.initialDelay = getSuspendTimeout() * 1_000
      timer.start()
    }
  }

  private fun stopTimer() {
    timer.stop()
  }

  private fun getSuspendTimeout(): Int {
    val value = Registry.get(registryKey).asInteger()
    return if (value > 0) value else defaultInitialTimeout
  }

  companion object {
    val instance: InInTimeoutListener
      get() = ApplicationManager.getApplication().getService(InInTimeoutListener::class.java)

    private const val defaultInitialTimeout = 5
    private const val registryKey = "ide.index.suspend.timeout"
  }
}