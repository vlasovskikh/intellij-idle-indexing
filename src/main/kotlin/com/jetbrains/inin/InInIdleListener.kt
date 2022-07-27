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

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry

class InInIdleListener : Disposable, InInModel.Listener {
  private var idleTimeout = getIdleTimeout()

  private val idleListener = Runnable {
    InInModel.instance.userIdle()
  }

  init {
    queue.addIdleListener(idleListener, idleTimeout * 1000)
    queue.addActivityListener(::activityListener, this)
    InInModel.instance.addListener(this)
  }

  override fun dispose() {
    queue.removeIdleListener(idleListener)
    InInModel.instance.removeListener(this)
  }

  override fun stateChanged() {
    updateIdleTimeout()
  }

  private fun updateIdleTimeout() {
    val newIdleTimeout = getIdleTimeout()
    if (newIdleTimeout != idleTimeout) {
      idleTimeout = newIdleTimeout
      queue.removeIdleListener(idleListener)
      queue.addIdleListener(idleListener, idleTimeout * 1000)
    }
  }

  private fun getIdleTimeout(): Int {
    val registryValue = Registry.get(registryKey).asInteger()
    return if (registryValue > 0) registryValue else defaultIdleTimeout
  }

  private fun activityListener() {
    InInModel.instance.userActivity()
  }

  private val queue: IdeEventQueue
    get() = IdeEventQueue.getInstance()

  companion object {
    val instance: InInIdleListener
      get() = ApplicationManager.getApplication().getService(InInIdleListener::class.java)

    private const val defaultIdleTimeout = 3
    private const val registryKey = "ide.index.idle.timeout"
  }
}