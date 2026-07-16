package com.rk.exec
import android.app.Activity
class TerminalCommand(val exe: String, val args: Array<String>, val id: String, val workingDir: String?)
fun launchTerminal(activity: Activity, terminalCommand: TerminalCommand) {}
fun isTerminalInstalled(): Boolean = true
