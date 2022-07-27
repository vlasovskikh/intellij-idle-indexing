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
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.progress.impl.ProgressSuspender

@Suppress("UnstableApiUsage")
class InInSuspenderListener : ProgressSuspender.SuspenderListener, Disposable, InInModel.Listener {
  private val suspenders = mutableListOf<ProgressSuspender>()
  private var ownListenerSuppressed = false

  init {
    InInModel.instance.addListener(this)
  }

  override fun suspendableProgressAppeared(suspender: ProgressSuspender) {
    // We assume that any suspendable progress is a sign of entering the dumb mode
    suspenders += suspender
    InInModel.instance.enteredDumbMode()
    update()
  }

  override fun suspendedStatusChanged(suspender: ProgressSuspender) {
    when {
      ownListenerSuppressed ->
        return
      suspender.isSuspended ->
        InInModel.instance.indexingPaused()
      else ->
        InInModel.instance.indexingResumed()
    }
  }

  override fun stateChanged() {
    update()
  }

  override fun dispose() {
    InInModel.instance.removeListener(this)
  }

  private fun update() {
    when (InInModel.instance.state) {
      InInModel.State.ACTIVE_INDEXING_SUSPENDED ->
        suspendActiveProcesses()
      InInModel.State.IDLE_INDEXING ->
        resumeSuspendedProcesses()
      InInModel.State.ACTIVE_INDEXING_RESUMED -> {
        resumeSuspendedProcesses()
        suspenders.clear()
      }
      InInModel.State.ACTIVE_INITIAL_INDEXING,
      InInModel.State.IDLE_INITIAL_INDEXING ->
        Unit
      else ->
        suspenders.clear()
    }
  }

  private fun resumeSuspendedProcesses() {
    suspenders
        .filter { it.isSuspended }
        .forEach {
          suppressOwnListener {
            it.resumeProcess()
          }
        }
  }

  private fun suspendActiveProcesses() {
    suspenders
        .filterNot { it.isSuspended }
        .forEach {
          suppressOwnListener {
            it.suspendProcess("Indexing suspended until ${ApplicationInfo.getInstance().versionName} is idle")
          }
        }
  }

  private fun suppressOwnListener(block: () -> Unit) {
    ownListenerSuppressed = true
    try {
      block()
    } finally {
      ownListenerSuppressed = false
    }
  }
}