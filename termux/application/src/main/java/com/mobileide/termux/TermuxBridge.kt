package com.mobileide.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mobileide.app.data.LineType
import com.mobileide.app.data.TerminalLine
import com.mobileide.app.logger.LogTag
import com.mobileide.app.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
